// Copyright 2018 The Exonum Team
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

use exonum::helpers;
use jni::objects::JClass;
use jni::sys::jlong;
use jni::JNIEnv;

use std::panic;

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
