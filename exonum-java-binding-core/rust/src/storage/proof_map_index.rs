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

use jni::objects::{JClass, JObject, JString};
use jni::sys::{jboolean, jbyteArray, jobject};
use jni::JNIEnv;

use std::panic;
use std::ptr;

use exonum::crypto::Hash;
use exonum::storage::proof_map_index::{
    ProofMapIndexIter, ProofMapIndexKeys, ProofMapIndexValues, ProofPath, PROOF_MAP_KEY_SIZE,
};
use exonum::storage::{Fork, ProofMapIndex, Snapshot, StorageValue};

use storage::db::{Value, View, ViewRef};
use utils::{self, Handle, PairIter};
use JniResult;

type Key = [u8; PROOF_MAP_KEY_SIZE];
type Index<T> = ProofMapIndex<T, Key, Value>;

const MAP_PROOF_ENTRY_BRANCH: &str =
    "com/exonum/binding/storage/proofs/map/flat/MapProofEntryBranch";
const MAP_PROOF_ENTRY_LEAF: &str = "com/exonum/binding/storage/proofs/map/flat/MapProofEntryLeaf";
const MAP_PROOF_ENTRY: &str = "com/exonum/binding/storage/proofs/map/flat/MapProofEntry";
const UNCHECKED_FLAT_MAP_PROOF: &str =
    "com/exonum/binding/storage/proofs/map/flat/UncheckedFlatMapProof";
const UNCHECKED_FLAT_MAP_PROOF_SIG: &str =
    "([Lcom/exonum/binding/storage/proofs/map/flat/MapProofEntry;)Lcom/exonum/binding/storage/proofs/map/flat/UncheckedFlatMapProof;";

enum IndexType {
    SnapshotIndex(Index<&'static Snapshot>),
    ForkIndex(Index<&'static mut Fork>),
}

type Iter<'a> = PairIter<ProofMapIndexIter<'a, Key, Value>>;

const JAVA_ENTRY_FQN: &str = "com/exonum/binding/storage/indices/MapEntryInternal";

/// Returns a pointer to the created `ProofMapIndex` object.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofMapIndexProxy_nativeCreate(
    env: JNIEnv,
    _: JClass,
    name: JString,
    view_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let name = utils::convert_to_string(&env, name)?;
        Ok(utils::to_handle(
            match *utils::cast_handle::<View>(view_handle).get() {
                ViewRef::Snapshot(snapshot) => {
                    IndexType::SnapshotIndex(Index::new(name, &*snapshot))
                }
                ViewRef::Fork(ref mut fork) => IndexType::ForkIndex(Index::new(name, fork)),
            },
        ))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns a pointer to the created `ProofMapIndex` instance in an index family (= group).
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofMapIndexProxy_nativeCreateInGroup(
    env: JNIEnv,
    _: JClass,
    group_name: JString,
    map_id: jbyteArray,
    view_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let group_name = utils::convert_to_string(&env, group_name)?;
        let map_id = env.convert_byte_array(map_id)?;
        let view_ref = utils::cast_handle::<View>(view_handle).get();
        Ok(utils::to_handle(match *view_ref {
            ViewRef::Snapshot(snapshot) => {
                IndexType::SnapshotIndex(Index::new_in_family(group_name, &map_id, &*snapshot))
            }
            ViewRef::Fork(ref mut fork) => {
                IndexType::ForkIndex(Index::new_in_family(group_name, &map_id, fork))
            }
        }))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Destroys the underlying `ProofMapIndex` object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofMapIndexProxy_nativeFree(
    env: JNIEnv,
    _: JClass,
    map_handle: Handle,
) {
    utils::drop_handle::<IndexType>(&env, map_handle);
}

/// Returns the root hash of the proof map or default hash value if it is empty.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofMapIndexProxy_nativeGetRootHash(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let hash = match *utils::cast_handle::<IndexType>(map_handle) {
            IndexType::SnapshotIndex(ref map) => map.merkle_root(),
            IndexType::ForkIndex(ref map) => map.merkle_root(),
        };
        utils::convert_hash(&env, &hash)
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Returns value identified by the `key`. Null pointer is returned if value is not found.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofMapIndexProxy_nativeGet(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
    key: jbyteArray,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let key = convert_to_key(&env, key)?;
        let val = match *utils::cast_handle::<IndexType>(map_handle) {
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
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofMapIndexProxy_nativeContainsKey(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
    key: jbyteArray,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let key = convert_to_key(&env, key)?;
        Ok(match *utils::cast_handle::<IndexType>(map_handle) {
            IndexType::SnapshotIndex(ref map) => map.contains(&key),
            IndexType::ForkIndex(ref map) => map.contains(&key),
        } as jboolean)
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns Java-proof object.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofMapIndexProxy_nativeGetProof(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
    key: jbyteArray,
) -> jobject {
    let res = panic::catch_unwind(|| {
        let key = convert_to_key(&env, key)?;
        let proof = match *utils::cast_handle::<IndexType>(map_handle) {
            IndexType::SnapshotIndex(ref map) => map.get_proof(key),
            IndexType::ForkIndex(ref map) => map.get_proof(key),
        };

        let unchecked_entries = proof.proof_unchecked();
        let branches: Vec<_> = unchecked_entries
            .into_iter()
            .map(|(path, hash)| create_java_map_proof_entry_branch(&env, &path, &hash))
            .collect::<JniResult<_>>()?;
        let local_refs = 100 + 2 * branches.len();
        env.ensure_local_capacity(local_refs as i32)?;

        // TODO: avoid checking proofs (ECR-1802)
        let checked_proof = proof.check().unwrap();
        let leaves: Vec<_> = checked_proof
            .all_entries()
            .into_iter()
            .map(|(key, optional_value)| {
                if optional_value.is_none() {
                    unimplemented!("Proofs for missing keys are not yet supported");
                }
                let path = ProofPath::new(key);
                let value: Vec<u8> = optional_value.cloned().unwrap().into_bytes();
                create_java_map_proof_entry_leaf(&env, &path, &value)
            })
            .collect::<JniResult<_>>()?;
        let array = create_java_array_map_proof_entry(&env, &leaves, &branches)?;
        let unchecked_flat_map_proof = create_java_unchecked_flat_map_proof(&env, array)?;
        Ok(unchecked_flat_map_proof.into_inner())
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

fn create_java_map_proof_entry_branch<'a>(
    env: &'a JNIEnv,
    proof_path: &ProofPath,
    hash: &Hash,
) -> JniResult<JObject<'a>> {
    let proof_path: JObject = env.byte_array_from_slice(proof_path.as_bytes())?.into();
    let hash: JObject = utils::convert_hash(env, hash)?.into();
    env.new_object(
        MAP_PROOF_ENTRY_BRANCH,
        "([B[B)V",
        &[proof_path.into(), hash.into()],
    )
}

fn create_java_map_proof_entry_leaf<'a>(
    env: &'a JNIEnv,
    proof_path: &ProofPath,
    value: &[u8],
) -> JniResult<JObject<'a>> {
    let proof_path: JObject = env.byte_array_from_slice(proof_path.as_bytes())?.into();
    let value: JObject = env.byte_array_from_slice(value)?.into();
    env.new_object(
        MAP_PROOF_ENTRY_LEAF,
        "([B[B)V",
        &[proof_path.into(), value.into()],
    )
}

fn create_java_array_map_proof_entry<'a, 'b: 'a>(
    env: &'a JNIEnv,
    leaves: &'b [JObject],
    branches: &'b [JObject],
) -> JniResult<JObject<'a>> {
    let length = leaves.len() + branches.len();
    let element_class = MAP_PROOF_ENTRY;
    let array = env.new_object_array(length as i32, element_class, JObject::null())?;
    for (index, entity) in branches.iter().chain(leaves.iter()).enumerate() {
        env.set_object_array_element(array, index as i32, *entity)?;
    }
    Ok(array.into())
}

fn create_java_unchecked_flat_map_proof<'a>(
    env: &'a JNIEnv,
    entries: JObject,
) -> JniResult<JObject<'a>> {
    let java_proof = env.call_static_method(
        UNCHECKED_FLAT_MAP_PROOF,
        "fromUnsorted",
        UNCHECKED_FLAT_MAP_PROOF_SIG,
        &[entries.into()],
    )?;
    java_proof.l()
}

/// Returns the pointer to the iterator over a map keys and values.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofMapIndexProxy_nativeCreateEntriesIter(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let iter = match *utils::cast_handle::<IndexType>(map_handle) {
            IndexType::SnapshotIndex(ref map) => map.iter(),
            IndexType::ForkIndex(ref map) => map.iter(),
        };
        let iter = Iter::new(&env, iter, JAVA_ENTRY_FQN)?;
        Ok(utils::to_handle(iter))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns the pointer to the iterator over map keys.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofMapIndexProxy_nativeCreateKeysIter(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        Ok(utils::to_handle(
            match *utils::cast_handle::<IndexType>(map_handle) {
                IndexType::SnapshotIndex(ref map) => map.keys(),
                IndexType::ForkIndex(ref map) => map.keys(),
            },
        ))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns the pointer to the iterator over map values.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofMapIndexProxy_nativeCreateValuesIter(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        Ok(utils::to_handle(
            match *utils::cast_handle::<IndexType>(map_handle) {
                IndexType::SnapshotIndex(ref map) => map.values(),
                IndexType::ForkIndex(ref map) => map.values(),
            },
        ))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns the pointer to the iterator over a map keys and values starting at the given key.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofMapIndexProxy_nativeCreateIterFrom(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
    key: jbyteArray,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let key = convert_to_key(&env, key)?;
        let iter = match *utils::cast_handle::<IndexType>(map_handle) {
            IndexType::SnapshotIndex(ref map) => map.iter_from(&key),
            IndexType::ForkIndex(ref map) => map.iter_from(&key),
        };
        let iter = Iter::new(&env, iter, JAVA_ENTRY_FQN)?;
        Ok(utils::to_handle(iter))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns the pointer to the iterator over map keys starting at the given key.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofMapIndexProxy_nativeKeysFrom(
    env: JNIEnv,
    _: JClass,
    map_handle: Handle,
    key: jbyteArray,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let key = convert_to_key(&env, key)?;
        Ok(utils::to_handle(
            match *utils::cast_handle::<IndexType>(map_handle) {
                IndexType::SnapshotIndex(ref map) => map.keys_from(&key),
                IndexType::ForkIndex(ref map) => map.keys_from(&key),
            },
        ))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns the pointer to the iterator over map values starting at the given key.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofMapIndexProxy_nativeValuesFrom(
    env: JNIEnv,
    _: JClass,
    map_handle: Handle,
    key: jbyteArray,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let key = convert_to_key(&env, key)?;
        Ok(utils::to_handle(
            match *utils::cast_handle::<IndexType>(map_handle) {
                IndexType::SnapshotIndex(ref map) => map.values_from(&key),
                IndexType::ForkIndex(ref map) => map.values_from(&key),
            },
        ))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Sets `value` identified by the `key` into the index.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofMapIndexProxy_nativePut(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
    key: jbyteArray,
    value: jbyteArray,
) {
    let res = panic::catch_unwind(|| match *utils::cast_handle::<IndexType>(map_handle) {
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
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofMapIndexProxy_nativeRemove(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
    key: jbyteArray,
) {
    let res = panic::catch_unwind(|| match *utils::cast_handle::<IndexType>(map_handle) {
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
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofMapIndexProxy_nativeClear(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
) {
    let res = panic::catch_unwind(|| match *utils::cast_handle::<IndexType>(map_handle) {
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
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofMapIndexProxy_nativeEntriesIterNext(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) -> jobject {
    let res = panic::catch_unwind(|| {
        let iterWrapper = utils::cast_handle::<Iter>(iter_handle);
        match iterWrapper.iter.next() {
            Some(val) => {
                let key: JObject = env.byte_array_from_slice(&val.0)?.into();
                let value: JObject = env.byte_array_from_slice(&val.1)?.into();
                Ok(env.new_object_by_id(
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

/// Destroys the underlying `ProofMapIndex` iterator object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofMapIndexProxy_nativeEntriesIterFree(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) {
    utils::drop_handle::<Iter>(&env, iter_handle);
}

/// Returns the next value from the keys-iterator. Returns null pointer when iteration is finished.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofMapIndexProxy_nativeKeysIterNext(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let iter = utils::cast_handle::<ProofMapIndexKeys<Key>>(iter_handle);
        match iter.next() {
            Some(val) => env.byte_array_from_slice(&val),
            None => Ok(ptr::null_mut()),
        }
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Destroys the underlying `ProofMapIndex` keys-iterator object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofMapIndexProxy_nativeKeysIterFree(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) {
    utils::drop_handle::<ProofMapIndexKeys<Key>>(&env, iter_handle);
}

/// Return next value from the values-iterator. Returns null pointer when iteration is finished.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofMapIndexProxy_nativeValuesIterNext(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let iter = utils::cast_handle::<ProofMapIndexValues<Value>>(iter_handle);
        match iter.next() {
            Some(val) => env.byte_array_from_slice(&val),
            None => Ok(ptr::null_mut()),
        }
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Destroys the underlying `ProofMapIndex` values-iterator object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofMapIndexProxy_nativeValuesIterFree(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) {
    utils::drop_handle::<ProofMapIndexValues<Value>>(&env, iter_handle);
}

fn convert_to_key(env: &JNIEnv, array: jbyteArray) -> JniResult<Key> {
    let bytes = env.convert_byte_array(array)?;
    assert_eq!(PROOF_MAP_KEY_SIZE, bytes.len());

    let mut key = Key::default();
    key.copy_from_slice(&bytes);
    Ok(key)
}
