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
    access::{FromAccess, RawAccess},
    Fork,
    indexes::proof_map::{Hashed, Iter as IndexIter, Keys, PROOF_MAP_KEY_SIZE, Values}, ObjectHash, ProofMapIndex, RawProofMapIndex, Snapshot,
};
use exonum_proto::ProtobufConvert;
use jni::{
    JNIEnv,
    objects::{GlobalRef, JClass, JMethodID, JObject, JString},
    sys::{jboolean, jbyteArray, JNI_TRUE, jobject, jobjectArray},
};
use JniResult;
use protobuf::Message;

use handle::{self, Handle};
use storage::{
    db::{Key, Value, View, ViewRef},
    PairIter,
};
use utils;

type RawKey = [u8; PROOF_MAP_KEY_SIZE];

// Wrapper for an underlying ProofMapIndex that supports two types of keys:
//  1. RawKey - fixed-length 256 bits key that won't be hashed by ProofMapIndex
//  2. Key - variable-length array of bytes that will be hashed by ProofMapIndex
enum Index<T>
where
    T: RawAccess,
{
    Raw(RawProofMapIndex<T, RawKey, Value>),
    Hashed(ProofMapIndex<T, Key, Value>),
}

const MAP_ENTRY_INTERNAL_FQN: &str = "com/exonum/binding/core/storage/indices/MapEntryInternal";

enum IndexType {
    SnapshotIndex(Index<&'static dyn Snapshot>),
    ForkIndex(Index<&'static Fork>),
}

enum Iter<'a> {
    Raw(PairIter<IndexIter<'a, RawKey, Value>>),
    Hashed(PairIter<IndexIter<'a, Key, Value>>),
}

enum KeysIter<'a> {
    Raw(Keys<'a, RawKey>),
    Hashed(Keys<'a, Key>),
}

// For easy conversion to RawKey.
trait ToRawKey {
    fn to_raw(&self) -> RawKey;
}

impl<T: RawAccess> From<ProofMapIndex<T, Key, Value>> for Index<T> {
    fn from(map: ProofMapIndex<T, Key, Value>) -> Self {
        Index::Hashed(map)
    }
}

impl<T: RawAccess> From<RawProofMapIndex<T, RawKey, Value>> for Index<T> {
    fn from(map: RawProofMapIndex<T, RawKey, Value>) -> Self {
        Index::Raw(map)
    }
}

impl ToRawKey for Key {
    fn to_raw(&self) -> RawKey {
        assert_eq!(
            self.len(),
            PROOF_MAP_KEY_SIZE,
            "Key size should be 256 bits"
        );
        let mut result: RawKey = [0; 32];
        result.copy_from_slice(self.as_slice());
        result
    }
}

/// Returns a pointer to the created `ProofMapIndex` object.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofMapIndexProxy_nativeCreate(
    env: JNIEnv,
    _: JClass,
    name: JString,
    id_in_group: jbyteArray,
    view_handle: Handle,
    key_hashing: jboolean,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let address = utils::convert_to_index_address(&env, name, id_in_group)?;
        let key_is_hashed = key_hashing == JNI_TRUE;
        Ok(handle::to_handle(
            match handle::cast_handle::<View>(view_handle).get() {
                ViewRef::Snapshot(snapshot) => {
                    let index = if key_is_hashed {
                        ProofMapIndex::<_, _, _, Hashed>::from_access(snapshot, address)
                            .unwrap()
                            .into()
                    } else {
                        RawProofMapIndex::from_access(snapshot, address)
                            .unwrap()
                            .into()
                    };
                    IndexType::SnapshotIndex(index)
                }
                ViewRef::Fork(fork) => {
                    let index = if key_is_hashed {
                        ProofMapIndex::<_, _, _, Hashed>::from_access(fork, address)
                            .unwrap()
                            .into()
                    } else {
                        RawProofMapIndex::from_access(fork, address).unwrap().into()
                    };
                    IndexType::ForkIndex(index)
                }
            },
        ))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Destroys the underlying `ProofMapIndex` object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofMapIndexProxy_nativeFree(
    env: JNIEnv,
    _: JClass,
    map_handle: Handle,
) {
    handle::drop_handle::<IndexType>(&env, map_handle);
}

/// Returns the object hash of the proof map or default hash value if it is empty.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofMapIndexProxy_nativeGetIndexHash(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let hash = match *handle::cast_handle::<IndexType>(map_handle) {
            IndexType::SnapshotIndex(ref index) => match index {
                Index::Raw(map) => map.object_hash(),
                Index::Hashed(map) => map.object_hash(),
            },
            IndexType::ForkIndex(ref index) => match index {
                Index::Raw(map) => map.object_hash(),
                Index::Hashed(map) => map.object_hash(),
            },
        };
        utils::convert_hash(&env, &hash)
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Returns value identified by the `key`. Null pointer is returned if value is not found.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofMapIndexProxy_nativeGet(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
    key: jbyteArray,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let key = env.convert_byte_array(key)?;
        let val = match *handle::cast_handle::<IndexType>(map_handle) {
            IndexType::SnapshotIndex(ref index) => match index {
                Index::Raw(map) => map.get(&key.to_raw()),
                Index::Hashed(map) => map.get(&key),
            },
            IndexType::ForkIndex(ref index) => match index {
                Index::Raw(map) => map.get(&key.to_raw()),
                Index::Hashed(map) => map.get(&key),
            },
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
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofMapIndexProxy_nativeContainsKey(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
    key: jbyteArray,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let key = env.convert_byte_array(key)?;
        Ok(match *handle::cast_handle::<IndexType>(map_handle) {
            IndexType::SnapshotIndex(ref index) => match index {
                Index::Raw(map) => map.contains(&key.to_raw()),
                Index::Hashed(map) => map.contains(&key),
            },
            IndexType::ForkIndex(ref index) => match index {
                Index::Raw(map) => map.contains(&key.to_raw()),
                Index::Hashed(map) => map.contains(&key),
            },
        } as jboolean)
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns proof that is serialized in protobuf.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofMapIndexProxy_nativeGetProof(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
    key: jbyteArray,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let key = env.convert_byte_array(key)?;
        let proof_proto = match *handle::cast_handle::<IndexType>(map_handle) {
            IndexType::SnapshotIndex(ref index) => match index {
                Index::Raw(ref map) => map.get_proof(key.to_raw()).to_pb(),
                Index::Hashed(ref map) => map.get_proof(key).to_pb(),
            },
            IndexType::ForkIndex(ref index) => match index {
                Index::Raw(ref map) => map.get_proof(key.to_raw()).to_pb(),
                Index::Hashed(ref map) => map.get_proof(key).to_pb(),
            },
        };

        env.byte_array_from_slice(&proof_proto.write_to_bytes().unwrap())
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Returns multiproof that is serialized in protobuf.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofMapIndexProxy_nativeGetMultiProof(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
    keys: jobjectArray,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let keys = convert_to_keys(&env, keys)?;
        let proof_proto = match *handle::cast_handle::<IndexType>(map_handle) {
            IndexType::SnapshotIndex(ref index) => match index {
                Index::Raw(ref map) => map.get_multiproof(convert_keys(keys)).to_pb(),
                Index::Hashed(ref map) => map.get_multiproof(keys).to_pb(),
            },
            IndexType::ForkIndex(ref index) => match index {
                Index::Raw(ref map) => map.get_multiproof(convert_keys(keys)).to_pb(),
                Index::Hashed(ref map) => map.get_multiproof(keys).to_pb(),
            },
        };

        env.byte_array_from_slice(&proof_proto.write_to_bytes().unwrap())
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Returns the pointer to the iterator over a map keys and values.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofMapIndexProxy_nativeCreateEntriesIter(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let iter = match *handle::cast_handle::<IndexType>(map_handle) {
            IndexType::SnapshotIndex(ref index) => match index {
                Index::Raw(ref map) => Iter::Raw(create_pair_iter(&env, map.iter())?),
                Index::Hashed(ref map) => Iter::Hashed(create_pair_iter(&env, map.iter())?),
            },
            IndexType::ForkIndex(ref index) => match index {
                Index::Raw(ref map) => Iter::Raw(create_pair_iter(&env, map.iter())?),
                Index::Hashed(ref map) => Iter::Hashed(create_pair_iter(&env, map.iter())?),
            },
        };
        Ok(handle::to_handle(iter))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns the pointer to the iterator over map keys.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofMapIndexProxy_nativeCreateKeysIter(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let keys_iter = match handle::cast_handle::<IndexType>(map_handle) {
            IndexType::SnapshotIndex(index) => match index {
                Index::Raw(map) => KeysIter::Raw(map.keys()),
                Index::Hashed(map) => KeysIter::Hashed(map.keys()),
            },
            IndexType::ForkIndex(index) => match index {
                Index::Raw(map) => KeysIter::Raw(map.keys()),
                Index::Hashed(map) => KeysIter::Hashed(map.keys()),
            },
        };
        Ok(handle::to_handle(keys_iter))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns the pointer to the iterator over map values.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofMapIndexProxy_nativeCreateValuesIter(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let values = match *handle::cast_handle::<IndexType>(map_handle) {
            IndexType::SnapshotIndex(ref index) => match index {
                Index::Raw(map) => map.values(),
                Index::Hashed(map) => map.values(),
            },
            IndexType::ForkIndex(ref index) => match index {
                Index::Raw(map) => map.values(),
                Index::Hashed(map) => map.values(),
            },
        };
        Ok(handle::to_handle(values))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns the pointer to the iterator over a map keys and values starting at the given key.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofMapIndexProxy_nativeCreateIterFrom(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
    key: jbyteArray,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let key = env.convert_byte_array(key)?;
        let iter = match *handle::cast_handle::<IndexType>(map_handle) {
            IndexType::SnapshotIndex(ref index) => match index {
                Index::Raw(map) => Iter::Raw(create_pair_iter(&env, map.iter_from(&key.to_raw()))?),
                Index::Hashed(map) => Iter::Hashed(create_pair_iter(&env, map.iter_from(&key))?),
            },
            IndexType::ForkIndex(ref index) => match index {
                Index::Raw(map) => Iter::Raw(create_pair_iter(&env, map.iter_from(&key.to_raw()))?),
                Index::Hashed(map) => Iter::Hashed(create_pair_iter(&env, map.iter_from(&key))?),
            },
        };
        Ok(handle::to_handle(iter))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns the pointer to the iterator over map keys starting at the given key.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofMapIndexProxy_nativeKeysFrom(
    env: JNIEnv,
    _: JClass,
    map_handle: Handle,
    key: jbyteArray,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let key = env.convert_byte_array(key)?;
        let iter = match *handle::cast_handle::<IndexType>(map_handle) {
            IndexType::SnapshotIndex(ref index) => match index {
                Index::Raw(map) => KeysIter::Raw(map.keys_from(&key.to_raw())),
                Index::Hashed(map) => KeysIter::Hashed(map.keys_from(&key)),
            },
            IndexType::ForkIndex(ref index) => match index {
                Index::Raw(map) => KeysIter::Raw(map.keys_from(&key.to_raw())),
                Index::Hashed(map) => KeysIter::Hashed(map.keys_from(&key)),
            },
        };
        Ok(handle::to_handle(iter))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns the pointer to the iterator over map values starting at the given key.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofMapIndexProxy_nativeValuesFrom(
    env: JNIEnv,
    _: JClass,
    map_handle: Handle,
    key: jbyteArray,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let key = env.convert_byte_array(key)?;
        let values = match *handle::cast_handle::<IndexType>(map_handle) {
            IndexType::SnapshotIndex(ref index) => match index {
                Index::Raw(map) => map.values_from(&key.to_raw()),
                Index::Hashed(map) => map.values_from(&key),
            },
            IndexType::ForkIndex(ref index) => match index {
                Index::Raw(map) => map.values_from(&key.to_raw()),
                Index::Hashed(map) => map.values_from(&key),
            },
        };
        Ok(handle::to_handle(values))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Sets `value` identified by the `key` into the index.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofMapIndexProxy_nativePut(
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
        IndexType::ForkIndex(ref mut index) => {
            let key = env.convert_byte_array(key)?;
            let value = env.convert_byte_array(value)?;
            match index {
                Index::Raw(map) => map.put(&key.to_raw(), value),
                Index::Hashed(map) => map.put(&key, value),
            }
            Ok(())
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Removes value identified by the `key` from the index.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofMapIndexProxy_nativeRemove(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
    key: jbyteArray,
) {
    let res = panic::catch_unwind(|| match *handle::cast_handle::<IndexType>(map_handle) {
        IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        IndexType::ForkIndex(ref mut index) => {
            let key = env.convert_byte_array(key)?;
            match index {
                Index::Raw(map) => map.remove(&key.to_raw()),
                Index::Hashed(map) => map.remove(&key),
            }
            Ok(())
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Removes all entries of the map.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofMapIndexProxy_nativeClear(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
) {
    let res = panic::catch_unwind(|| match *handle::cast_handle::<IndexType>(map_handle) {
        IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        IndexType::ForkIndex(ref mut index) => {
            match index {
                Index::Raw(map) => map.clear(),
                Index::Hashed(map) => map.clear(),
            }
            Ok(())
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns the next value from the iterator. Returns null pointer when iteration is finished.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofMapIndexProxy_nativeEntriesIterNext(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) -> jobject {
    let res = panic::catch_unwind(|| {
        let iterWrapper = handle::cast_handle::<Iter>(iter_handle);

        let result = match iterWrapper {
            Iter::Raw(ref mut wrapper) => wrapper.iter.next().map(|(arr, val)| {
                create_element(
                    &env,
                    &arr[..],
                    &val,
                    &wrapper.element_class,
                    wrapper.constructor_id,
                )
            }),
            Iter::Hashed(ref mut wrapper) => wrapper.iter.next().map(|(key, val)| {
                create_element(
                    &env,
                    key.as_slice(),
                    &val,
                    &wrapper.element_class,
                    wrapper.constructor_id,
                )
            }),
        };

        result.or(Some(Ok(ptr::null_mut()))).unwrap()
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Destroys the underlying `ProofMapIndex` iterator object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofMapIndexProxy_nativeEntriesIterFree(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) {
    handle::drop_handle::<Iter>(&env, iter_handle);
}

/// Returns the next value from the keys-iterator. Returns null pointer when iteration is finished.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofMapIndexProxy_nativeKeysIterNext(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let array = match *handle::cast_handle::<KeysIter>(iter_handle) {
            KeysIter::Raw(ref mut iter) => iter.next().map(|val| env.byte_array_from_slice(&val)),
            KeysIter::Hashed(ref mut iter) => {
                iter.next().map(|val| env.byte_array_from_slice(&val))
            }
        };
        array.or(Some(Ok(ptr::null_mut()))).unwrap()
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Destroys the underlying `ProofMapIndex` keys-iterator object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofMapIndexProxy_nativeKeysIterFree(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) {
    handle::drop_handle::<KeysIter>(&env, iter_handle);
}

/// Return next value from the values-iterator. Returns null pointer when iteration is finished.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofMapIndexProxy_nativeValuesIterNext(
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

/// Destroys the underlying `ProofMapIndex` values-iterator object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofMapIndexProxy_nativeValuesIterFree(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) {
    handle::drop_handle::<Values<Value>>(&env, iter_handle);
}

// Converts array of Java bytes arrays to the vector of keys.
fn convert_to_keys(env: &JNIEnv, array: jobjectArray) -> JniResult<Vec<Key>> {
    let num_elements = env.get_array_length(array)?;
    let mut keys = Vec::with_capacity(num_elements as usize);
    for i in 0..num_elements {
        let array_element = env.auto_local(env.get_object_array_element(array, i)?);
        let key = env.convert_byte_array(array_element.as_obj().into_inner())?;
        keys.push(key);
    }
    Ok(keys)
}

// Converts vector of Keys to Vector of RawKeys.
fn convert_keys(keys: Vec<Key>) -> Vec<RawKey> {
    keys.into_iter().map(|key| key.to_raw()).collect()
}

// Creates element for PairIter.
fn create_element(
    env: &JNIEnv,
    key: &[u8],
    value: &[u8],
    class: &GlobalRef,
    constructor: JMethodID,
) -> JniResult<jobject> {
    let key: JObject = env.byte_array_from_slice(key)?.into();
    let value: JObject = env.byte_array_from_slice(value)?.into();
    Ok(env
        .new_object_unchecked(class, constructor, &[key.into(), value.into()])?
        .into_inner())
}

// Creates PairIter for corresponding iterator and map entry.
fn create_pair_iter<I: Iterator>(env: &JNIEnv, iter: I) -> JniResult<PairIter<I>> {
    PairIter::new(&env, iter, MAP_ENTRY_INTERNAL_FQN)
}
