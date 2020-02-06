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

use std::{panic, ptr};

use exonum::merkledb::{
    access::AccessExt,
    generic::{ErasedAccess, GenericRawAccess},
    indexes::Keys,
    KeySetIndex,
};
use jni::{
    objects::{JClass, JObject, JString},
    sys::{jboolean, jbyteArray},
    JNIEnv,
};

use handle::{self, Handle};
use storage::Key;
use utils;

type Index = KeySetIndex<GenericRawAccess<'static>, Key>;

type KeyIter<'a> = Keys<'a, Key>;

/// Returns pointer to created `KeySetIndex` object.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_KeySetIndexProxy_nativeCreate(
    env: JNIEnv,
    _: JClass,
    name: JString,
    id_in_group: jbyteArray,
    access_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let address = utils::convert_to_index_address(&env, name, id_in_group)?;
        let access = handle::cast_handle::<ErasedAccess>(access_handle);
        let index: Index = access.get_key_set(address);
        Ok(handle::to_handle(index))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Destroys underlying `KeySetIndex` object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_KeySetIndexProxy_nativeFree(
    env: JNIEnv,
    _: JClass,
    set_handle: Handle,
) {
    handle::drop_handle::<Index>(&env, set_handle);
}

/// Returns `true` if the set contains the specified value.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_KeySetIndexProxy_nativeContains(
    env: JNIEnv,
    _: JObject,
    set_handle: Handle,
    value: jbyteArray,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let set = handle::cast_handle::<Index>(set_handle);
        let value = env.convert_byte_array(value)?;
        let contains = set.contains(&value);
        (Ok(contains as jboolean))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns pointer to the iterator over set.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_KeySetIndexProxy_nativeCreateIterator(
    env: JNIEnv,
    _: JObject,
    set_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let set = handle::cast_handle::<Index>(set_handle);
        let iter = set.iter();
        Ok(handle::to_handle(iter))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns pointer to the iterator over set starting at the given key.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_KeySetIndexProxy_nativeCreateIteratorFrom(
    env: JNIEnv,
    _: JObject,
    set_handle: Handle,
    from: jbyteArray,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let set = handle::cast_handle::<Index>(set_handle);
        let from = env.convert_byte_array(from)?;
        let iter = set.iter_from(&from);
        Ok(handle::to_handle(iter))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Inserts value in the set.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_KeySetIndexProxy_nativeAdd(
    env: JNIEnv,
    _: JObject,
    set_handle: Handle,
    value: jbyteArray,
) {
    let res = panic::catch_unwind(|| {
        let set = handle::cast_handle::<Index>(set_handle);
        let value = env.convert_byte_array(value)?;
        set.insert(&value);
        Ok(())
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Removes value from the set.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_KeySetIndexProxy_nativeRemove(
    env: JNIEnv,
    _: JObject,
    set_handle: Handle,
    value: jbyteArray,
) {
    let res = panic::catch_unwind(|| {
        let set = handle::cast_handle::<Index>(set_handle);
        let value = env.convert_byte_array(value)?;
        set.remove(&value);
        Ok(())
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Clears the set, removing all values.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_KeySetIndexProxy_nativeClear(
    env: JNIEnv,
    _: JObject,
    set_handle: Handle,
) {
    let res = panic::catch_unwind(|| {
        let set = handle::cast_handle::<Index>(set_handle);
        set.clear();
        Ok(())
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Return next value from the iterator. Returns null pointer when iteration is finished.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_KeySetIndexProxy_nativeIteratorNext(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let iter = handle::cast_handle::<KeyIter>(iter_handle);
        utils::optional_array_to_java(&env, iter.next())
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Destroys underlying `KeySetIndex` iterator object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_KeySetIndexProxy_nativeIteratorFree(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) {
    handle::drop_handle::<KeyIter>(&env, iter_handle);
}
