use jni::JNIEnv;
use jni::objects::JClass;

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
    utils::unwrap_exc_or_default(&env, res);
}
