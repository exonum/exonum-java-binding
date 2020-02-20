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
    access::AccessExt,
    generic::{ErasedAccess, GenericRawAccess},
    ObjectHash, ProofEntry,
};
use jni::{
    objects::{JClass, JObject, JString},
    sys::{jboolean, jbyteArray},
    JNIEnv,
};

use std::{panic, ptr};

use crate::{
    handle::{self, Handle},
    storage::Value,
    utils,
};

type Index = ProofEntry<GenericRawAccess<'static>, Value>;

/// Returns pointer to the created `Entry` object.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofEntryIndexProxy_nativeCreate(
    env: JNIEnv,
    _: JClass,
    address: JString,
    access_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let address = utils::convert_to_string(&env, address)?;
        let access = handle::cast_handle::<ErasedAccess>(access_handle);
        let index: Index = access.get_proof_entry(address);
        Ok(handle::to_handle(index))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Destroys the underlying `Entry` object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofEntryIndexProxy_nativeFree(
    env: JNIEnv,
    _: JClass,
    entry_handle: Handle,
) {
    handle::drop_handle::<Index>(&env, entry_handle);
}

/// Returns the value or null pointer if it is absent.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofEntryIndexProxy_nativeGet(
    env: JNIEnv,
    _: JObject,
    entry_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let entry = handle::cast_handle::<Index>(entry_handle);
        let value = entry.get();
        utils::optional_array_to_java(&env, value)
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Returns `true` if the entry contains the value.
#[no_mangle]
pub extern "C" fn Java_com_exonum_binding_core_storage_indices_ProofEntryIndexProxy_nativeIsPresent(
    env: JNIEnv,
    _: JObject,
    entry_handle: Handle,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let entry = handle::cast_handle::<Index>(entry_handle);
        let exists = entry.exists();
        Ok(exists as jboolean)
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns the hash of the value or default hash if value is absent.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofEntryIndexProxy_nativeGetIndexHash(
    env: JNIEnv,
    _: JObject,
    entry_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let entry = handle::cast_handle::<Index>(entry_handle);
        let hash = entry.object_hash();
        utils::convert_hash(&env, &hash)
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Inserts value to the entry.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofEntryIndexProxy_nativeSet(
    env: JNIEnv,
    _: JObject,
    entry_handle: Handle,
    value: jbyteArray,
) {
    let res = panic::catch_unwind(|| {
        let entry = handle::cast_handle::<Index>(entry_handle);
        let value = env.convert_byte_array(value)?;
        entry.set(value);
        Ok(())
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Removes a value from the entry.
#[no_mangle]
pub extern "C" fn Java_com_exonum_binding_core_storage_indices_ProofEntryIndexProxy_nativeRemove(
    env: JNIEnv,
    _: JObject,
    entry_handle: Handle,
) {
    let res = panic::catch_unwind(|| {
        let entry = handle::cast_handle::<Index>(entry_handle);
        entry.remove();
        Ok(())
    });
    utils::unwrap_exc_or_default(&env, res)
}
