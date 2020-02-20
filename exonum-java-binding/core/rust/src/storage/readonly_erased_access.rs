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

use exonum::merkledb::{
    generic::{ErasedAccess, GenericAccess},
    AsReadonly,
};
use jni::{objects::JClass, JNIEnv};

use std::panic;

use crate::{handle, into_erased_access, utils::unwrap_exc_or_default, Handle};

/// Creates a readonly access from the base access (passed as `access_handle`).
///
/// Throws exception and returns null if creation of a readonly access is not possible.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_database_RoErasedAccess_nativeAsReadonly(
    env: JNIEnv,
    _: JClass,
    access_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let access = handle::cast_handle::<ErasedAccess>(access_handle);
        let readonly_access = match access {
            GenericAccess::Raw(raw) => {
                let readonly_access = raw.as_readonly();
                unsafe { into_erased_access(readonly_access) }
            }
            _ => panic!(
                "Attempt to create readonly Access from an unsupported type: {:?}",
                access
            ),
        };
        let handle = handle::to_handle(readonly_access);
        Ok(handle)
    });

    unwrap_exc_or_default(&env, res)
}
