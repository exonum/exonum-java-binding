use jni::JNIEnv;
use jni::errors::Result as JNIResult;
use jni::objects::JObject;

/// Returns a class name of an object as a `String`.
pub fn get_class_name(env: &JNIEnv, object: JObject) -> JNIResult<String> {
    let class_object = env.call_method(object, "getClass", "()Ljava/lang/Class;", &[])?
        .l()?;
    let class_name = env.call_method(class_object, "getName", "()Ljava/lang/String;", &[])?
        .l()?;
    Ok(String::from(env.get_string(class_name.into())?))
}
