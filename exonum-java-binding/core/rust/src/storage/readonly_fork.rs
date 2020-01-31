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

use exonum_merkledb::generic::{ErasedAccess, GenericAccess, GenericRawAccess};
use jni::{objects::JObject, JNIEnv};

use std::panic;

use {handle, into_generic_raw_access, utils::unwrap_exc_or_default, Handle};

/// Creates `ReadonlyFork` from base access (passed as `access_handle`).
///
/// Throws exception and returns null if creation of `ReadonlyFork` is not possible.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_database_ReadonlyFork_nativeCreate(
    env: JNIEnv,
    _: JObject,
    access_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let access = handle::cast_handle::<ErasedAccess>(access_handle);
        let readonly_fork = match access {
            GenericAccess::Raw(raw) => match raw {
                GenericRawAccess::Fork(fork) => fork.readonly(),
                GenericRawAccess::OwnedFork(fork) => fork.readonly(),
                _ => panic!("Attempt to create ReadonlyFork from non-fork: {:?}", access),
            },
            _ => panic!("Attempt to create ReadonlyFork from non-Fork: {:?}", access),
        };
        let readonly_fork = ErasedAccess::from(unsafe { into_generic_raw_access(readonly_fork) });
        let handle = handle::to_handle(readonly_fork);
        Ok(handle)
    });

    unwrap_exc_or_default(&env, res)
}
