use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::{jlong, jbyteArray, jboolean};

use std::panic;
use std::ptr;

use exonum::storage::{Snapshot, Fork, ListIndex};
use utils;
use super::db::{View, Value};

type Index<T> = ListIndex<T, Value>;

enum IndexType {
    SnapshotIndex(Index<&'static Box<Snapshot>>),
    ForkIndex(Index<&'static mut Fork>),
}

/// Returns pointer to the created `ListIndex` object.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_binding_index_IndexList_nativeCreate(
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

/// Destroys underlying `ListIndex` object and frees memory.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_binding_index_IndexList_nativeFree(
    env: JNIEnv,
    _: JClass,
    index: jlong,
) {
    utils::drop_object::<IndexType>(&env, index);
}

/// Returns the value by index. Null pointer is returned if value is not found.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_binding_index_IndexList_nativeGet(
    env: JNIEnv,
    _: JClass,
    i: jlong,
    index: jlong,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let val = match utils::cast_object::<IndexType>(index) {
            &mut IndexType::SnapshotIndex(ref index) => index.get(i as u64),
            &mut IndexType::ForkIndex(ref index) => index.get(i as u64),
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

/// Returns the last value or null pointer if the list is empty.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_binding_index_IndexList_nativeLast(
    env: JNIEnv,
    _: JClass,
    index: jlong,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let val = match utils::cast_object::<IndexType>(index) {
            &mut IndexType::SnapshotIndex(ref index) => index.last(),
            &mut IndexType::ForkIndex(ref index) => index.last(),
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

/// Returns `true` if the list is empty.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_binding_index_IndexList_nativeIsEmpty(
    env: JNIEnv,
    _: JClass,
    index: jlong,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        (match utils::cast_object::<IndexType>(index) {
             &mut IndexType::SnapshotIndex(ref index) => index.is_empty(),
             &mut IndexType::ForkIndex(ref index) => index.is_empty(),
         }) as jboolean
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns length of the list.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_binding_index_IndexList_nativeLen(
    env: JNIEnv,
    _: JClass,
    index: jlong,
) -> jlong {
    let res = panic::catch_unwind(|| {
        (match utils::cast_object::<IndexType>(index) {
             &mut IndexType::SnapshotIndex(ref index) => index.len(),
             &mut IndexType::ForkIndex(ref index) => index.len(),
         }) as jlong
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Adds value to the list.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_binding_index_IndexList_nativePush(
    env: JNIEnv,
    _: JClass,
    value: jbyteArray,
    index: jlong,
) {
    let res = panic::catch_unwind(|| match utils::cast_object::<IndexType>(index) {
        &mut IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        &mut IndexType::ForkIndex(ref mut index) => {
            let value = env.convert_byte_array(value).unwrap();
            index.push(value);
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Removes the last element from a list and returns it, or null pointer if it is empty.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_binding_index_IndexList_nativePop(
    env: JNIEnv,
    _: JClass,
    index: jlong,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let val = match utils::cast_object::<IndexType>(index) {
            &mut IndexType::SnapshotIndex(_) => {
                panic!("Unable to modify snapshot.");
            }
            &mut IndexType::ForkIndex(ref mut index) => index.pop(),
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

/// Shortens the list, keeping the first len elements and dropping the rest.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_binding_index_IndexList_nativeTruncate(
    env: JNIEnv,
    _: JClass,
    len: jlong,
    index: jlong,
) {
    let res = panic::catch_unwind(|| match utils::cast_object::<IndexType>(index) {
        &mut IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        &mut IndexType::ForkIndex(ref mut index) => {
            index.truncate(len as u64);
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Sets value into specified index. Panics if `i` is out of bounds.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_binding_index_IndexList_nativeSet(
    env: JNIEnv,
    _: JClass,
    i: jlong,
    value: jbyteArray,
    index: jlong,
) {
    let res = panic::catch_unwind(|| match utils::cast_object::<IndexType>(index) {
        &mut IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        &mut IndexType::ForkIndex(ref mut index) => {
            let value = env.convert_byte_array(value).unwrap();
            index.set(i as u64, value);
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Clears the list, removing all values.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_binding_index_IndexList_nativeClear(
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
