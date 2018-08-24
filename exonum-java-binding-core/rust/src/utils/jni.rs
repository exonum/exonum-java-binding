use jni::objects::JObject;
use jni::JNIEnv;

use utils::convert_to_string;
use JniResult;

#[cfg(windows)]
pub const PATH_SEPARATOR: &str = ";";
#[cfg(not(windows))]
pub const PATH_SEPARATOR: &str = ":";

/// Joins several classpaths into a single classpath, using the default path separator.
/// Preserves the relative order of class path entries.
pub fn join_paths(parts: &[&str]) -> String {
    parts.join(PATH_SEPARATOR)
}

/// Returns a class name of an object as a `String`.
pub fn get_class_name(env: &JNIEnv, object: JObject) -> JniResult<String> {
    let class_object = env
        .call_method(object, "getClass", "()Ljava/lang/Class;", &[])?
        .l()?;
    let class_name = env
        .call_method(class_object, "getName", "()Ljava/lang/String;", &[])?
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

#[cfg(test)]
mod tests {
    use super::*;
    const FOO: &str = "foo";
    const BAR: &str = "bar";
    const BAZ: &str = "baz";

    #[cfg(windows)]
    const FOO_BAR_BAZ: &str = "foo;bar;baz";
    #[cfg(not(windows))]
    const FOO_BAR_BAZ: &str = "foo:bar:baz";

    #[test]
    fn join_paths_preserves_order() {
        let result = join_paths(&[FOO, BAR, BAZ]);
        assert_eq!(result, FOO_BAR_BAZ);
    }
}
