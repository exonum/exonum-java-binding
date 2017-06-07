use jni::JNIEnv;
use jni::objects::JClass;

use std::panic;

use blockchain_explorer::helpers;
use utils;

/// Performs the logger initialization.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_binding_ClassNameTODO_nativeInitLogger(env: JNIEnv, _: JClass) {
    let res = panic::catch_unwind(|| {
                                      // Ignore logger initialization failure.
                                      let _ = helpers::init_logger();
                                  });
    utils::unwrap_or_exception(&env, res);
}

// TODO: exonum::crypto::init()?
