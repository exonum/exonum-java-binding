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
use jni::sys::{jboolean, jbyteArray, jobject, jsize};
use jni::JNIEnv;

use std::panic;
use std::ptr;

use exonum::crypto::Hash;
use exonum::storage::proof_map_index::{
    CheckedMapProof, MapProof, ProofMapIndexIter, ProofMapIndexKeys, ProofMapIndexValues,
    ProofPath, PROOF_MAP_KEY_SIZE,
};
use exonum::storage::{Fork, ProofMapIndex, Snapshot};

use storage::db::{Value, View, ViewRef};
use utils::{self, Handle, PairIter};
use JniResult;

type Key = [u8; PROOF_MAP_KEY_SIZE];
type Index<T> = ProofMapIndex<T, Key, Value>;

const JAVA_ENTRY_FQN: &str = "com/exonum/binding/storage/indices/MapEntryInternal";
const MAP_PROOF_ENTRY: &str = "com/exonum/binding/common/proofs/map/flat/MapProofEntry";
const MAP_ENTRY: &str = "com/exonum/binding/common/proofs/map/flat/MapEntry";
const UNCHECKED_FLAT_MAP_PROOF: &str =
    "com/exonum/binding/common/proofs/map/flat/UncheckedFlatMapProof";
const UNCHECKED_FLAT_MAP_PROOF_SIG: &str =
    "([Lcom/exonum/binding/common/proofs/map/flat/MapProofEntry;[Lcom/exonum/binding/common/proofs/map/flat/MapEntry;[[B)Lcom/exonum/binding/common/proofs/map/flat/UncheckedFlatMapProof;";
const BYTE_ARRAY: &str = "[B";

enum IndexType {
    SnapshotIndex(Index<&'static Snapshot>),
    ForkIndex(Index<&'static mut Fork>),
}

type Iter<'a> = PairIter<ProofMapIndexIter<'a, Key, Value>>;

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

        Ok(convert_to_java_proof(&env, proof)?.into_inner())
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Returns Java-proof object.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofMapIndexProxy_nativeGetMultiProof(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
    keys: jbyteArray,
) -> jobject {
    let res = panic::catch_unwind(|| {
        let keys = convert_to_keys(&env, keys)?;
        let proof = match *utils::cast_handle::<IndexType>(map_handle) {
            IndexType::SnapshotIndex(ref map) => map.get_multiproof(keys),
            IndexType::ForkIndex(ref map) => map.get_multiproof(keys),
        };

        Ok(convert_to_java_proof(&env, proof)?.into_inner())
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

fn convert_to_java_proof<'a>(
    env: &'a JNIEnv,
    proof: MapProof<Key, Value>
) -> JniResult<JObject<'a>> {
    let proof_nodes: JObject = create_java_proof_nodes(&env, &proof)?;
    let missing_keys: JObject = create_java_missing_keys(&env, &proof)?;

    // TODO: avoid checking proofs (ECR-1802) and reorder the surrounding operations
    let checked_proof = proof.check().unwrap();
    let map_entries: JObject = create_java_map_entries(&env, &checked_proof)?;

    create_java_unchecked_flat_map_proof(&env, proof_nodes, map_entries, missing_keys)
}

fn create_java_proof_nodes<'a>(
    env: &'a JNIEnv,
    map_proof: &MapProof<Key, Value>,
) -> JniResult<JObject<'a>> {
    let proof_entries = map_proof.proof_unchecked();
    let java_entries = env.new_object_array(
        proof_entries.len() as jsize,
        MAP_PROOF_ENTRY,
        JObject::null(),
    )?;
    for (i, (proof_path, value_hash)) in proof_entries.iter().enumerate() {
        // todo: [ECR-2360] Estimate precisely the upper bound on the number of references ^ and
        //   consider using a single frame
        env.with_local_frame(8, || {
            let je = create_java_proof_node(env, &proof_path, &value_hash)?;
            env.set_object_array_element(java_entries, i as jsize, je)?;
            Ok(JObject::null())
        })?;
    }
    Ok(java_entries.into())
}

/// Creates a proof node — a node in a proof contour that corresponds to a tree node
/// that does not contain any of the requested keys.
fn create_java_proof_node<'a>(
    env: &'a JNIEnv,
    proof_path: &ProofPath,
    hash: &Hash,
) -> JniResult<JObject<'a>> {
    let proof_path: JObject = env.byte_array_from_slice(proof_path.as_bytes())?.into();
    let hash: JObject = utils::convert_hash(env, hash)?.into();
    env.new_object(
        MAP_PROOF_ENTRY,
        "([B[B)V",
        &[proof_path.into(), hash.into()],
    )
}

fn create_java_map_entries<'a>(
    env: &'a JNIEnv,
    checked_proof: &CheckedMapProof<Key, Value>,
) -> JniResult<JObject<'a>> {
    let entries: Vec<(&Key, &Value)> = checked_proof.entries();
    let java_entries = env.new_object_array(entries.len() as jsize, MAP_ENTRY, JObject::null())?;
    for (i, (key, value)) in entries.iter().enumerate() {
        // todo: [ECR-2360] Estimate precisely the upper bound on the number of references ^ and
        //   consider using a single frame
        env.with_local_frame(8, || {
            let je = create_java_map_entry(env, key, value)?;
            env.set_object_array_element(java_entries, i as jsize, je)?;
            Ok(JObject::null())
        })?;
    }
    Ok(java_entries.into())
}

#[cfg_attr(feature = "cargo-clippy", allow(ptr_arg))]
fn create_java_map_entry<'a>(env: &'a JNIEnv, key: &Key, value: &Value) -> JniResult<JObject<'a>> {
    let key: JObject = env.byte_array_from_slice(key)?.into();
    let value: JObject = env.byte_array_from_slice(value.as_slice())?.into();
    env.new_object(MAP_ENTRY, "([B[B)V", &[key.into(), value.into()])
}

fn create_java_missing_keys<'a>(
    env: &'a JNIEnv,
    map_proof: &MapProof<Key, Value>,
) -> JniResult<JObject<'a>> {
    let missing_keys = map_proof.missing_keys_unchecked();
    let java_missing_keys =
        env.new_object_array(missing_keys.len() as jsize, BYTE_ARRAY, JObject::null())?;
    for (i, key) in missing_keys.iter().enumerate() {
        let java_key = env.byte_array_from_slice(key.as_ref())?.into();
        env.set_object_array_element(java_missing_keys, i as jsize, java_key)?;
        env.delete_local_ref(java_key)?;
    }
    Ok(java_missing_keys.into())
}

fn create_java_unchecked_flat_map_proof<'a>(
    env: &'a JNIEnv,
    proof_nodes: JObject,
    map_entries: JObject,
    missing_keys: JObject,
) -> JniResult<JObject<'a>> {
    let java_proof = env.call_static_method(
        UNCHECKED_FLAT_MAP_PROOF,
        "fromNative",
        UNCHECKED_FLAT_MAP_PROOF_SIG,
        &[proof_nodes.into(), map_entries.into(), missing_keys.into()],
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
                Ok(env
                    .new_object_by_id(
                        &iterWrapper.element_class,
                        iterWrapper.constructor_id,
                        &[key.into(), value.into()],
                    )?.into_inner())
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

fn convert_to_keys(env: &JNIEnv, array: jbyteArray) -> JniResult<Vec<Key>> {
    let bytes = env.convert_byte_array(array)?;
    assert_eq!(bytes.len() % PROOF_MAP_KEY_SIZE, 0);

    let keys = bytes
        .chunks(PROOF_MAP_KEY_SIZE)
        .map(|bytes| {
            let mut key = Key::default();
            key.copy_from_slice(bytes);
            key
        }).collect();
    Ok(keys)
}
