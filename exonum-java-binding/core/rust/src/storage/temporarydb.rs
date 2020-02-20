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

use exonum::merkledb::{Database, Patch, TemporaryDB};
use jni::{
    objects::{JClass, JObject},
    JNIEnv,
};

use std::panic;

use crate::{
    handle::{self, Handle},
    storage::into_erased_access,
    utils,
};

/// Returns pointer to created `TemporaryDB` object.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_database_TemporaryDb_nativeCreate(
    env: JNIEnv,
    _: JClass,
) -> Handle {
    let res = panic::catch_unwind(|| Ok(handle::to_handle(TemporaryDB::new())));
    utils::unwrap_exc_or_default(&env, res)
}

/// Destroys underlying `TemporaryDB` object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_database_TemporaryDb_nativeFree(
    env: JNIEnv,
    _: JClass,
    db_handle: Handle,
) {
    handle::drop_handle::<TemporaryDB>(&env, db_handle);
}

/// Returns pointer to created `Snapshot` object.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_database_TemporaryDb_nativeCreateSnapshot(
    env: JNIEnv,
    _: JObject,
    db_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let db = handle::cast_handle::<TemporaryDB>(db_handle);
        let access = unsafe { into_erased_access(db.snapshot()) };
        Ok(handle::to_handle(access))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns pointer to created `Fork` object.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_database_TemporaryDb_nativeCreateFork(
    env: JNIEnv,
    _: JObject,
    db_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let db = handle::cast_handle::<TemporaryDB>(db_handle);
        let access = unsafe { into_erased_access(db.fork()) };
        Ok(handle::to_handle(access))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Merges the given patch into the database.
/// The provided `patch_handle` is invalidated after the procedure and the
/// Rust side is responsible for it.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_database_TemporaryDb_nativeMerge(
    env: JNIEnv,
    _: JObject,
    db_handle: Handle,
    patch_handle: Handle,
) {
    let res = panic::catch_unwind(|| {
        let db = handle::cast_handle::<TemporaryDB>(db_handle);
        let patch = handle::acquire_handle_ownership::<Patch>(patch_handle);
        db.merge(*patch).expect("Unable to merge patch");
        Ok(())
    });
    utils::unwrap_exc_or_default(&env, res)
}
