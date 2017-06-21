use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::{jlong, jboolean, jbyteArray};

use std::panic;
use std::ptr;

use exonum::storage::{Snapshot, Fork, MapIndex};
use utils;
use super::db::{View, Key, Value};

type Index<T> = MapIndex<T, Key, Value>;

enum IndexType {
    SnapshotIndex(Index<&'static Box<Snapshot>>),
    ForkIndex(Index<&'static mut Fork>),
}

/// Returns pointer to created `MapIndex` object.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_binding_index_IndexMap_nativeCreate(
    env: JNIEnv,
    _: JClass,
    view: jlong,
    prefix: jbyteArray,
) -> jlong {
    let res = panic::catch_unwind(|| {
        let prefix = env.convert_byte_array(prefix).unwrap();
        Box::into_raw(Box::new(match *utils::cast_object(view) {
            View::Snapshot(ref snapshot) => IndexType::SnapshotIndex(Index::new(prefix, snapshot)),
            View::Fork(ref mut fork) => IndexType::ForkIndex(Index::new(prefix, fork)),
        })) as jlong
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Destroys underlying `MapIndex` object and frees memory.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_binding_index_IndexMap_nativeFree(
    env: JNIEnv,
    _: JClass,
    index: jlong,
) {
    utils::drop_object::<IndexType>(&env, index);
}

/// Returns value identified by the `key`. Null pointer is returned if value is not found.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_binding_index_IndexMap_nativeGet(
    env: JNIEnv,
    _: JClass,
    key: jbyteArray,
    index: jlong,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let key = env.convert_byte_array(key).unwrap()[0];
        let val = match utils::cast_object::<IndexType>(index) {
            &mut IndexType::SnapshotIndex(ref index) => index.get(&key),
            &mut IndexType::ForkIndex(ref index) => index.get(&key),
        };
        match val {
            Some(val) => utils::convert_to_java_array(&env, &val),
            None => ptr::null_mut(),
        }
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Returns `true` if the map contains a value for the specified key.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_binding_index_IndexMap_nativeContains(
    env: JNIEnv,
    _: JClass,
    key: jbyteArray,
    index: jlong,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let key = env.convert_byte_array(key).unwrap()[0];
        (match utils::cast_object::<IndexType>(index) {
             &mut IndexType::SnapshotIndex(ref index) => index.contains(&key),
             &mut IndexType::ForkIndex(ref index) => index.contains(&key),
         }) as jboolean
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Sets `value` identified by the `key` into the index.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_binding_index_IndexMap_nativePut(
    env: JNIEnv,
    _: JClass,
    key: jbyteArray,
    value: jbyteArray,
    index: jlong,
) {
    let res = panic::catch_unwind(|| match utils::cast_object::<IndexType>(index) {
        &mut IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        &mut IndexType::ForkIndex(ref mut index) => {
            let key = env.convert_byte_array(key).unwrap()[0];
            let value = env.convert_byte_array(value).unwrap();
            index.put(&key, value);
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Removes value identified by the `key` from the index.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_binding_index_IndexMap_nativeDelete(
    env: JNIEnv,
    _: JClass,
    key: jbyteArray,
    index: jlong,
) {
    let res = panic::catch_unwind(|| match utils::cast_object::<IndexType>(index) {
        &mut IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        &mut IndexType::ForkIndex(ref mut index) => {
            let key = env.convert_byte_array(key).unwrap()[0];
            index.remove(&key);
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Clears the index, removing all values.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_binding_index_IndexMap_nativeClear(
    env: JNIEnv,
    _: JClass,
    index: jlong,
) {
    let res = panic::catch_unwind(|| match utils::cast_object::<IndexType>(index) {
        &mut IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        &mut IndexType::ForkIndex(ref mut index) => {
            index.clear();
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}
