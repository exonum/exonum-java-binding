use jni::JNIEnv;
use jni::objects::JObject;

use JniResult;
use utils::convert_to_string;

#[cfg(windows)]
pub const PATH_SEPARATOR: &str = ";";
#[cfg(not(windows))]
pub const PATH_SEPARATOR: &str = ":";

/// Returns a joined with target rules path
///
/// Removes duplicated entries.
pub fn join_path(parts: &[&str]) -> String {
    let mut parts: Vec<&str> = parts
        .iter()
        .flat_map(|s| s.split(PATH_SEPARATOR))
        .filter(|s| !s.is_empty())
        .collect();
    parts.dedup_by(|a, b| a == b);
    parts.join(PATH_SEPARATOR)
}

/// Returns a class name of an object as a `String`.
pub fn get_class_name(env: &JNIEnv, object: JObject) -> JniResult<String> {
    let class_object = env.call_method(object, "getClass", "()Ljava/lang/Class;", &[])?
        .l()?;
    let class_name = env.call_method(class_object, "getName", "()Ljava/lang/String;", &[])?
        .l()?;
    convert_to_string(env, class_name)
}

pub fn get_exception_message(_env: &JNIEnv, exception: JObject) -> JniResult<String> {
    assert!(!exception.is_null(), "Invalid exception argument");
    // FIXME uncomment when the issue is fixed [https://jira.bf.local/browse/ECR-1035]
    //let message = env.call_method(
    //    exception,
    //    "getMessage",
    //    "()Ljava/lang/String;",
    //    &[],
    //)?;
    //let message = message.l()?;
    //if message.is_null() {
    //    return Ok(String::new());
    //}
    //convert_to_string(env, message)
    Ok(String::new())
}

pub fn get_exception_stack_trace(_env: &JNIEnv, exception: JObject) -> JniResult<String> {
    assert!(!exception.is_null(), "Invalid exception argument");
    // FIXME uncomment when the issue is fixed [https://jira.bf.local/browse/ECR-1035]
    //let frames = env.call_method(
    //    exception,
    //    "getStackTrace",
    //    "()[Ljava/lang/StackTraceElement;",
    //    &[],
    //)?
    //    .l()?;
    //if frames.is_null() {
    //    return Ok(String::new());
    //}
    //let frames = frames.into_inner();
    //let frames_len = env.get_array_length(frames)?;
    //let mut stack: Vec<String> = Vec::with_capacity(frames_len as usize);
    //for i in 0..frames_len {
    //    let frame = env.get_object_array_element(frames, i)?;
    //    let frame_string = env.call_method(
    //        frame,
    //        "toString",
    //        "()Ljava/lang/String;",
    //        &[],
    //    )?;
    //    let frame_string = format!("    at {}\n", convert_to_string(env, frame_string.l()?)?);
    //    stack.push(frame_string);
    //}
    //Ok(stack.join(""))
    Ok(String::new())
}
