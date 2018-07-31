use jni::objects::JObject;
use jni::JNIEnv;

use utils::convert_to_string;
use JniResult;

/// Returns a class name of an object as a `String`.
pub fn get_class_name(env: &JNIEnv, object: JObject) -> JniResult<String> {
    let class_object = env.call_method(object, "getClass", "()Ljava/lang/Class;", &[])?
        .l()?;
    let class_name = env.call_method(class_object, "getName", "()Ljava/lang/String;", &[])?
        .l()?;
    convert_to_string(env, class_name)
}

/// Returns the message from the exception if it is not null.
///
/// `exception` should extend `java.lang.Throwable` and be not null
pub fn get_exception_message(env: &JNIEnv, exception: JObject) -> JniResult<Option<String>> {
    assert!(!exception.is_null(), "Invalid exception argument");
    let message = env.call_method(exception, "getMessage", "()Ljava/lang/String;", &[])?;
    let message = message.l()?;
    if message.is_null() {
        return Ok(None);
    }
    convert_to_string(env, message).map(Some)
}
