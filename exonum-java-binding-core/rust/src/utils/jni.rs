use jni::objects::JObject;
use jni::signature::JavaType;
use jni::JNIEnv;

use utils::convert_to_string;
use utils::jni_cache;
use JniResult;

const RETVAL_TYPE_STRING: &str = "java/lang/String";
const RETVAL_TYPE_CLASS: &str = "java/lang/Class";

/// Returns a class name of an object as a `String`.
pub fn get_class_name(env: &JNIEnv, object: JObject) -> JniResult<String> {
    let class_object = unsafe {
        env.call_method_unsafe(
            object,
            jni_cache::get_object_get_class(),
            JavaType::Object(RETVAL_TYPE_CLASS.into()),
            &[],
        )
    }?.l()?;

    let class_name = unsafe {
        env.call_method_unsafe(
            class_object,
            jni_cache::get_class_get_name(),
            JavaType::Object(RETVAL_TYPE_STRING.into()),
            &[],
        )
    }?.l()?;
    convert_to_string(env, class_name)
}

/// Returns the message from the exception if it is not null.
///
/// `exception` should extend `java.lang.Throwable` and be not null
pub fn get_exception_message(env: &JNIEnv, exception: JObject) -> JniResult<Option<String>> {
    assert!(!exception.is_null(), "Invalid exception argument");
    let message = unsafe {
        env.call_method_unsafe(
            exception,
            jni_cache::get_throwable_get_message(),
            JavaType::Object(RETVAL_TYPE_STRING.into()),
            &[],
        )
    }?;
    let message = message.l()?;
    if message.is_null() {
        return Ok(None);
    }
    convert_to_string(env, message).map(Some)
}
