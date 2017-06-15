use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::{jlong, jbyteArray};

use std::panic;
use std::ptr;

use exonum::storage::{self, Snapshot, Fork};
use utils;
use super::db::{View, Key, Value};

type Index<T> = storage::MapIndex<T, Key, Value>;

enum IndexType {
    SnapshotIndex(Index<&'static Box<Snapshot>>),
    ForkIndex(Index<&'static mut Fork>),
}

/// Returns pointer to created `MapIndex` object.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_binding_index_IndexMap_createNativeIndexMap(
    env: JNIEnv,
    _: JClass,
    view: jlong,
    prefix: jbyteArray,
) -> jlong {
    let res = panic::catch_unwind(|| {
        let prefix = utils::bytes_array_to_vec(&env, prefix);
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
pub extern "C" fn Java_com_exonum_binding_index_IndexMap_freeNativeIndexMap(
    env: JNIEnv,
    _: JClass,
    index: jlong,
) {
    utils::drop_object::<IndexType>(&env, index);
}

/// Sets `value` identified by the `key` into the index.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_binding_index_IndexMap_putToIndexMap(
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
            let key = utils::bytes_array_to_vec(&env, key)[0];
            let value = utils::bytes_array_to_vec(&env, value);
            index.put(&key, value);
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns value identified by the `key`. Null pointer is returned if value is not found.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_binding_index_IndexMap_getFromIndexMap(
    env: JNIEnv,
    _: JClass,
    key: jbyteArray,
    index: jlong,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let key = utils::bytes_array_to_vec(&env, key)[0];
        let val = match utils::cast_object::<IndexType>(index) {
            &mut IndexType::SnapshotIndex(ref index) => index.get(&key),
            &mut IndexType::ForkIndex(ref index) => index.get(&key),
        };
        match val {
            Some(val) => {
                // TODO: Remove casting.
                let signed: Vec<_> = val.iter().map(|x| *x as i8).collect();
                env.new_byte_array(signed.as_slice()).unwrap()
            }
            None => ptr::null_mut(),
        }
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Removes value identified by the `key` from the index.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_binding_index_IndexMap_deleteFromIndexMap(
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
            let key = utils::bytes_array_to_vec(&env, key)[0];
            index.remove(&key);
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}
