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

use exonum::merkledb::generic::ErasedAccess;
use jni::{objects::JObject, sys::jboolean, JNIEnv};

use std::panic;

use crate::{
    handle::{self, acquire_handle_ownership, to_handle, Handle},
    storage::EjbAccessExt,
    utils,
};

/// Creates checkpoint for `Fork`.
///
/// Throws RuntimeException if the Access behind the provided handle does not support checkpoints.
///
/// See `EjbAccessExt::create_checkpoint`.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_database_Fork_nativeCreateCheckpoint(
    env: JNIEnv,
    _: JObject,
    access_handle: Handle,
) {
    let res = panic::catch_unwind(|| {
        let access = handle::cast_handle::<ErasedAccess>(access_handle);
        access.create_checkpoint();
        Ok(())
    });
    utils::unwrap_exc_or(&env, res, ())
}

/// Rollbacks `Fork`.
///
/// Throws RuntimeException if the Access behind the provided handle does not support rollbacks.
///
/// See `EjbAccessExt::rollback`.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_database_Fork_nativeRollback(
    env: JNIEnv,
    _: JObject,
    access_handle: Handle,
) {
    let res = panic::catch_unwind(|| {
        let access = handle::cast_handle::<ErasedAccess>(access_handle);
        access.rollback();
        Ok(())
    });
    utils::unwrap_exc_or(&env, res, ())
}

/// Returns true if this Access supports creating checkpoints and rollback.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_database_Fork_nativeCanRollback(
    env: JNIEnv,
    _: JObject,
    access_handle: Handle,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let access = handle::cast_handle::<ErasedAccess>(access_handle);
        Ok(access.can_rollback() as jboolean)
    });
    utils::unwrap_exc_or(&env, res, false as jboolean)
}

/// Returns true if this Access can be converted into patch.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_database_Fork_nativeCanConvertIntoPatch(
    env: JNIEnv,
    _: JObject,
    access_handle: Handle,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let access = handle::cast_handle::<ErasedAccess>(access_handle);
        Ok(access.can_convert_into_fork() as jboolean)
    });
    utils::unwrap_exc_or(&env, res, false as jboolean)
}

/// Converts Access into patch and returns the handle to this patch.
/// Provided `access_handle` will be cleared and can no longer be used.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_database_Fork_nativeIntoPatch(
    env: JNIEnv,
    _: JObject,
    access_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let access: Box<ErasedAccess> = acquire_handle_ownership(access_handle);
        let fork = access.into_fork();
        let patch = fork.into_patch();
        Ok(to_handle(patch))
    });
    utils::unwrap_exc_or_default(&env, res)
}
