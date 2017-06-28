use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::{jlong, jbyteArray, jboolean};

use std::panic;
use std::ptr;

use exonum::storage::{Snapshot, Fork, ListIndex};
use utils::{self, Handle};
use super::db::{View, Value};

type Index<T> = ListIndex<T, Value>;

enum IndexType {
    SnapshotIndex(Index<&'static Snapshot>),
    ForkIndex(Index<&'static mut Fork>),
}

/// Returns pointer to the created `ListIndex` object.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_IndexList_nativeCreate(
    env: JNIEnv,
    _: JClass,
    view: jlong,
    prefix: jbyteArray,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let prefix = env.convert_byte_array(prefix).unwrap();
        Box::into_raw(Box::new(match *utils::cast_object(view) {
            View::Snapshot(ref snapshot) => IndexType::SnapshotIndex(
                Index::new(prefix, &**snapshot),
            ),
            View::Fork(ref mut fork) => IndexType::ForkIndex(Index::new(prefix, fork)),
        })) as Handle
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Destroys underlying `ListIndex` object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_IndexList_nativeFree(
    env: JNIEnv,
    _: JClass,
    list_handle: Handle,
) {
    utils::drop_object::<IndexType>(&env, list_handle);
}

/// Returns the value by index. Null pointer is returned if value is not found.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_IndexList_nativeGet(
    env: JNIEnv,
    _: JClass,
    index: jlong,
    list_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let val = match *utils::cast_object::<IndexType>(list_handle) {
            IndexType::SnapshotIndex(ref list) => list.get(index as u64),
            IndexType::ForkIndex(ref list) => list.get(index as u64),
        };
        match val {
            Some(val) => env.byte_array_from_slice(&val).unwrap(),
            None => ptr::null_mut(),
        }
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Returns the last value or null pointer if the list is empty.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_IndexList_nativeLast(
    env: JNIEnv,
    _: JClass,
    list_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let val = match *utils::cast_object::<IndexType>(list_handle) {
            IndexType::SnapshotIndex(ref list) => list.last(),
            IndexType::ForkIndex(ref list) => list.last(),
        };
        match val {
            Some(val) => env.byte_array_from_slice(&val).unwrap(),
            None => ptr::null_mut(),
        }
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Returns `true` if the list is empty.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_IndexList_nativeIsEmpty(
    env: JNIEnv,
    _: JClass,
    list_handle: Handle,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        (match *utils::cast_object::<IndexType>(list_handle) {
             IndexType::SnapshotIndex(ref list) => list.is_empty(),
             IndexType::ForkIndex(ref list) => list.is_empty(),
         }) as jboolean
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns length of the list.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_IndexList_nativeLen(
    env: JNIEnv,
    _: JClass,
    list_handle: Handle,
) -> jlong {
    let res = panic::catch_unwind(|| {
        (match *utils::cast_object::<IndexType>(list_handle) {
             IndexType::SnapshotIndex(ref list) => list.len(),
             IndexType::ForkIndex(ref list) => list.len(),
         }) as jlong
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Adds value to the list.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_IndexList_nativePush(
    env: JNIEnv,
    _: JClass,
    value: jbyteArray,
    list_handle: Handle,
) {
    let res = panic::catch_unwind(|| match *utils::cast_object::<IndexType>(list_handle) {
        IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        IndexType::ForkIndex(ref mut list) => {
            let value = env.convert_byte_array(value).unwrap();
            list.push(value);
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Removes the last element from a list and returns it, or null pointer if it is empty.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_IndexList_nativePop(
    env: JNIEnv,
    _: JClass,
    list_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let val = match *utils::cast_object::<IndexType>(list_handle) {
            IndexType::SnapshotIndex(_) => {
                panic!("Unable to modify snapshot.");
            }
            IndexType::ForkIndex(ref mut list) => list.pop(),
        };
        match val {
            Some(val) => env.byte_array_from_slice(&val).unwrap(),
            None => ptr::null_mut(),
        }
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Shortens the list, keeping the first len elements and dropping the rest.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_IndexList_nativeTruncate(
    env: JNIEnv,
    _: JClass,
    len: jlong,
    list_handle: Handle,
) {
    let res = panic::catch_unwind(|| match *utils::cast_object::<IndexType>(list_handle) {
        IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        IndexType::ForkIndex(ref mut list) => {
            list.truncate(len as u64);
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Sets value into specified index. Panics if `i` is out of bounds.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_IndexList_nativeSet(
    env: JNIEnv,
    _: JClass,
    index: jlong,
    value: jbyteArray,
    list_handle: Handle,
) {
    let res = panic::catch_unwind(|| match *utils::cast_object::<IndexType>(list_handle) {
        IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        IndexType::ForkIndex(ref mut list) => {
            let value = env.convert_byte_array(value).unwrap();
            list.set(index as u64, value);
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Clears the list, removing all values.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_IndexList_nativeClear(
    env: JNIEnv,
    _: JClass,
    list_handle: Handle,
) {
    let res = panic::catch_unwind(|| match *utils::cast_object::<IndexType>(list_handle) {
        IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        IndexType::ForkIndex(ref mut list) => {
            list.clear();
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}
