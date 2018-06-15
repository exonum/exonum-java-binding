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

use exonum::storage::{Snapshot, Fork, Entry};
use jni::JNIEnv;
use jni::objects::{JClass, JObject, JString};
use jni::sys::{jbyteArray, jboolean};

use std::panic;
use std::ptr;

use storage::db::{View, ViewRef, Value};
use utils::{self, Handle};

type Index<T> = Entry<T, Value>;

enum IndexType {
    SnapshotIndex(Index<&'static Snapshot>),
    ForkIndex(Index<&'static mut Fork>),
}

/// Returns pointer to the created `Entry` object.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_EntryIndexProxy_nativeCreate(
    env: JNIEnv,
    _: JClass,
    name: JString,
    view_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let name = utils::convert_to_string(&env, name)?;
        Ok(utils::to_handle(
            match *utils::cast_handle::<View>(view_handle).get() {
                ViewRef::Snapshot(snapshot) => IndexType::SnapshotIndex(
                    Index::new(name, &*snapshot),
                ),
                ViewRef::Fork(ref mut fork) => IndexType::ForkIndex(Index::new(name, fork)),
            },
        ))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Destroys the underlying `Entry` object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_EntryIndexProxy_nativeFree(
    env: JNIEnv,
    _: JClass,
    entry_handle: Handle,
) {
    utils::drop_handle::<IndexType>(&env, entry_handle);
}

/// Returns the value or null pointer if it is absent.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_EntryIndexProxy_nativeGet(
    env: JNIEnv,
    _: JObject,
    entry_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let val = match *utils::cast_handle::<IndexType>(entry_handle) {
            IndexType::SnapshotIndex(ref entry) => entry.get(),
            IndexType::ForkIndex(ref entry) => entry.get(),
        };
        match val {
            Some(val) => env.byte_array_from_slice(&val),
            None => Ok(ptr::null_mut()),
        }
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Returns `true` if the entry contains the value.
#[no_mangle]
pub extern "C" fn Java_com_exonum_binding_storage_indices_EntryIndexProxy_nativeIsPresent(
    env: JNIEnv,
    _: JObject,
    entry_handle: Handle,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        Ok(match *utils::cast_handle::<IndexType>(entry_handle) {
            IndexType::SnapshotIndex(ref entry) => entry.exists(),
            IndexType::ForkIndex(ref entry) => entry.exists(),
        } as jboolean)
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns the hash of the value or default hash if value is absent.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_EntryIndexProxy_nativeGetHash(
    env: JNIEnv,
    _: JObject,
    entry_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        utils::convert_hash(
            &env,
            &match *utils::cast_handle::<IndexType>(entry_handle) {
                IndexType::SnapshotIndex(ref entry) => entry.hash(),
                IndexType::ForkIndex(ref entry) => entry.hash(),
            },
        )
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Inserts value to the entry.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_EntryIndexProxy_nativeSet(
    env: JNIEnv,
    _: JObject,
    entry_handle: Handle,
    value: jbyteArray,
) {
    let res = panic::catch_unwind(|| match *utils::cast_handle::<IndexType>(entry_handle) {
        IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        IndexType::ForkIndex(ref mut entry) => {
            let value = env.convert_byte_array(value)?;
            entry.set(value);
            Ok(())
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Removes a value from the entry.
#[no_mangle]
pub extern "C" fn Java_com_exonum_binding_storage_indices_EntryIndexProxy_nativeRemove(
    env: JNIEnv,
    _: JObject,
    entry_handle: Handle,
) {
    let res = panic::catch_unwind(|| match *utils::cast_handle::<IndexType>(entry_handle) {
        IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        IndexType::ForkIndex(ref mut entry) => {
            entry.remove();
            Ok(())
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}
