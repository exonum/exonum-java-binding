use jni::JNIEnv;
use jni::objects::{JClass, JObject, JString, AutoLocal};
use jni::sys::{jboolean, jbyteArray, jobject};
use jni::errors::Result;

use std::panic;
use std::ptr;

use exonum::crypto::Hash;
use exonum::storage::{Snapshot, Fork, ProofMapIndex, MapProof, StorageKey};
use exonum::storage::proof_map_index::{ProofMapIndexIter, ProofMapIndexKeys, ProofMapIndexValues,
                                       ProofMapDBKey, BranchProofNode, ProofNode,
                                       PROOF_MAP_KEY_SIZE};
use utils::{self, Handle, PairIter};
use super::db::{View, Value};

type Key = [u8; PROOF_MAP_KEY_SIZE];
type Index<T> = ProofMapIndex<T, Key, Value>;

enum IndexType {
    SnapshotIndex(Index<&'static Snapshot>),
    ForkIndex(Index<&'static mut Fork>),
}

type Iter<'a> = PairIter<ProofMapIndexIter<'a, Key, Value>>;

const JAVA_ENTRY_FQN: &str = "com/exonum/binding/storage/indices/MapEntry";

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
        Ok(utils::to_handle(match *utils::cast_handle(view_handle) {
            View::Snapshot(ref snapshot) => IndexType::SnapshotIndex(Index::new(name, &**snapshot)),
            View::Fork(ref mut fork) => IndexType::ForkIndex(Index::new(name, fork)),
        }))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Destroys the underlying `ProofMapIndex` object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofMapIndexProxy_nativeFree(
    env: JNIEnv,
    _: JObject,
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
            IndexType::SnapshotIndex(ref map) => map.root_hash(),
            IndexType::ForkIndex(ref map) => map.root_hash(),
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
) -> jboolean{
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
            IndexType::SnapshotIndex(ref map) => map.get_proof(&key),
            IndexType::ForkIndex(ref map) => map.get_proof(&key),
        };

        match proof {
            MapProof::LeafRootInclusive(key, val) => {
                make_java_equal_value_at_root(&env, &key, &val)
            }
            MapProof::LeafRootExclusive(key, hash) => {
                make_java_non_equal_value_at_root(&env, &key, &hash)
            }
            MapProof::Empty => make_java_empty_proof(&env),
            MapProof::Branch(branch) => make_java_brach_proof(&env, &branch),
        }
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Returns the pointer to the iterator over a map keys and values.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofMapIndexProxy_nativeCreateEntriesIter(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
) -> Handle{
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
) -> Handle{
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
) -> Handle{
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
) -> Handle{
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
) -> Handle{
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
) -> jobject{
    let res = panic::catch_unwind(|| {
        let iterWrapper = utils::cast_handle::<Iter>(iter_handle);
        match iterWrapper.iter.next() {
            Some(val) => {
                let key: JObject = env.byte_array_from_slice(&val.0)?.into();
                let value: JObject = env.byte_array_from_slice(&val.1)?.into();
                Ok(
                    env.new_object_by_id(
                        &iterWrapper.element_class,
                        iterWrapper.constructor_id,
                        &[key.into(), value.into()],
                    )?
                        .into_inner(),
                )
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
){
    utils::drop_handle::<Iter>(&env, iter_handle);
}

/// Returns the next value from the keys-iterator. Returns null pointer when iteration is finished.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofMapIndexProxy_nativeKeysIterNext(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) -> jbyteArray{
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
){
    utils::drop_handle::<ProofMapIndexKeys<Key>>(&env, iter_handle);
}

/// Return next value from the values-iterator. Returns null pointer when iteration is finished.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofMapIndexProxy_nativeValuesIterNext(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) -> jbyteArray{
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
){
    utils::drop_handle::<ProofMapIndexValues<Value>>(&env, iter_handle);
}

fn convert_to_key(env: &JNIEnv, array: jbyteArray) -> Result<Key> {
    // TODO: Optimize copying and allocations.
    let bytes = env.convert_byte_array(array)?;
    assert_eq!(PROOF_MAP_KEY_SIZE, bytes.len());

    let mut key = Key::default();
    key.copy_from_slice(&bytes);
    Ok(key)
}

fn make_java_empty_proof(env: &JNIEnv) -> Result<jobject> {
    Ok(
        env.new_object(
            "com/exonum/binding/storage/proofs/map/EmptyMapProof",
            "()V",
            &[],
        )?
            .into_inner(),
    )
}

// TODO: Remove attribute (https://github.com/rust-lang-nursery/rust-clippy/issues/1981).
#[cfg_attr(feature = "cargo-clippy", allow(ptr_arg))]
fn make_java_equal_value_at_root(
    env: &JNIEnv,
    key: &ProofMapDBKey,
    value: &Value,
) -> Result<jobject> {
    let key = make_java_db_key(env, key)?;
    let value = env.auto_local(env.byte_array_from_slice(value)?.into());
    Ok(
        env.new_object(
            "com/exonum/binding/storage/proofs/map/EqualValueAtRoot",
            "(Lcom/exonum/binding/storage/proofs/map/DbKey;[B)V",
            &[key.as_obj().into(), value.as_obj().into()],
        )?
            .into_inner(),
    )
}

fn make_java_db_key<'a>(env: &'a JNIEnv, key: &ProofMapDBKey) -> Result<AutoLocal<'a>> {
    // TODO: Export `DB_KEY_SIZE`?
    const PROOF_KEY_SIZE: usize = PROOF_MAP_KEY_SIZE + 2;
    debug_assert_eq!(PROOF_KEY_SIZE, key.size());

    let mut buffer = [0; PROOF_KEY_SIZE];
    key.write(&mut buffer);

    let key = env.auto_local(env.byte_array_from_slice(&buffer)?.into());
    let java_db_key = env.new_object(
        "com/exonum/binding/storage/proofs/map/DbKey",
        "([B)V",
        &[key.as_obj().into()],
    )?;
    Ok(env.auto_local(java_db_key))
}

fn make_java_hash<'a>(env: &'a JNIEnv, hash: &Hash) -> Result<AutoLocal<'a>> {
    let hash = env.auto_local(utils::convert_hash(env, hash)?.into());
    let java_hash = env.new_object(
        "com/exonum/binding/storage/proofs/map/HashCode",
        "([B)V",
        &[hash.as_obj().into()],
    )?;
    Ok(env.auto_local(java_hash))
}

fn make_java_non_equal_value_at_root(
    env: &JNIEnv,
    key: &ProofMapDBKey,
    hash: &Hash,
) -> Result<jobject> {
    let key = make_java_db_key(env, key)?;
    let hash = make_java_hash(env, hash)?;
    Ok(
        env.new_object(
            "com/exonum/binding/storage/proofs/map/NonEqualValueAtRoot",
            "(Lcom/exonum/binding/storage/proofs/map/DbKey;\
              Lcom/exonum/binding/storage/proofs/map/HashCode;)V",
            &[key.as_obj().into(), hash.as_obj().into()],
        )?
            .into_inner(),
    )
}

fn make_java_brach_proof(env: &JNIEnv, branch: &BranchProofNode<Value>) -> Result<jobject> {
    match *branch {
        BranchProofNode::BranchKeyNotFound {
            ref left_hash,
            ref right_hash,
            ref left_key,
            ref right_key,
        } => make_java_mapping_not_found_branch(env, left_hash, right_hash, left_key, right_key),
        BranchProofNode::LeftBranch {
            ref left_node,
            ref right_hash,
            ref left_key,
            ref right_key,
        } => make_java_left_proof_branch(env, left_node, right_hash, left_key, right_key),
        BranchProofNode::RightBranch {
            ref left_hash,
            ref right_node,
            ref left_key,
            ref right_key,
        } => make_java_right_proof_branch(env, left_hash, right_node, left_key, right_key),
    }
}

fn make_java_mapping_not_found_branch(
    env: &JNIEnv,
    left_hash: &Hash,
    right_hash: &Hash,
    left_key: &ProofMapDBKey,
    right_key: &ProofMapDBKey,
) -> Result<jobject> {
    let left_hash = make_java_hash(env, left_hash)?;
    let right_hash = make_java_hash(env, right_hash)?;
    let left_key = make_java_db_key(env, left_key)?;
    let right_key = make_java_db_key(env, right_key)?;
    Ok(
        env.new_object(
            "com/exonum/binding/storage/proofs/map/MappingNotFoundProofBranch",
            "(Lcom/exonum/binding/storage/proofs/map/HashCode;\
              Lcom/exonum/binding/storage/proofs/map/HashCode;\
              Lcom/exonum/binding/storage/proofs/map/DbKey;\
              Lcom/exonum/binding/storage/proofs/map/DbKey;)V",
            &[
                left_hash.as_obj().into(),
                right_hash.as_obj().into(),
                left_key.as_obj().into(),
                right_key.as_obj().into(),
            ],
        )?
            .into_inner(),
    )
}

fn make_java_left_proof_branch(
    env: &JNIEnv,
    left_node: &ProofNode<Value>,
    right_hash: &Hash,
    left_key: &ProofMapDBKey,
    right_key: &ProofMapDBKey,
) -> Result<jobject> {
    let left_node = make_java_proof_node(env, left_node)?;
    let right_hash = make_java_hash(env, right_hash)?;
    let left_key = make_java_db_key(env, left_key)?;
    let right_key = make_java_db_key(env, right_key)?;
    Ok(
        env.new_object(
            "com/exonum/binding/storage/proofs/map/LeftMapProofBranch",
            "(Lcom/exonum/binding/storage/proofs/map/MapProofNode;\
              Lcom/exonum/binding/storage/proofs/map/HashCode;\
              Lcom/exonum/binding/storage/proofs/map/DbKey;\
              Lcom/exonum/binding/storage/proofs/map/DbKey;)V",
            &[
                left_node.as_obj().into(),
                right_hash.as_obj().into(),
                left_key.as_obj().into(),
                right_key.as_obj().into(),
            ],
        )?
            .into_inner(),
    )
}

fn make_java_right_proof_branch(
    env: &JNIEnv,
    left_hash: &Hash,
    right_node: &ProofNode<Value>,
    left_key: &ProofMapDBKey,
    right_key: &ProofMapDBKey,
) -> Result<jobject> {
    let left_hash = make_java_hash(env, left_hash)?;
    let right_node = make_java_proof_node(env, right_node)?;
    let left_key = make_java_db_key(env, left_key)?;
    let right_key = make_java_db_key(env, right_key)?;
    Ok(
        env.new_object(
            "com/exonum/binding/storage/proofs/map/RightMapProofBranch",
            "(Lcom/exonum/binding/storage/proofs/map/HashCode;\
              Lcom/exonum/binding/storage/proofs/map/MapProofNode;\
              Lcom/exonum/binding/storage/proofs/map/DbKey;\
              Lcom/exonum/binding/storage/proofs/map/DbKey;)V",
            &[
                left_hash.as_obj().into(),
                right_node.as_obj().into(),
                left_key.as_obj().into(),
                right_key.as_obj().into(),
            ],
        )?
            .into_inner(),
    )
}

fn make_java_proof_node<'a>(
    env: &'a JNIEnv,
    proof_node: &ProofNode<Value>,
) -> Result<AutoLocal<'a>> {
    match *proof_node {
        ProofNode::Branch(ref branch_proof_node) => {
            let branch = make_java_brach_proof(env, branch_proof_node)?;
            Ok(env.auto_local(branch.into()))
        }
        ProofNode::Leaf(ref value) => make_java_leaf_proof_node(env, value),
    }
}

// TODO: Remove attribute (https://github.com/rust-lang-nursery/rust-clippy/issues/1981).
#[cfg_attr(feature = "cargo-clippy", allow(ptr_arg))]
fn make_java_leaf_proof_node<'a>(env: &'a JNIEnv, value: &Value) -> Result<AutoLocal<'a>> {
    let value = env.auto_local(env.byte_array_from_slice(value)?.into());
    let leaf_proof_node = env.new_object(
        "com/exonum/binding/storage/proofs/map/LeafMapProofNode",
        "([B)V",
        &[value.as_obj().into()],
    )?;
    Ok(env.auto_local(leaf_proof_node))
}
