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

use jni::{objects::JObject, sys::jboolean, JNIEnv};

use std::panic;

use handle::{self, acquire_handle_ownership, Handle};
use storage::db::View;
use {to_handle, utils};

/// Creates checkpoint for Fork. Is a no-op for Snapshots.
///
/// See `View::create_checkpoint`.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_database_Fork_nativeCreateCheckpoint(
    env: JNIEnv,
    _: JObject,
    view_handle: Handle,
) {
    let res = panic::catch_unwind(|| {
        let view = handle::cast_handle::<View>(view_handle);
        view.create_checkpoint();
        Ok(())
    });
    utils::unwrap_exc_or(&env, res, ())
}

/// Rollbacks Fork. Is a no-op for Snapshots.
///
/// See `View::rollback`.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_database_Fork_nativeRollback(
    env: JNIEnv,
    _: JObject,
    view_handle: Handle,
) {
    let res = panic::catch_unwind(|| {
        let view = handle::cast_handle::<View>(view_handle);
        view.rollback();
        Ok(())
    });
    utils::unwrap_exc_or(&env, res, ())
}

/// Returns true if this View can be converted into patch.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_database_Fork_nativeCanConvertIntoPatch(
    env: JNIEnv,
    _: JObject,
    view_handle: Handle,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let view = handle::cast_handle::<View>(view_handle);
        Ok(view.can_convert_into_fork() as jboolean)
    });
    utils::unwrap_exc_or(&env, res, false as jboolean)
}

/// Converts View into patch and returns the handle to this patch.
/// Provided `view_handle` will be cleared and can no longer be used.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_database_Fork_nativeIntoPatch(
    env: JNIEnv,
    _: JObject,
    view_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let view: Box<View> = acquire_handle_ownership(view_handle);
        let fork = view.into_fork();
        let patch = fork.into_patch();
        Ok(to_handle(patch))
    });
    utils::unwrap_exc_or_default(&env, res)
}
