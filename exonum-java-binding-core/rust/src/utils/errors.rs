use jni::JNIEnv;
use jni::errors::{ErrorKind, Result};
use jni::objects::JObject;

use super::get_class_name;

/// Checks if Java exception occurred and then panics.
pub fn panic_on_exception<T>(env: &JNIEnv, result: &Result<T>) -> Result<()> {
    if let Err(ref jni_error) = *result {
        if let ErrorKind::JavaException = jni_error.0 {
            let exception: JObject = env.exception_occurred()?.into();
            env.exception_clear()?;
            assert!(!exception.is_null());
            panic!(
                "Java exception: {}\n{:#?}",
                get_class_name(env, exception)?,
                jni_error.backtrace()
            )
        }
    }
    Ok(())
}
