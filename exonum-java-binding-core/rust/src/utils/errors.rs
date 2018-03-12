use jni::JNIEnv;
use jni::errors::{Error as JNIError, ErrorKind, Result as JNIResult};
use jni::objects::JObject;

use super::get_class_name;

const CLASS_JL_ERROR: &str = "java/lang/Error";

/// Unwraps the result, returning its content.
///
/// Panics:
/// - Panics if there is some JNI error.
/// - If there is a pending Java exception of any type,
///   handles it and panics with a message from the exception.

pub fn panic_on_exception<T>(env: &JNIEnv, result: JNIResult<T>) -> T {
    result.unwrap_or_else(|jni_error| match jni_error.0 {
        ErrorKind::JavaException => {
            let exception = get_and_clear_java_exception(env);
            panic!(describe_java_exception(env, exception, &jni_error));
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

pub fn check_error_on_exception<T>(env: &JNIEnv, result: JNIResult<T>) -> Result<T, String> {
    result.map_err(|jni_error| match jni_error.0 {
        ErrorKind::JavaException => {
            let exception = get_and_clear_java_exception(env);
            let message = describe_java_exception(env, exception, &jni_error);
            if unwrap_jni(env.is_instance_of(exception, CLASS_JL_ERROR)) {
                panic!(message);
            }
            message
        }
        _ => unwrap_jni(Err(jni_error)),
    })
}

/// Panics if an error occurred in the JNI API.
pub fn unwrap_jni<T>(res: JNIResult<T>) -> T {
    res.unwrap_or_else(|err| panic!("JNI error: {:?}", err))
}

pub fn get_and_clear_java_exception<'e>(env: &'e JNIEnv) -> JObject<'e> {
    let exception: JObject = unwrap_jni(env.exception_occurred()).into();
    // A null exception from #exception_occurred indicates that there is no pending exception.
    // It is possible if current thread is reattached to JVM.
    assert!(!exception.is_null(), "No exception thrown.");
    unwrap_jni(env.exception_clear());
    exception
}

fn describe_java_exception(env: &JNIEnv, exception: JObject, jni_error: &JNIError) -> String {
    format!(
        "Java exception: {}\n{:#?}",
        unwrap_jni(get_class_name(env, exception)),
        jni_error.backtrace(),
    )
}
