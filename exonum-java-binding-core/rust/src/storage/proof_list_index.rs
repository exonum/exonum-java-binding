use jni::JNIEnv;
use jni::errors::Result;
use jni::objects::{JClass, JObject};
use jni::sys::{jlong, jint, jbyteArray, jboolean, jobject};

use std::panic;
use std::ptr;

use exonum::storage::{Snapshot, Fork, ProofListIndex};
use exonum::storage::proof_list_index::{ProofListIndexIter, ListProof};
use exonum::crypto::Hash;
use utils::{self, Handle, AutoLocalRef};
use super::db::{View, Value};

type Index<T> = ProofListIndex<T, Value>;

enum IndexType {
    SnapshotIndex(Index<&'static Snapshot>),
    ForkIndex(Index<&'static mut Fork>),
}

/// Returns pointer to the created `ProofListIndex` object.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofListIndexProxy_nativeCreate(
    env: JNIEnv,
    _: JClass,
    prefix: jbyteArray,
    view_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let prefix = env.convert_byte_array(prefix)?;
        Ok(utils::to_handle(match *utils::cast_handle(view_handle) {
            View::Snapshot(ref snapshot) => IndexType::SnapshotIndex(
                Index::new(prefix, &**snapshot),
            ),
            View::Fork(ref mut fork) => IndexType::ForkIndex(Index::new(prefix, fork)),
        }))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Destroys the underlying `ProofListIndex` object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofListIndexProxy_nativeFree(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
) {
    utils::drop_handle::<IndexType>(&env, list_handle);
}

/// Returns the value by index. Null pointer is returned if value is not found.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofListIndexProxy_nativeGet(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
    index: jlong,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let val = match *utils::cast_handle::<IndexType>(list_handle) {
            IndexType::SnapshotIndex(ref list) => list.get(index as u64),
            IndexType::ForkIndex(ref list) => list.get(index as u64),
        };
        match val {
            Some(val) => env.byte_array_from_slice(&val),
            None => Ok(ptr::null_mut()),
        }
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Returns the last value or null pointer if the list is empty.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofListIndexProxy_nativeGetLast(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let val = match *utils::cast_handle::<IndexType>(list_handle) {
            IndexType::SnapshotIndex(ref list) => list.last(),
            IndexType::ForkIndex(ref list) => list.last(),
        };
        match val {
            Some(val) => env.byte_array_from_slice(&val),
            None => Ok(ptr::null_mut()),
        }
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Returns `true` if the list is empty.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofListIndexProxy_nativeIsEmpty(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        Ok(match *utils::cast_handle::<IndexType>(list_handle) {
            IndexType::SnapshotIndex(ref list) => list.is_empty(),
            IndexType::ForkIndex(ref list) => list.is_empty(),
        } as jboolean)
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns length of the list.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofListIndexProxy_nativeSize(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
) -> jlong {
    let res = panic::catch_unwind(|| {
        Ok(match *utils::cast_handle::<IndexType>(list_handle) {
            IndexType::SnapshotIndex(ref list) => list.len(),
            IndexType::ForkIndex(ref list) => list.len(),
        } as jlong)
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns the height of the proof list.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofListIndexProxy_nativeHeight(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
) -> jint {
    let res = panic::catch_unwind(|| {
        Ok(match *utils::cast_handle::<IndexType>(list_handle) {
            IndexType::SnapshotIndex(ref list) => list.height(),
            IndexType::ForkIndex(ref list) => list.height(),
        } as jint)
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns the root hash of the proof list or default hash value if it is empty.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofListIndexProxy_nativeGetRootHash(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
) -> jbyteArray{
    let res = panic::catch_unwind(|| {
        let hash = match *utils::cast_handle::<IndexType>(list_handle) {
            IndexType::SnapshotIndex(ref list) => list.root_hash(),
            IndexType::ForkIndex(ref list) => list.root_hash(),
        };
        utils::convert_hash(&env, &hash)
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Returns Java representation of the proof that an element exists at the specified index.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofListIndexProxy_nativeGetProof(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
    index: jlong,
) -> jobject{
    let res = panic::catch_unwind(|| {
        let proof = match *utils::cast_handle::<IndexType>(list_handle) {
            IndexType::SnapshotIndex(ref list) => list.get_proof(index as u64),
            IndexType::ForkIndex(ref list) => list.get_proof(index as u64),
        };
        make_java_proof(&env, &proof).map(|x| x.into_inner())
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Returns Java representation of the proof that some elements exists in the specified range.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofListIndexProxy_nativeGetRangeProof(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
    from: jlong,
    to: jlong,
) -> jobject{
    let res = panic::catch_unwind(|| {
        let proof = match *utils::cast_handle::<IndexType>(list_handle) {
            IndexType::SnapshotIndex(ref list) => list.get_range_proof(from as u64, to as u64),
            IndexType::ForkIndex(ref list) => list.get_range_proof(from as u64, to as u64),
        };
        make_java_proof(&env, &proof).map(|x| x.into_inner())
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Returns pointer to the iterator over list.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofListIndexProxy_nativeCreateIter(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
) -> Handle{
    let res = panic::catch_unwind(|| {
        Ok(utils::to_handle(
            match *utils::cast_handle::<IndexType>(list_handle) {
                IndexType::SnapshotIndex(ref list) => list.iter(),
                IndexType::ForkIndex(ref list) => list.iter(),
            },
        ))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns pointer to the iterator over list starting at given index.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofListIndexProxy_nativeIterFrom(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
    index_from: jlong,
) -> Handle{
    let res = panic::catch_unwind(|| {
        Ok(utils::to_handle(
            match *utils::cast_handle::<IndexType>(list_handle) {
                IndexType::SnapshotIndex(ref list) => list.iter_from(index_from as u64),
                IndexType::ForkIndex(ref list) => list.iter_from(index_from as u64),
            },
        ))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Adds value to the list.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofListIndexProxy_nativeAdd(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
    value: jbyteArray,
) {
    let res = panic::catch_unwind(|| match *utils::cast_handle::<IndexType>(list_handle) {
        IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        IndexType::ForkIndex(ref mut list) => {
            let value = env.convert_byte_array(value)?;
            list.push(value);
            Ok(())
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Sets value into specified index. Panics if `i` is out of bounds.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofListIndexProxy_nativeSet(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
    index: jlong,
    value: jbyteArray,
) {
    let res = panic::catch_unwind(|| match *utils::cast_handle::<IndexType>(list_handle) {
        IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        IndexType::ForkIndex(ref mut list) => {
            let value = env.convert_byte_array(value)?;
            list.set(index as u64, value);
            Ok(())
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Clears the list, removing all values.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofListIndexProxy_nativeClear(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
) {
    let res = panic::catch_unwind(|| match *utils::cast_handle::<IndexType>(list_handle) {
        IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        IndexType::ForkIndex(ref mut list) => {
            list.clear();
            Ok(())
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns next value from the iterator. Returns null pointer when iteration is finished.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofListIndexProxy_nativeIterNext(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) -> jbyteArray{
    let res = panic::catch_unwind(|| {
        let mut iter = utils::cast_handle::<ProofListIndexIter<Value>>(iter_handle);
        match iter.next() {
            Some(val) => env.byte_array_from_slice(&val),
            None => Ok(ptr::null_mut()),
        }
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Destroys the underlying `ProofListIndex` iterator object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ProofListIndexProxy_nativeIterFree(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
){
    utils::drop_handle::<ProofListIndexIter<Value>>(&env, iter_handle);
}

fn make_java_proof<'a>(env: &JNIEnv<'a>, proof: &ListProof<Value>) -> Result<JObject<'a>> {
    match *proof {
        ListProof::Full(ref left, ref right) => {
            let left = make_java_proof(env, left.as_ref())?;
            let right = make_java_proof(env, right.as_ref())?;
            make_java_proof_branch(env, left, right)
        }
        ListProof::Left(ref left, ref hash) => {
            let left = make_java_proof(env, left.as_ref())?;
            let right = hash.map_or(Ok(ptr::null_mut().into()), |hash| {
                make_java_hash_node(env, &hash)
            })?;
            make_java_proof_branch(env, left, right)
        }
        ListProof::Right(ref hash, ref right) => {
            let left = make_java_hash_node(env, hash)?;
            let right = make_java_proof(env, right.as_ref())?;
            make_java_proof_branch(env, left, right)
        }
        ListProof::Leaf(ref value) => make_java_proof_element(env, value),
    }
}

// TODO: Remove attribute (https://github.com/rust-lang-nursery/rust-clippy/issues/1981).
#[cfg_attr(feature = "cargo-clippy", allow(ptr_arg))]
fn make_java_proof_element<'a>(env: &JNIEnv<'a>, value: &Value) -> Result<JObject<'a>> {
    let value = AutoLocalRef::new(env, env.byte_array_from_slice(value)?);
    env.new_object(
        "com/exonum/binding/storage/proofs/list/ProofListElement",
        "([B)V",
        &[value.as_obj().into()],
    )
}

fn make_java_proof_branch<'a>(
    env: &JNIEnv<'a>,
    left: JObject,
    right: JObject,
) -> Result<JObject<'a>> {
    let left = AutoLocalRef::new(env, left.into_inner());
    let right = AutoLocalRef::new(env, right.into_inner());
    env.new_object(
        "com/exonum/binding/storage/proofs/list/ListProofBranch",
        "(Lcom/exonum/binding/storage/proofs/list/ListProof;\
          Lcom/exonum/binding/storage/proofs/list/ListProof;)V",
        &[left.as_obj().into(), right.as_obj().into()],
    )
}

fn make_java_hash_node<'a>(env: &JNIEnv<'a>, hash: &Hash) -> Result<JObject<'a>> {
    let hash = AutoLocalRef::new(env, utils::convert_hash(env, hash)?);
    env.new_object(
        "com/exonum/binding/storage/proofs/list/HashNode",
        "([B)V",
        &[hash.as_obj().into()],
    )
}
