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

use exonum::storage::key_set_index::KeySetIndexIter;
use exonum::storage::{Fork, KeySetIndex, Snapshot};
use jni::objects::{JClass, JObject, JString};
use jni::sys::{jboolean, jbyteArray};
use jni::JNIEnv;

use std::panic;
use std::ptr;

use storage::db::{Key, View, ViewRef};
use utils::{self, Handle};

type Index<T> = KeySetIndex<T, Key>;

enum IndexType {
    SnapshotIndex(Index<&'static Snapshot>),
    ForkIndex(Index<&'static mut Fork>),
}

/// Returns pointer to created `KeySetIndex` object.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_KeySetIndexProxy_nativeCreate(
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

/// Returns a pointer to the created `KeySetIndex` object in an index family (= group).
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_KeySetIndexProxy_nativeCreateInGroup(
    env: JNIEnv,
    _: JClass,
    group_name: JString,
    set_id: jbyteArray,
    view_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let group_name = utils::convert_to_string(&env, group_name)?;
        let set_id = env.convert_byte_array(set_id)?;
        let view_ref = utils::cast_handle::<View>(view_handle).get();
        Ok(utils::to_handle(match *view_ref {
            ViewRef::Snapshot(snapshot) => {
                IndexType::SnapshotIndex(Index::new_in_family(group_name, &set_id, &*snapshot))
            }
            ViewRef::Fork(ref mut fork) => {
                IndexType::ForkIndex(Index::new_in_family(group_name, &set_id, fork))
            }
        }))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Destroys underlying `KeySetIndex` object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_KeySetIndexProxy_nativeFree(
    env: JNIEnv,
    _: JClass,
    set_handle: Handle,
) {
    utils::drop_handle::<IndexType>(&env, set_handle);
}

/// Returns `true` if the set contains the specified value.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_KeySetIndexProxy_nativeContains(
    env: JNIEnv,
    _: JObject,
    set_handle: Handle,
    value: jbyteArray,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let value = env.convert_byte_array(value)?;
        Ok(match *utils::cast_handle::<IndexType>(set_handle) {
            IndexType::SnapshotIndex(ref set) => set.contains(&value),
            IndexType::ForkIndex(ref set) => set.contains(&value),
        } as jboolean)
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns pointer to the iterator over set.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_KeySetIndexProxy_nativeCreateIterator(
    env: JNIEnv,
    _: JObject,
    set_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        Ok(utils::to_handle(
            match *utils::cast_handle::<IndexType>(set_handle) {
                IndexType::SnapshotIndex(ref set) => set.iter(),
                IndexType::ForkIndex(ref set) => set.iter(),
            },
        ))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns pointer to the iterator over set starting at the given key.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_KeySetIndexProxy_nativeCreateIteratorFrom(
    env: JNIEnv,
    _: JObject,
    set_handle: Handle,
    from: jbyteArray,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let from = env.convert_byte_array(from)?;
        Ok(utils::to_handle(
            match *utils::cast_handle::<IndexType>(set_handle) {
                IndexType::SnapshotIndex(ref set) => set.iter_from(&from),
                IndexType::ForkIndex(ref set) => set.iter_from(&from),
            },
        ))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Inserts value in the set.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_KeySetIndexProxy_nativeAdd(
    env: JNIEnv,
    _: JObject,
    set_handle: Handle,
    value: jbyteArray,
) {
    let res = panic::catch_unwind(|| match *utils::cast_handle::<IndexType>(set_handle) {
        IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        IndexType::ForkIndex(ref mut set) => {
            let value = env.convert_byte_array(value)?;
            set.insert(value);
            Ok(())
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Removes value from the set.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_KeySetIndexProxy_nativeRemove(
    env: JNIEnv,
    _: JObject,
    set_handle: Handle,
    value: jbyteArray,
) {
    let res = panic::catch_unwind(|| match *utils::cast_handle::<IndexType>(set_handle) {
        IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        IndexType::ForkIndex(ref mut set) => {
            let value = env.convert_byte_array(value)?;
            set.remove(&value);
            Ok(())
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Clears the set, removing all values.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_KeySetIndexProxy_nativeClear(
    env: JNIEnv,
    _: JObject,
    set_handle: Handle,
) {
    let res = panic::catch_unwind(|| match *utils::cast_handle::<IndexType>(set_handle) {
        IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        IndexType::ForkIndex(ref mut set) => {
            set.clear();
            Ok(())
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Return next value from the iterator. Returns null pointer when iteration is finished.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_KeySetIndexProxy_nativeIteratorNext(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let iter = utils::cast_handle::<KeySetIndexIter<Key>>(iter_handle);
        match iter.next() {
            Some(val) => env.byte_array_from_slice(&val),
            None => Ok(ptr::null_mut()),
        }
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Destroys underlying `KeySetIndex` iterator object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_KeySetIndexProxy_nativeIteratorFree(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) {
    utils::drop_handle::<KeySetIndexIter<Key>>(&env, iter_handle);
}
