/*
 * Copyright 2019 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

use jni::objects::JObject;
use jni::JNIEnv;

use std::any::Any;
use std::cell::Cell;
use std::error::Error;
use std::result;
use std::thread;

use utils::{get_class_name, get_exception_message, jni_cache::classes_refs};
use {JniError, JniErrorKind, JniResult};

/// Unwraps the result, returning its content.
///
/// Panics:
/// - If there is some JNI error.
/// - If there is a pending Java exception of any type,
///   handles it and panics with a message from the exception.
pub fn panic_on_exception<T>(env: &JNIEnv, result: JniResult<T>) -> T {
    result.unwrap_or_else(|jni_error| match jni_error.0 {
        JniErrorKind::JavaException => {
            let exception = get_and_clear_java_exception(env);
            panic!(describe_java_exception(env, exception));
        }
        _ => unwrap_jni(Err(jni_error)),
    })
}

/// Handles and describes non-fatal Java exceptions.
///
/// Java exceptions are converted into `Error`s with their descriptions, Java errors and JNI errors
/// are treated as unrecoverable and result in a panic.
///
/// Panics:
/// - Panics if there is some JNI error.
/// - If there is a pending Java exception that is a subclass of `java.lang.Error`.
pub fn check_error_on_exception<T>(env: &JNIEnv, result: JniResult<T>) -> Result<T, String> {
    result.map_err(|jni_error| match jni_error.0 {
        JniErrorKind::JavaException => {
            let exception = get_and_clear_java_exception(env);
            let message = describe_java_exception(env, exception);
            if unwrap_jni_verbose(
                env,
                env.is_instance_of(exception, &classes_refs::java_lang_error()),
            ) {
                panic!(message);
            }
            message
        }
        _ => unwrap_jni(Err(jni_error)),
    })
}

/// Unwraps `jni::Result`
///
/// Panics if there is some JNI error.
pub fn unwrap_jni<T>(res: JniResult<T>) -> T {
    res.unwrap_or_else(|err| panic!("JNI error: {:?}", err))
}

/// Unwraps `jni::Result` with verbose error message if Java exception occurred.
/// To get an additional info about the exception, it calls JNI API, which can lead
/// to another exception. In that case it gives up to get verbose error message to prevent
/// an infinite recursion and stack overflow.
///
/// Panics if there is some JNI error.
pub fn unwrap_jni_verbose<T>(env: &JNIEnv, res: JniResult<T>) -> T {
    thread_local! {
        static IN_RECURSION: Cell<bool> = Cell::new(false);
    }
    IN_RECURSION.with(|in_recursion| {
        res.unwrap_or_else(|jni_error| {
            // If we get another JNI error whilst handling one — stop processing both and panic.
            if in_recursion.get() {
                // Reset the flag to allow future use of this method.
                in_recursion.set(false);
                panic!("Recursive JNI error: {:?}", jni_error);
            } else {
                match jni_error.0 {
                    JniErrorKind::JavaException => {
                        in_recursion.set(true);
                        let exception = get_and_clear_java_exception(env);
                        let message = describe_java_exception(env, exception);
                        in_recursion.set(false);
                        panic!(message);
                    }
                    _ => unwrap_jni(Err(jni_error)),
                }
            }
        })
    })
}

/// Returns (and clears) any exception that is currently being thrown.
pub fn get_and_clear_java_exception<'e>(env: &'e JNIEnv) -> JObject<'e> {
    let exception: JObject = unwrap_jni(env.exception_occurred()).into();
    // A null exception from #exception_occurred indicates that there is no pending exception.
    // It is possible if current thread is reattached to JVM.
    assert!(!exception.is_null(), "No exception thrown.");
    unwrap_jni_verbose(env, env.exception_clear());
    exception
}

/// Describes Java exception in a form of "Java exception: EXCEPTION_NAME: EXCEPTION_DETAILS"
pub fn describe_java_exception(env: &JNIEnv, exception: JObject) -> String {
    assert!(!exception.is_null(), "No exception thrown.");
    let format = || {
        Ok(format!(
            "Java exception: {}; {:?}",
            get_class_name(env, exception)?,
            get_exception_message(env, exception)?,
        ))
    };
    unwrap_jni_verbose(env, format())
}

type ExceptionResult<T> = thread::Result<result::Result<T, JniError>>;

/// Returns value or "throws" exception. `error_val` is returned, because exception will be thrown
/// at the Java side. So this function should be used only for the `panic::catch_unwind` result.
pub fn unwrap_exc_or<T>(env: &JNIEnv, res: ExceptionResult<T>, error_val: T) -> T {
    match res {
        Ok(val) => {
            match val {
                Ok(val) => val,
                Err(jni_error) => {
                    // Do nothing if there is a pending Java-exception that will be thrown
                    // automatically by the JVM when the native method returns.
                    if !env.exception_check().unwrap() {
                        // Throw a Java exception manually in case of an internal error.
                        throw(env, &jni_error.to_string())
                    }
                    error_val
                }
            }
        }
        Err(ref e) => {
            throw(env, &any_to_string(e));
            error_val
        }
    }
}

/// Same as `unwrap_exc_or` but returns default value.
pub fn unwrap_exc_or_default<T: Default>(env: &JNIEnv, res: ExceptionResult<T>) -> T {
    unwrap_exc_or(env, res, T::default())
}

/// Calls a corresponding `JNIEnv` method, so exception will be thrown when execution returns to
/// the Java side.
fn throw(env: &JNIEnv, description: &str) {
    // We cannot throw exception from this function, so errors should be written in log instead.
    let exception = match env.find_class("java/lang/RuntimeException") {
        Ok(val) => val,
        Err(e) => {
            error!(
                "Unable to find 'RuntimeException' class: {}",
                e.description()
            );
            return;
        }
    };
    if let Err(e) = env.throw_new(exception, description) {
        error!(
            "Unable to find 'RuntimeException' class: {}",
            e.description()
        );
    }
}

/// Tries to get meaningful description from panic-error.
pub fn any_to_string(any: &Box<Any + Send>) -> String {
    if let Some(s) = any.downcast_ref::<&str>() {
        s.to_string()
    } else if let Some(s) = any.downcast_ref::<String>() {
        s.clone()
    } else if let Some(error) = any.downcast_ref::<Box<Error + Send>>() {
        error.description().to_string()
    } else {
        "Unknown error occurred".to_string()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::error::Error;
    use std::panic;

    #[test]
    fn str_any() {
        let string = "Static string (&str)";
        let error = panic_error(string);
        assert_eq!(string, any_to_string(&error));
    }

    #[test]
    fn string_any() {
        let string = "Owned string (String)".to_owned();
        let error = panic_error(string.clone());
        assert_eq!(string, any_to_string(&error));
    }

    #[test]
    fn box_error_any() {
        let error: Box<Error + Send> = Box::new("e".parse::<i32>().unwrap_err());
        let description = error.description().to_owned();
        let error = panic_error(error);
        assert_eq!(description, any_to_string(&error));
    }

    #[test]
    fn unknown_any() {
        let error = panic_error(1);
        assert_eq!("Unknown error occurred", any_to_string(&error));
    }

    fn panic_error<T: Send + 'static>(val: T) -> Box<Any + Send> {
        panic::catch_unwind(panic::AssertUnwindSafe(|| panic!(val))).unwrap_err()
    }
}
