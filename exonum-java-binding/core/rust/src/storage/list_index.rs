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

use exonum_merkledb::{
    access::AccessExt,
    generic::{ErasedAccess, GenericRawAccess},
    indexes::list::Iter,
    ListIndex,
};
use jni::{
    objects::{JClass, JObject, JString},
    sys::{jboolean, jbyteArray, jlong},
    JNIEnv,
};

use handle::{self, Handle};
use storage::Value;
use utils;

type Index = ListIndex<GenericRawAccess<'static>, Value>;

/// Returns pointer to the created `ListIndex` object.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ListIndexProxy_nativeCreate(
    env: JNIEnv,
    _: JClass,
    name: JString,
    id_in_group: jbyteArray,
    access_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let address = utils::convert_to_index_address(&env, name, id_in_group)?;
        let access = handle::cast_handle::<ErasedAccess>(access_handle);
        let index: Index = access.get_list(address);
        Ok(handle::to_handle(index))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Destroys the underlying `ListIndex` object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ListIndexProxy_nativeFree(
    env: JNIEnv,
    _: JClass,
    list_handle: Handle,
) {
    handle::drop_handle::<Index>(&env, list_handle);
}

/// Returns the value by index. Null pointer is returned if value is not found.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ListIndexProxy_nativeGet(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
    index: jlong,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let list = handle::cast_handle::<Index>(list_handle);
        let value = list.get(index as u64);
        match value {
            Some(value) => env.byte_array_from_slice(&value),
            None => Ok(ptr::null_mut()),
        }
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Returns the last value or null pointer if the list is empty.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ListIndexProxy_nativeGetLast(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let list = handle::cast_handle::<Index>(list_handle);
        let value = list.last();
        match value {
            Some(value) => env.byte_array_from_slice(&value),
            None => Ok(ptr::null_mut()),
        }
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Returns `true` if the list is empty.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ListIndexProxy_nativeIsEmpty(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let list = handle::cast_handle::<Index>(list_handle);
        let is_empty = list.is_empty();
        Ok(is_empty as jboolean)
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns length of the list.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ListIndexProxy_nativeSize(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
) -> jlong {
    let res = panic::catch_unwind(|| {
        let list = handle::cast_handle::<Index>(list_handle);
        let len = list.len();
        Ok(len as jlong)
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns pointer to the iterator over list.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ListIndexProxy_nativeCreateIter(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let list = handle::cast_handle::<Index>(list_handle);
        let iter = list.iter();
        Ok(handle::to_handle(iter))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns pointer to the iterator over list starting at given index.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ListIndexProxy_nativeIterFrom(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
    index_from: jlong,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let list = handle::cast_handle::<Index>(list_handle);
        let iter = list.iter_from(index_from as u64);
        Ok(handle::to_handle(iter))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Adds value to the list.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ListIndexProxy_nativeAdd(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
    value: jbyteArray,
) {
    let res = panic::catch_unwind(|| {
        let list = handle::cast_handle::<Index>(list_handle);
        let value = env.convert_byte_array(value)?;
        list.push(value);
        Ok(())
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Removes the last element from a list and returns it, or null pointer if it is empty.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ListIndexProxy_nativeRemoveLast(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let list = handle::cast_handle::<Index>(list_handle);
        let value = list.pop();
        match value {
            Some(value) => env.byte_array_from_slice(&value),
            None => Ok(ptr::null_mut()),
        }
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Shortens the list, keeping the first len elements and dropping the rest.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ListIndexProxy_nativeTruncate(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
    len: jlong,
) {
    let res = panic::catch_unwind(|| {
        let list = handle::cast_handle::<Index>(list_handle);
        list.truncate(len as u64);
        Ok(())
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Sets value into specified index. Panics if `i` is out of bounds.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ListIndexProxy_nativeSet(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
    index: jlong,
    value: jbyteArray,
) {
    let res = panic::catch_unwind(|| {
        let list = handle::cast_handle::<Index>(list_handle);
        let value = env.convert_byte_array(value)?;
        list.set(index as u64, value);
        Ok(())
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Clears the list, removing all values.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ListIndexProxy_nativeClear(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
) {
    let res = panic::catch_unwind(|| {
        let list = handle::cast_handle::<Index>(list_handle);
        list.clear();
        Ok(())
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns next value from the iterator. Returns null pointer when iteration is finished.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ListIndexProxy_nativeIterNext(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let iter = handle::cast_handle::<Iter<Value>>(iter_handle);
        match iter.next() {
            Some(val) => env.byte_array_from_slice(&val),
            None => Ok(ptr::null_mut()),
        }
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Destroys the underlying `IndexList` iterator object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ListIndexProxy_nativeIterFree(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) {
    handle::drop_handle::<Iter<Value>>(&env, iter_handle);
}
