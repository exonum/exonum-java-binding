use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::jlong;

use std::panic;

use exonum::helpers;
use utils;

/// Performs the logger initialization.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_ClassNameTODO_nativeInitLogger(
    env: JNIEnv,
    _: JClass,
) {
    let res = panic::catch_unwind(|| {
        // Ignore logger initialization failure.
        let _ = helpers::init_logger();
        Ok(())
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns the number of the resource manager handles.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_ClassNameTODO_nativeKnownHandles(
    env: JNIEnv,
    _: JClass,
) -> jlong {
    let res = panic::catch_unwind(|| Ok(utils::known_handles() as jlong));
    utils::unwrap_exc_or_default(&env, res)
}
