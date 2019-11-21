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

use exonum_merkledb::{
    access::FromAccess, proof_list_index::ProofListIndexIter, Fork, IndexAddress, ObjectHash,
    ProofListIndex, Snapshot,
};
use jni::{
    objects::{JClass, JObject, JString},
    sys::{jboolean, jbyteArray, jint, jlong, jobject},
    JNIEnv,
};

use std::{panic, ptr};

use handle::{self, Handle};
use storage::db::{Value, View, ViewRef};
use utils;

type Index<T> = ProofListIndex<T, Value>;

enum IndexType {
    SnapshotIndex(Index<&'static dyn Snapshot>),
    ForkIndex(Index<&'static Fork>),
}

/// Returns pointer to the created `ProofListIndex` object.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofListIndexProxy_nativeCreate(
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

/// Returns a pointer to the created `ProofListIndex` instance in an index family (= group).
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofListIndexProxy_nativeCreateInGroup(
    env: JNIEnv,
    _: JClass,
    group_name: JString,
    list_id: jbyteArray,
    view_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let group_name = utils::convert_to_string(&env, group_name)?;
        let list_id = env.convert_byte_array(list_id)?;
        let address = IndexAddress::with_root(group_name).append_bytes(&list_id);
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

/// Destroys the underlying `ProofListIndex` object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofListIndexProxy_nativeFree(
    env: JNIEnv,
    _: JClass,
    list_handle: Handle,
) {
    handle::drop_handle::<IndexType>(&env, list_handle);
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
        let val = match *handle::cast_handle::<IndexType>(list_handle) {
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
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofListIndexProxy_nativeGetLast(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let val = match *handle::cast_handle::<IndexType>(list_handle) {
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
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofListIndexProxy_nativeIsEmpty(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        Ok(match *handle::cast_handle::<IndexType>(list_handle) {
            IndexType::SnapshotIndex(ref list) => list.is_empty(),
            IndexType::ForkIndex(ref list) => list.is_empty(),
        } as jboolean)
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
    match *handle::cast_handle::<IndexType>(list_handle) {
        IndexType::SnapshotIndex(ref list) => list.len(),
        IndexType::ForkIndex(ref list) => list.len(),
    }
}

/// Returns the height of the proof list.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofListIndexProxy_nativeHeight(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
) -> jint {
    let res = panic::catch_unwind(|| {
        Ok(i32::from(
            match *handle::cast_handle::<IndexType>(list_handle) {
                IndexType::SnapshotIndex(ref list) => list.height(),
                IndexType::ForkIndex(ref list) => list.height(),
            },
        ))
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
        let hash = match *handle::cast_handle::<IndexType>(list_handle) {
            IndexType::SnapshotIndex(ref list) => list.object_hash(),
            IndexType::ForkIndex(ref list) => list.object_hash(),
        };
        utils::convert_hash(&env, &hash)
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Returns Java representation of the proof that an element exists at the specified index.
#[no_mangle]
// fixme: ECR-3614
#[allow(unused_variables)]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofListIndexProxy_nativeGetProof(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
    index: jlong,
) -> jobject {
    ptr::null_mut()
}

/// Returns Java representation of the proof that some elements exists in the specified range.
#[no_mangle]
// fixme: ECR-3614
#[allow(unused_variables)]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofListIndexProxy_nativeGetRangeProof(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
    from: jlong,
    to: jlong,
) -> jobject {
    // fixme: ECR-3614
    ptr::null_mut()
}

/// Returns pointer to the iterator over list.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofListIndexProxy_nativeCreateIter(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        Ok(handle::to_handle(
            match *handle::cast_handle::<IndexType>(list_handle) {
                IndexType::SnapshotIndex(ref list) => list.iter(),
                IndexType::ForkIndex(ref list) => list.iter(),
            },
        ))
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
        Ok(handle::to_handle(
            match *handle::cast_handle::<IndexType>(list_handle) {
                IndexType::SnapshotIndex(ref list) => list.iter_from(index_from as u64),
                IndexType::ForkIndex(ref list) => list.iter_from(index_from as u64),
            },
        ))
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
    let res = panic::catch_unwind(|| match *handle::cast_handle::<IndexType>(list_handle) {
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
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofListIndexProxy_nativeSet(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
    index: jlong,
    value: jbyteArray,
) {
    let res = panic::catch_unwind(|| match *handle::cast_handle::<IndexType>(list_handle) {
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
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofListIndexProxy_nativeClear(
    env: JNIEnv,
    _: JObject,
    list_handle: Handle,
) {
    let res = panic::catch_unwind(|| match *handle::cast_handle::<IndexType>(list_handle) {
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
pub extern "system" fn Java_com_exonum_binding_core_storage_indices_ProofListIndexProxy_nativeIterNext(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let iter = handle::cast_handle::<ProofListIndexIter<Value>>(iter_handle);
        match iter.next() {
            Some(val) => env.byte_array_from_slice(&val),
            None => Ok(ptr::null_mut()),
        }
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
    handle::drop_handle::<ProofListIndexIter<Value>>(&env, iter_handle);
}
