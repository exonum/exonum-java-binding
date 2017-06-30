use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::{jboolean, jbyteArray};

use std::panic;
use std::ptr;

use exonum::storage::{Snapshot, Fork, MapIndex};
use utils::{self, Handle};
use super::db::{View, Key, Value};

type Index<T> = MapIndex<T, Key, Value>;

enum IndexType {
    SnapshotIndex(Index<&'static Snapshot>),
    ForkIndex(Index<&'static mut Fork>),
}

/// Returns pointer to created `MapIndex` object.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_MapIndex_nativeCreate(
    env: JNIEnv,
    _: JClass,
    view_handle: Handle,
    prefix: jbyteArray,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let prefix = env.convert_byte_array(prefix).unwrap();
        Box::into_raw(Box::new(match *utils::cast_object(view_handle) {
            View::Snapshot(ref snapshot) => IndexType::SnapshotIndex(
                Index::new(prefix, &**snapshot),
            ),
            View::Fork(ref mut fork) => IndexType::ForkIndex(Index::new(prefix, fork)),
        })) as Handle
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Destroys underlying `MapIndex` object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_MapIndex_nativeFree(
    env: JNIEnv,
    _: JClass,
    map_handle: Handle,
) {
    utils::drop_object::<IndexType>(&env, map_handle);
}

/// Returns value identified by the `key`. Null pointer is returned if value is not found.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_MapIndex_nativeGet(
    env: JNIEnv,
    _: JClass,
    key: jbyteArray,
    map_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let key = env.convert_byte_array(key).unwrap()[0];
        let val = match *utils::cast_object::<IndexType>(map_handle) {
            IndexType::SnapshotIndex(ref map) => map.get(&key),
            IndexType::ForkIndex(ref map) => map.get(&key),
        };
        match val {
            Some(val) => env.byte_array_from_slice(&val).unwrap(),
            None => ptr::null_mut(),
        }
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Returns `true` if the map contains a value for the specified key.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_MapIndex_nativeContains(
    env: JNIEnv,
    _: JClass,
    key: jbyteArray,
    map_handle: Handle,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let key = env.convert_byte_array(key).unwrap()[0];
        (match *utils::cast_object::<IndexType>(map_handle) {
             IndexType::SnapshotIndex(ref map) => map.contains(&key),
             IndexType::ForkIndex(ref map) => map.contains(&key),
         }) as jboolean
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Sets `value` identified by the `key` into the index.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_MapIndex_nativePut(
    env: JNIEnv,
    _: JClass,
    key: jbyteArray,
    value: jbyteArray,
    map_handle: Handle,
) {
    let res = panic::catch_unwind(|| match *utils::cast_object::<IndexType>(map_handle) {
        IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        IndexType::ForkIndex(ref mut map) => {
            let key = env.convert_byte_array(key).unwrap()[0];
            let value = env.convert_byte_array(value).unwrap();
            map.put(&key, value);
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Removes value identified by the `key` from the index.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_MapIndex_nativeDelete(
    env: JNIEnv,
    _: JClass,
    key: jbyteArray,
    map_handle: Handle,
) {
    let res = panic::catch_unwind(|| match *utils::cast_object::<IndexType>(map_handle) {
        IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        IndexType::ForkIndex(ref mut map) => {
            let key = env.convert_byte_array(key).unwrap()[0];
            map.remove(&key);
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Clears the index, removing all values.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_MapIndex_nativeClear(
    env: JNIEnv,
    _: JClass,
    map_handle: Handle,
) {
    let res = panic::catch_unwind(|| match *utils::cast_object::<IndexType>(map_handle) {
        IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        IndexType::ForkIndex(ref mut map) => {
            map.clear();
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}
