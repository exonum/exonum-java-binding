use jni::JNIEnv;
use jni::errors::{ErrorKind, Result as JNIResult};
use jni::objects::JObject;

use super::get_class_name;

const CLASS_JL_ERROR: &str = "java/lang/Error";

/// Checks if Java exception occurred and then panics.
pub fn panic_on_exception<T>(env: &JNIEnv, result: &JNIResult<T>) {
    if let Err(ref jni_error) = *result {
        if let ErrorKind::JavaException = jni_error.0 {
            let exception: JObject = unwrap_jni(env.exception_occurred()).into();
            unwrap_jni(env.exception_clear());
            assert!(!exception.is_null());
            panic!(
                "Java exception: {}\n{:#?}",
                unwrap_jni(get_class_name(env, exception)),
                jni_error.backtrace()
            )
        }
    }
}

/// Checks if Java exception occurred and then panics if it is a Java error
/// otherwise, converts it into a Rust error.
pub fn check_error_on_exception<T>(env: &JNIEnv, result: JNIResult<T>) -> Result<T, String> {
    result.map_err(|jni_error| match jni_error.0 {
        ErrorKind::JavaException => {
            let exception: JObject = unwrap_jni(env.exception_occurred()).into();
            unwrap_jni(env.exception_clear());
            assert!(!exception.is_null());
            let message =
                format!(
                "Java exception: {}\n{:#?}",
                unwrap_jni(get_class_name(env, exception)),
                jni_error.backtrace(),
            );
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
