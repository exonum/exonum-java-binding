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
    let message = env.call_method(
        exception,
        "getMessage",
        "()Ljava/lang/String;",
        &[],
    )?;
    let message = message.l()?;
    if message.is_null() {
        return Ok(None);
    }
    convert_to_string(env, message).map(Some)
}

/// Returns a string with a formatted stack trace, if available.
///
/// `exception` should extend `java.lang.Throwable` and be not null
pub fn get_exception_stack_trace(env: &JNIEnv, exception: JObject) -> JniResult<Option<String>> {
    assert!(!exception.is_null(), "Invalid exception argument");
    let frames = env.call_method(
        exception,
        "getStackTrace",
        "()[Ljava/lang/StackTraceElement;",
        &[],
    )?
        .l()?;
    if frames.is_null() {
        return Ok(None);
    }
    let frames = frames.into_inner();
    let frames_len = env.get_array_length(frames)?;
    let mut stack: Vec<String> = Vec::with_capacity(frames_len as usize);
    for i in 0..frames_len {
        let frame = env.get_object_array_element(frames, i)?;
        let frame_string = env.call_method(
            frame,
            "toString",
            "()Ljava/lang/String;",
            &[],
        )?;
        let frame_string = format!("    at {}\n", convert_to_string(env, frame_string.l()?)?);
        stack.push(frame_string);
    }
    Ok(Some(stack.join("")))
}
