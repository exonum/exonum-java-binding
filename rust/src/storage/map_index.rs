use jni::JNIEnv;
use jni::objects::{JClass, JObject};
use jni::sys::{jboolean, jbyteArray};

use std::panic;
use std::ptr;

use exonum::storage::{Snapshot, Fork, MapIndex};
use exonum::storage::map_index::{MapIndexKeys, MapIndexValues};
use utils::{self, Handle};
use super::db::{View, Key, Value};

type Index<T> = MapIndex<T, Key, Value>;

enum IndexType {
    SnapshotIndex(Index<&'static Snapshot>),
    ForkIndex(Index<&'static mut Fork>),
}

/// Returns a pointer to created `MapIndex` object.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_proxy_MapIndexProxy_nativeCreate(
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
pub extern "system" fn Java_com_exonum_binding_proxy_MapIndexProxy_nativeFree(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
) {
    utils::drop_object::<IndexType>(&env, map_handle);
}

/// Returns value identified by the `key`. Null pointer is returned if value is not found.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_proxy_MapIndexProxy_nativeGet(
    env: JNIEnv,
    _: JObject,
    key: jbyteArray,
    map_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let key = env.convert_byte_array(key).unwrap();
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
pub extern "system" fn Java_com_exonum_binding_proxy_MapIndexProxy_nativeContains(
    env: JNIEnv,
    _: JObject,
    key: jbyteArray,
    map_handle: Handle,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let key = env.convert_byte_array(key).unwrap();
        (match *utils::cast_object::<IndexType>(map_handle) {
             IndexType::SnapshotIndex(ref map) => map.contains(&key),
             IndexType::ForkIndex(ref map) => map.contains(&key),
         }) as jboolean
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns a pointer to the iterator over map keys.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_proxy_MapIndexProxy_nativeCreateKeysIter(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        Box::into_raw(Box::new(
            match *utils::cast_object::<IndexType>(map_handle) {
                IndexType::SnapshotIndex(ref map) => map.keys(),
                IndexType::ForkIndex(ref map) => map.keys(),
            },
        )) as Handle
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns a pointer to the iterator over map values.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_proxy_MapIndexProxy_nativeCreateValuesIter(
    env: JNIEnv,
    _: JObject,
    map_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        Box::into_raw(Box::new(
            match *utils::cast_object::<IndexType>(map_handle) {
                IndexType::SnapshotIndex(ref map) => map.values(),
                IndexType::ForkIndex(ref map) => map.values(),
            },
        )) as Handle
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns a pointer to the iterator over map keys starting at the given key.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_proxy_MapIndexProxy_nativeKeysFrom(
    env: JNIEnv,
    _: JClass,
    key: jbyteArray,
    map_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let key = env.convert_byte_array(key).unwrap();
        Box::into_raw(Box::new(
            match *utils::cast_object::<IndexType>(map_handle) {
                IndexType::SnapshotIndex(ref map) => map.keys_from(&key),
                IndexType::ForkIndex(ref map) => map.keys_from(&key),
            },
        )) as Handle
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns a pointer to the iterator over map values starting at the given key.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_proxy_MapIndexProxy_nativeValuesFrom(
    env: JNIEnv,
    _: JClass,
    key: jbyteArray,
    map_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let key = env.convert_byte_array(key).unwrap();
        Box::into_raw(Box::new(
            match *utils::cast_object::<IndexType>(map_handle) {
                IndexType::SnapshotIndex(ref map) => map.values_from(&key),
                IndexType::ForkIndex(ref map) => map.values_from(&key),
            },
        )) as Handle
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Sets `value` identified by the `key` into the index.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_proxy_MapIndexProxy_nativePut(
    env: JNIEnv,
    _: JObject,
    key: jbyteArray,
    value: jbyteArray,
    map_handle: Handle,
) {
    let res = panic::catch_unwind(|| match *utils::cast_object::<IndexType>(map_handle) {
        IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        IndexType::ForkIndex(ref mut map) => {
            let key = env.convert_byte_array(key).unwrap();
            let value = env.convert_byte_array(value).unwrap();
            map.put(&key, value);
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Removes value identified by the `key` from the index.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_proxy_MapIndexProxy_nativeRemove(
    env: JNIEnv,
    _: JObject,
    key: jbyteArray,
    map_handle: Handle,
) {
    let res = panic::catch_unwind(|| match *utils::cast_object::<IndexType>(map_handle) {
        IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        IndexType::ForkIndex(ref mut map) => {
            let key = env.convert_byte_array(key).unwrap();
            map.remove(&key);
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Clears the index, removing all values.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_proxy_MapIndexProxy_nativeClear(
    env: JNIEnv,
    _: JObject,
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

/// Returns the next value from the keys-iterator. Returns null pointer when iteration is finished.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_proxy_MapIndexProxy_nativeKeysIterNext(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let mut iter = utils::cast_object::<MapIndexKeys<Key>>(iter_handle);
        match iter.next() {
            Some(val) => env.byte_array_from_slice(&val).unwrap(),
            None => ptr::null_mut(),
        }
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Destroys underlying `MapIndex` keys-iterator object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_proxy_MapIndexProxy_nativeKeysIterFree(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) {
    utils::drop_object::<MapIndexKeys<Key>>(&env, iter_handle);
}

/// Return next value from the values-iterator. Returns null pointer when iteration is finished.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_proxy_MapIndexProxy_nativeValuesIterNext(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let mut iter = utils::cast_object::<MapIndexValues<Value>>(iter_handle);
        match iter.next() {
            Some(val) => env.byte_array_from_slice(&val).unwrap(),
            None => ptr::null_mut(),
        }
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Destroys underlying `MapIndex` values-iterator object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_proxy_MapIndexProxy_nativeValuesIterFree(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) {
    utils::drop_object::<MapIndexValues<Value>>(&env, iter_handle);
}
