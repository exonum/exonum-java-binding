use jni::JNIEnv;

use std::any::Any;
use std::thread;
use std::result;
use std::error::Error;

use JniError;

type ExceptionResult<T> = thread::Result<result::Result<T, JniError>>;

// Returns value or "throws" exception. `error_val` is returned, because exception will be thrown
// at the Java side. So this function should be used only for the `panic::catch_unwind` result.
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

// Same as `unwrap_exc_or` but returns default value.
pub fn unwrap_exc_or_default<T: Default>(env: &JNIEnv, res: ExceptionResult<T>) -> T {
    unwrap_exc_or(env, res, T::default())
}

// Calls a corresponding `JNIEnv` method, so exception will be thrown when execution returns to
// the Java side.
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

// Tries to get meaningful description from panic-error.
// TODO: Remove `allow(borrowed_box)` after https://github.com/Manishearth/rust-clippy/issues/1884
// is fixed.
#[cfg_attr(feature = "cargo-clippy", allow(borrowed_box))]
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
    use std::panic;
    use std::error::Error;
    use super::*;

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
