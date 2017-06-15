use jni::JNIEnv;
use jni::objects::JClass;

use std::panic;

use exonum::helpers;
use utils;

/// Performs the logger initialization.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_binding_ClassNameTODO_nativeInitLogger(env: JNIEnv, _: JClass) {
    let res = panic::catch_unwind(|| {
                                      // Ignore logger initialization failure.
                                      let _ = helpers::init_logger();
                                  });
    utils::unwrap_exc_or_default(&env, res);
}

// TODO: exonum::crypto::init()?
