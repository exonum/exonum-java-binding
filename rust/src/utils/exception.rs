use jni::JNIEnv;

use std::any::Any;
use std::thread::Result;
use std::error::Error;

// Returns value or "throws" exception. `error_val` is returned, because exception will be thrown
// at the Java side. So this function should be used only for the `panic::catch_unwind` result.
pub fn unwrap_exc_or<T>(env: &JNIEnv, res: Result<T>, error_val: T) -> T {
    match res {
        Err(ref e) => {
            throw(env, &any_to_string(e));
            error_val
        }
        Ok(val) => val,
    }
}

// Same as `unwrap_exc_or` but returns default value.
pub fn unwrap_exc_or_default<T: Default>(env: &JNIEnv, res: Result<T>) -> T {
    unwrap_exc_or(env, res, T::default())
}

// Calls a corresponding `JNIEnv` method, so exception will be thrown when execution returns to
// the Java side.
fn throw(env: &JNIEnv, description: &str) {
    // We cannot throw exception from this function, so errors should be written in log instead.
    let exception = match env.find_class("java/lang/RuntimeException") {
        Ok(val) => val,
        Err(e) => {
            error!(
                "Unable to find 'RuntimeException' class: {}",
                e.description()
            );
            return;
        }
    };
    match env.throw_new(exception, description) {
        Ok(_) => {}
        Err(e) => {
            error!(
                "Unable to find 'RuntimeException' class: {}",
                e.description()
            );
        }
    }
}

// Tries to get meaningful description from panic-error.
fn any_to_string(any: &Any) -> String {
    // TODO: jni::errors::Error?
    // TODO: Handle more types?
    if let Some(error) = any.downcast_ref::<Box<Error>>() {
        error.description().to_string()
    } else if let Some(s) = any.downcast_ref::<&str>() {
        s.to_string()
    } else {
        "Unknown error occured".to_string()
    }
}
