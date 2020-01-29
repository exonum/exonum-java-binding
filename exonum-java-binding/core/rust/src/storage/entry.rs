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

use exonum_merkledb::{
    access::AccessExt,
    generic::{ErasedAccess, GenericRawAccess},
    Entry,
};
use jni::{
    objects::{JClass, JObject, JString},
    sys::{jboolean, jbyteArray},
    JNIEnv,
};

use std::{panic, ptr};

use handle::{self, Handle};
use storage::db::Value;
use utils;

type Index = Entry<GenericRawAccess<'static>, Value>;

/// Returns pointer to the created `Entry` object.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_EntryIndexProxy_nativeCreate(
    env: JNIEnv,
    _: JClass,
    address: JString,
    view_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let address = utils::convert_to_string(&env, address)?;
        let access = handle::cast_handle::<ErasedAccess>(view_handle);
        let index: Index = access.get_entry(address);
        Ok(handle::to_handle(index))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Destroys the underlying `Entry` object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_EntryIndexProxy_nativeFree(
    env: JNIEnv,
    _: JClass,
    entry_handle: Handle,
) {
    handle::drop_handle::<Index>(&env, entry_handle);
}

/// Returns the value or null pointer if it is absent.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_EntryIndexProxy_nativeGet(
    env: JNIEnv,
    _: JObject,
    entry_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let index = handle::cast_handle::<Index>(entry_handle);
        let value = index.get();
        match value {
            Some(val) => env.byte_array_from_slice(&val),
            None => Ok(ptr::null_mut()),
        }
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Returns `true` if the entry contains the value.
#[no_mangle]
pub extern "C" fn Java_com_exonum_binding_core_storage_indices_EntryIndexProxy_nativeIsPresent(
    env: JNIEnv,
    _: JObject,
    entry_handle: Handle,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let index = handle::cast_handle::<Index>(entry_handle);
        let exists = index.exists();
        Ok(exists as jboolean)
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Inserts value to the entry.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_EntryIndexProxy_nativeSet(
    env: JNIEnv,
    _: JObject,
    entry_handle: Handle,
    value: jbyteArray,
) {
    let res = panic::catch_unwind(|| {
        let index = handle::cast_handle::<Index>(entry_handle);
        let value = env.convert_byte_array(value)?;
        index.set(value);
        Ok(())
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Removes a value from the entry.
#[no_mangle]
pub extern "C" fn Java_com_exonum_binding_core_storage_indices_EntryIndexProxy_nativeRemove(
    env: JNIEnv,
    _: JObject,
    entry_handle: Handle,
) {
    let res = panic::catch_unwind(|| {
        let index = handle::cast_handle::<Index>(entry_handle);
        index.remove();
        Ok(())
    });
    utils::unwrap_exc_or_default(&env, res)
}
