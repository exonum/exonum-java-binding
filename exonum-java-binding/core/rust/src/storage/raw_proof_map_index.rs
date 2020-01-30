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
    indexes::proof_map::{Iter as IndexIter, Keys, Values, PROOF_MAP_KEY_SIZE},
    ObjectHash, RawProofMapIndex,
};
use jni::{
    objects::{JClass, JObject, JString},
    sys::{jboolean, jbyteArray, jobject, jobjectArray},
    JNIEnv,
};
use JniResult;

use handle::{self, Handle};
use storage::{db::Value, PairIter};
use utils;

type Key = [u8; PROOF_MAP_KEY_SIZE];
type Index = RawProofMapIndex<GenericRawAccess<'static>, Key, Value>;

const JAVA_ENTRY_FQN: &str = "com/exonum/binding/core/storage/indices/MapEntryInternal";

type Iter<'a> = PairIter<IndexIter<'a, Key, Value>>;

/// Returns a pointer to the created `RawProofMapIndex` object.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_RawProofMapIndexProxy_nativeCreate(
    env: JNIEnv,
    _: JClass,
    name: JString,
    id_in_group: jbyteArray,
    access_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let address = utils::convert_to_index_address(&env, name, id_in_group)?;
        let access = handle::cast_handle::<ErasedAccess>(access_handle);
        let index: Index = access.get_raw_proof_map(address);
        Ok(handle::to_handle(index))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Destroys the underlying `RawProofMapIndex` object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_RawProofMapIndexProxy_nativeFree(
    env: JNIEnv,
    _: JClass,
    map_handle: Handle,
) {
    handle::drop_handle::<Index>(&env, map_handle);
}

/// Returns the object hash of the proof map or default hash value if it is empty.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_RawProofMapIndexProxy_nativeGetIndexHash(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let map = handle::cast_handle::<Index>(map_handle);
        let hash = map.object_hash();
        utils::convert_hash(&env, &hash)
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Returns value identified by the `key`. Null pointer is returned if value is not found.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_RawProofMapIndexProxy_nativeGet(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
    key: jbyteArray,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let map = handle::cast_handle::<Index>(map_handle);
        let key = convert_to_key(&env, key)?;
        let value = map.get(&key);
        match value {
            Some(value) => env.byte_array_from_slice(&value),
            None => Ok(ptr::null_mut()),
        }
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Returns `true` if the map contains a value for the specified key.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_RawProofMapIndexProxy_nativeContainsKey(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
    key: jbyteArray,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let map = handle::cast_handle::<Index>(map_handle);
        let key = convert_to_key(&env, key)?;
        let contains = map.contains(&key);
        Ok(contains as jboolean)
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns Java-proof object.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_RawProofMapIndexProxy_nativeGetProof(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
    key: jbyteArray,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let map = handle::cast_handle::<Index>(map_handle);
        let key = convert_to_key(&env, key)?;
        let proof = map.get_proof(key);
        utils::proto_to_java_bytes(&env, &proof)
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Returns Java-proof object.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_RawProofMapIndexProxy_nativeGetMultiProof(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
    keys: jobjectArray,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let map = handle::cast_handle::<Index>(map_handle);
        let keys = utils::java_arrays_to_rust(&env, keys, convert_to_key)?;
        let proof = map.get_multiproof(keys);
        utils::proto_to_java_bytes(&env, &proof)
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Returns the pointer to the iterator over a map keys and values.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_RawProofMapIndexProxy_nativeCreateEntriesIter(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let map = handle::cast_handle::<Index>(map_handle);
        let iter = map.iter();
        let iter = Iter::new(&env, iter, JAVA_ENTRY_FQN)?;
        Ok(handle::to_handle(iter))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns the pointer to the iterator over map keys.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_RawProofMapIndexProxy_nativeCreateKeysIter(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let map = handle::cast_handle::<Index>(map_handle);
        let keys = map.keys();
        Ok(handle::to_handle(keys))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns the pointer to the iterator over map values.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_RawProofMapIndexProxy_nativeCreateValuesIter(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let map = handle::cast_handle::<Index>(map_handle);
        let values = map.values();
        Ok(handle::to_handle(values))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns the pointer to the iterator over a map keys and values starting at the given key.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_RawProofMapIndexProxy_nativeCreateIterFrom(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
    key: jbyteArray,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let map = handle::cast_handle::<Index>(map_handle);
        let key = convert_to_key(&env, key)?;
        let iter = map.iter_from(&key);
        let iter = Iter::new(&env, iter, JAVA_ENTRY_FQN)?;
        Ok(handle::to_handle(iter))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns the pointer to the iterator over map keys starting at the given key.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_RawProofMapIndexProxy_nativeKeysFrom(
    env: JNIEnv,
    _: JClass,
    map_handle: Handle,
    key: jbyteArray,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let map = handle::cast_handle::<Index>(map_handle);
        let key = convert_to_key(&env, key)?;
        let keys = map.keys_from(&key);
        Ok(handle::to_handle(keys))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns the pointer to the iterator over map values starting at the given key.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_RawProofMapIndexProxy_nativeValuesFrom(
    env: JNIEnv,
    _: JClass,
    map_handle: Handle,
    key: jbyteArray,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let map = handle::cast_handle::<Index>(map_handle);
        let key = convert_to_key(&env, key)?;
        let values = map.values_from(&key);
        Ok(handle::to_handle(values))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Sets `value` identified by the `key` into the index.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_RawProofMapIndexProxy_nativePut(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
    key: jbyteArray,
    value: jbyteArray,
) {
    let res = panic::catch_unwind(|| {
        let map = handle::cast_handle::<Index>(map_handle);
        let key = convert_to_key(&env, key)?;
        let value = env.convert_byte_array(value)?;
        map.put(&key, value);
        Ok(())
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Removes value identified by the `key` from the index.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_RawProofMapIndexProxy_nativeRemove(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
    key: jbyteArray,
) {
    let res = panic::catch_unwind(|| {
        let map = handle::cast_handle::<Index>(map_handle);
        let key = convert_to_key(&env, key)?;
        map.remove(&key);
        Ok(())
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Removes all entries of the map.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_RawProofMapIndexProxy_nativeClear(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
) {
    let res = panic::catch_unwind(|| {
        let map = handle::cast_handle::<Index>(map_handle);
        map.clear();
        Ok(())
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns the next value from the iterator. Returns null pointer when iteration is finished.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_RawProofMapIndexProxy_nativeEntriesIterNext(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) -> jobject {
    let res = panic::catch_unwind(|| {
        let iterWrapper = handle::cast_handle::<Iter>(iter_handle);
        match iterWrapper.iter.next() {
            Some(val) => {
                let key: JObject = env.byte_array_from_slice(&val.0)?.into();
                let value: JObject = env.byte_array_from_slice(&val.1)?.into();
                Ok(env
                    .new_object_unchecked(
                        &iterWrapper.element_class,
                        iterWrapper.constructor_id,
                        &[key.into(), value.into()],
                    )?
                    .into_inner())
            }
            None => Ok(ptr::null_mut()),
        }
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Destroys the underlying `RawProofMapIndex` iterator object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_RawProofMapIndexProxy_nativeEntriesIterFree(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) {
    handle::drop_handle::<Iter>(&env, iter_handle);
}

/// Returns the next value from the keys-iterator. Returns null pointer when iteration is finished.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_RawProofMapIndexProxy_nativeKeysIterNext(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let iter = handle::cast_handle::<Keys<Key>>(iter_handle);
        match iter.next() {
            Some(val) => env.byte_array_from_slice(&val),
            None => Ok(ptr::null_mut()),
        }
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Destroys the underlying `RawProofMapIndex` keys-iterator object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_RawProofMapIndexProxy_nativeKeysIterFree(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) {
    handle::drop_handle::<Keys<Key>>(&env, iter_handle);
}

/// Return next value from the values-iterator. Returns null pointer when iteration is finished.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_RawProofMapIndexProxy_nativeValuesIterNext(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let iter = handle::cast_handle::<Values<Value>>(iter_handle);
        match iter.next() {
            Some(val) => env.byte_array_from_slice(&val),
            None => Ok(ptr::null_mut()),
        }
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Destroys the underlying `RawProofMapIndex` values-iterator object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_RawProofMapIndexProxy_nativeValuesIterFree(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) {
    handle::drop_handle::<Values<Value>>(&env, iter_handle);
}

// Converts Java byte array to key.
fn convert_to_key(env: &JNIEnv, array: jbyteArray) -> JniResult<Key> {
    let key = env.convert_byte_array(array)?;
    assert_eq!(
        key.len(),
        PROOF_MAP_KEY_SIZE,
        "Key size expected to be {} bytes, found {} bytes",
        PROOF_MAP_KEY_SIZE,
        key.len()
    );
    let mut result: Key = [0; PROOF_MAP_KEY_SIZE];
    result.copy_from_slice(key.as_slice());
    Ok(result)
}
