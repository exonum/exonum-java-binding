use jni::JNIEnv;
use jni::objects::{JClass, JObject, JString};
use jni::sys::{jbyteArray, jboolean};

use std::panic;
use std::ptr;

use exonum::storage::{Snapshot, Fork, Entry};
use utils::{self, Handle};
use super::db::{View, Value};

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
        Ok(utils::to_handle(match *utils::cast_handle(view_handle) {
            View::Snapshot(ref snapshot) => IndexType::SnapshotIndex(
                Index::new(name, &**snapshot),
            ),
            View::Fork(ref mut fork) => IndexType::ForkIndex(Index::new(name, fork)),
        }))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Destroys the underlying `Entry` object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_EntryIndexProxy_nativeFree(
    env: JNIEnv,
    _: JObject,
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
