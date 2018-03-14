use jni::JNIEnv;
use jni::errors::Result as JniResult;
use jni::objects::JObject;

use super::convert_to_string;

/// Returns a class name of an object as a `String`.
pub fn get_class_name(env: &JNIEnv, object: JObject) -> JniResult<String> {
    let class_object = env.call_method(object, "getClass", "()Ljava/lang/Class;", &[])?
        .l()?;
    let class_name = env.call_method(class_object, "getName", "()Ljava/lang/String;", &[])?
        .l()?;
    convert_to_string(env, class_name)
}
