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
    indexes::Values as Iter,
    ObjectHash, ProofListIndex,
};
use jni::{
    objects::{JClass, JObject, JString},
    sys::{jboolean, jbyteArray, jint, jlong},
    JNIEnv,
};

use handle::{self, Handle};
use storage::Value;
use utils;

type Index = ProofListIndex<GenericRawAccess<'static>, Value>;

/// Returns pointer to the created `ProofListIndex` object.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofListIndexProxy_nativeCreate(
    env: JNIEnv,
    _: JClass,
    name: JString,
    id_in_group: jbyteArray,
    access_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let address = utils::convert_to_index_address(&env, name, id_in_group)?;
        let access = handle::cast_handle::<ErasedAccess>(access_handle);
        let index: Index = access.get_proof_list(address);
        Ok(handle::to_handle(index))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Destroys the underlying `ProofListIndex` object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofListIndexProxy_nativeFree(
    env: JNIEnv,
    _: JClass,
    list_handle: Handle,
) {
    handle::drop_handle::<Index>(&env, list_handle);
}

/// Returns the value by index. Null pointer is returned if value is not found.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofListIndexProxy_nativeGet(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
    index: jlong,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let list = handle::cast_handle::<Index>(list_handle);
        let value = list.get(index as u64);
        utils::optional_array_to_java(&env, value)
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Returns the last value or null pointer if the list is empty.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofListIndexProxy_nativeGetLast(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let list = handle::cast_handle::<Index>(list_handle);
        let value = list.last();
        utils::optional_array_to_java(&env, value)
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Removes the last element from a list and returns it, or null pointer if it is empty.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofListIndexProxy_nativeRemoveLast(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let list = handle::cast_handle::<Index>(list_handle);
        let value = list.pop();
        utils::optional_array_to_java(&env, value)
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Shortens the list, keeping the first len elements and dropping the rest.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofListIndexProxy_nativeTruncate(
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

/// Returns `true` if the list is empty.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofListIndexProxy_nativeIsEmpty(
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
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofListIndexProxy_nativeSize(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
) -> jlong {
    let res = panic::catch_unwind(|| Ok(get_list_length(list_handle) as jlong));
    utils::unwrap_exc_or_default(&env, res)
}

fn get_list_length(list_handle: Handle) -> u64 {
    let list = handle::cast_handle::<Index>(list_handle);
    list.len()
}

/// Returns the height of the proof list.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofListIndexProxy_nativeHeight(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
) -> jint {
    let res = panic::catch_unwind(|| {
        let list = handle::cast_handle::<Index>(list_handle);
        let height = list.height();
        Ok(i32::from(height))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns the object hash of the proof list or default hash value if it is empty.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofListIndexProxy_nativeGetIndexHash(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let list = handle::cast_handle::<Index>(list_handle);
        let hash = list.object_hash();
        utils::convert_hash(&env, &hash)
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Returns the proof that an element exists at the specified index. The proof is serialized in
/// the protobuf format.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofListIndexProxy_nativeGetProof(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
    index: jlong,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let list = handle::cast_handle::<Index>(list_handle);
        let proof = list.get_proof(index as u64);
        utils::proto_to_java_bytes(&env, &proof)
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Returns the proof that some elements exists in the specified range. The proof is serialized in
/// the protobuf format.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofListIndexProxy_nativeGetRangeProof(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
    from: jlong,
    to: jlong,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let list = handle::cast_handle::<Index>(list_handle);
        let proof = list.get_range_proof(from as u64..to as u64);
        utils::proto_to_java_bytes(&env, &proof)
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Returns pointer to the iterator over list.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofListIndexProxy_nativeCreateIter(
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
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofListIndexProxy_nativeIterFrom(
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
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofListIndexProxy_nativeAdd(
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

/// Sets value into specified index. Panics if `i` is out of bounds.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofListIndexProxy_nativeSet(
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
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofListIndexProxy_nativeClear(
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
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofListIndexProxy_nativeIterNext(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let iter = handle::cast_handle::<Iter<Value>>(iter_handle);
        utils::optional_array_to_java(&env, iter.next())
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Destroys the underlying `ProofListIndex` iterator object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofListIndexProxy_nativeIterFree(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) {
    handle::drop_handle::<Iter<Value>>(&env, iter_handle);
}
