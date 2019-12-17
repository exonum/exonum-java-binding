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
    access::FromAccess,
    proof_map_index::{
        ProofMapIndexIter, ProofMapIndexKeys, ProofMapIndexValues, PROOF_MAP_KEY_SIZE,
    },
    Fork, IndexAddress, ObjectHash, RawProofMapIndex, Snapshot,
};
use jni::{
    objects::{JClass, JObject, JString},
    sys::{jboolean, jbyteArray, jobject, jobjectArray},
    JNIEnv,
};

use std::{panic, ptr};

use handle::{self, Handle};
use storage::{
    db::{Value, View, ViewRef},
    PairIter,
};
use utils;
use JniResult;

type Key = [u8; PROOF_MAP_KEY_SIZE];
type Index<T> = RawProofMapIndex<T, Key, Value>;

const JAVA_ENTRY_FQN: &str = "com/exonum/binding/core/storage/indices/MapEntryInternal";

enum IndexType {
    SnapshotIndex(Index<&'static dyn Snapshot>),
    ForkIndex(Index<&'static Fork>),
}

type Iter<'a> = PairIter<ProofMapIndexIter<'a, Key, Value>>;

/// Returns a pointer to the created `RawProofMapIndex` object.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_RawProofMapIndexProxy_nativeCreate(
    env: JNIEnv,
    _: JClass,
    name: JString,
    view_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let name = utils::convert_to_string(&env, name)?;
        Ok(handle::to_handle(
            match handle::cast_handle::<View>(view_handle).get() {
                ViewRef::Snapshot(snapshot) => {
                    IndexType::SnapshotIndex(Index::from_access(snapshot, name.into()).unwrap())
                }
                ViewRef::Fork(fork) => {
                    IndexType::ForkIndex(Index::from_access(fork, name.into()).unwrap())
                }
            },
        ))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns a pointer to the created `RawProofMapIndex` instance in an index family (= group).
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_RawProofMapIndexProxy_nativeCreateInGroup(
    env: JNIEnv,
    _: JClass,
    group_name: JString,
    map_id: jbyteArray,
    view_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let group_name = utils::convert_to_string(&env, group_name)?;
        let map_id = env.convert_byte_array(map_id)?;
        let address = IndexAddress::with_root(group_name).append_bytes(&map_id);
        let view_ref = handle::cast_handle::<View>(view_handle).get();
        Ok(handle::to_handle(match view_ref {
            ViewRef::Snapshot(snapshot) => {
                IndexType::SnapshotIndex(Index::from_access(snapshot, address).unwrap())
            }
            ViewRef::Fork(fork) => IndexType::ForkIndex(Index::from_access(fork, address).unwrap()),
        }))
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
    handle::drop_handle::<IndexType>(&env, map_handle);
}

/// Returns the object hash of the proof map or default hash value if it is empty.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_RawProofMapIndexProxy_nativeGetIndexHash(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let hash = match *handle::cast_handle::<IndexType>(map_handle) {
            IndexType::SnapshotIndex(ref map) => map.object_hash(),
            IndexType::ForkIndex(ref map) => map.object_hash(),
        };
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
        let key = convert_to_key(&env, key)?;
        let val = match *handle::cast_handle::<IndexType>(map_handle) {
            IndexType::SnapshotIndex(ref map) => map.get(&key),
            IndexType::ForkIndex(ref map) => map.get(&key),
        };
        match val {
            Some(val) => env.byte_array_from_slice(&val),
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
        let key = convert_to_key(&env, key)?;
        Ok(match *handle::cast_handle::<IndexType>(map_handle) {
            IndexType::SnapshotIndex(ref map) => map.contains(&key),
            IndexType::ForkIndex(ref map) => map.contains(&key),
        } as jboolean)
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
) -> jobject {
    let res = panic::catch_unwind(|| {
        let key = convert_to_key(&env, key)?;
        let proof = match *handle::cast_handle::<IndexType>(map_handle) {
            IndexType::SnapshotIndex(ref map) => map.get_proof(key),
            IndexType::ForkIndex(ref map) => map.get_proof(key),
        };
        utils::proto_to_java_bytes(&env, proof)
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
) -> jobject {
    let res = panic::catch_unwind(|| {
        let keys = utils::java_arrays_to_rust(&env, keys, convert_to_key)?;
        let proof = match *handle::cast_handle::<IndexType>(map_handle) {
            IndexType::SnapshotIndex(ref map) => map.get_multiproof(keys),
            IndexType::ForkIndex(ref map) => map.get_multiproof(keys),
        };
        utils::proto_to_java_bytes(&env, proof)
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
        let iter = match *handle::cast_handle::<IndexType>(map_handle) {
            IndexType::SnapshotIndex(ref map) => map.iter(),
            IndexType::ForkIndex(ref map) => map.iter(),
        };
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
        Ok(handle::to_handle(
            match *handle::cast_handle::<IndexType>(map_handle) {
                IndexType::SnapshotIndex(ref map) => map.keys(),
                IndexType::ForkIndex(ref map) => map.keys(),
            },
        ))
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
        Ok(handle::to_handle(
            match *handle::cast_handle::<IndexType>(map_handle) {
                IndexType::SnapshotIndex(ref map) => map.values(),
                IndexType::ForkIndex(ref map) => map.values(),
            },
        ))
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
        let key = convert_to_key(&env, key)?;
        let iter = match *handle::cast_handle::<IndexType>(map_handle) {
            IndexType::SnapshotIndex(ref map) => map.iter_from(&key),
            IndexType::ForkIndex(ref map) => map.iter_from(&key),
        };
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
        let key = convert_to_key(&env, key)?;
        Ok(handle::to_handle(
            match *handle::cast_handle::<IndexType>(map_handle) {
                IndexType::SnapshotIndex(ref map) => map.keys_from(&key),
                IndexType::ForkIndex(ref map) => map.keys_from(&key),
            },
        ))
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
        let key = convert_to_key(&env, key)?;
        Ok(handle::to_handle(
            match *handle::cast_handle::<IndexType>(map_handle) {
                IndexType::SnapshotIndex(ref map) => map.values_from(&key),
                IndexType::ForkIndex(ref map) => map.values_from(&key),
            },
        ))
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
    let res = panic::catch_unwind(|| match *handle::cast_handle::<IndexType>(map_handle) {
        IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        IndexType::ForkIndex(ref mut map) => {
            let key = convert_to_key(&env, key)?;
            let value = env.convert_byte_array(value)?;
            map.put(&key, value);
            Ok(())
        }
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
    let res = panic::catch_unwind(|| match *handle::cast_handle::<IndexType>(map_handle) {
        IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        IndexType::ForkIndex(ref mut map) => {
            let key = convert_to_key(&env, key)?;
            map.remove(&key);
            Ok(())
        }
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
    let res = panic::catch_unwind(|| match *handle::cast_handle::<IndexType>(map_handle) {
        IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        IndexType::ForkIndex(ref mut map) => {
            map.clear();
            Ok(())
        }
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
        let iter = handle::cast_handle::<ProofMapIndexKeys<Key>>(iter_handle);
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
    handle::drop_handle::<ProofMapIndexKeys<Key>>(&env, iter_handle);
}

/// Return next value from the values-iterator. Returns null pointer when iteration is finished.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_RawProofMapIndexProxy_nativeValuesIterNext(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let iter = handle::cast_handle::<ProofMapIndexValues<Value>>(iter_handle);
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
    handle::drop_handle::<ProofMapIndexValues<Value>>(&env, iter_handle);
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
