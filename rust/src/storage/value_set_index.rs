use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::{jlong, jbyteArray, jboolean};

use std::panic;

use exonum::storage::{Snapshot, Fork, ValueSetIndex};
use utils::{self, Handle};
use super::db::{View, Value};

type Index<T> = ValueSetIndex<T, Value>;

enum IndexType {
    SnapshotIndex(Index<&'static Snapshot>),
    ForkIndex(Index<&'static mut Fork>),
}

/// Returns pointer to the created `ValueSetIndex` object.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_ValueSetIndex_nativeCreate(
    env: JNIEnv,
    _: JClass,
    view_handle: jlong,
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

/// Destroys underlying `ValueSetIndex` object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_ValueSetIndex_nativeFree(
    env: JNIEnv,
    _: JClass,
    set_handle: Handle,
) {
    utils::drop_object::<IndexType>(&env, set_handle);
}

/// Returns `true` if the set contains the specified value.
#[no_mangle]
pub extern "C" fn Java_com_exonum_binding_index_ValueSetIndex_nativeContains(
    env: JNIEnv,
    _: JClass,
    value: jbyteArray,
    set_handle: Handle,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let value = env.convert_byte_array(value).unwrap();
        (match *utils::cast_object::<IndexType>(set_handle) {
             IndexType::SnapshotIndex(ref set) => set.contains(&value),
             IndexType::ForkIndex(ref set) => set.contains(&value),
         }) as jboolean
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns `true` if the set contains value with the specified hash.
#[no_mangle]
pub extern "C" fn Java_com_exonum_binding_index_ValueSetIndex_nativeContainsByHash(
    env: JNIEnv,
    _: JClass,
    hash: jbyteArray,
    set_handle: Handle,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let hash = utils::convert_to_hash(&env, hash);
        (match *utils::cast_object::<IndexType>(set_handle) {
             IndexType::SnapshotIndex(ref set) => set.contains_by_hash(&hash),
             IndexType::ForkIndex(ref set) => set.contains_by_hash(&hash),
         }) as jboolean
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Inserts value to the set.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_ValueSetIndex_nativeInsert(
    env: JNIEnv,
    _: JClass,
    value: jbyteArray,
    set_handle: Handle,
) {
    let res = panic::catch_unwind(|| match *utils::cast_object::<IndexType>(set_handle) {
        IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        IndexType::ForkIndex(ref mut set) => {
            let value = env.convert_byte_array(value).unwrap();
            set.insert(value);
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Removes value from the set.
#[no_mangle]
pub extern "C" fn Java_com_exonum_binding_index_ValueSetIndex_nativeRemove(
    env: JNIEnv,
    _: JClass,
    value: jbyteArray,
    set_handle: Handle,
) {
    let res = panic::catch_unwind(|| match *utils::cast_object::<IndexType>(set_handle) {
        IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        IndexType::ForkIndex(ref mut set) => {
            let value = env.convert_byte_array(value).unwrap();
            set.remove(&value);
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Removes value with given hash from the set.
#[no_mangle]
pub extern "C" fn Java_com_exonum_binding_index_ValueSetIndex_nativeRemoveByHash(
    env: JNIEnv,
    _: JClass,
    hash: jbyteArray,
    set_handle: Handle,
) {
    let res = panic::catch_unwind(|| match *utils::cast_object::<IndexType>(set_handle) {
        IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        IndexType::ForkIndex(ref mut set) => {
            let hash = utils::convert_to_hash(&env, hash);
            set.remove_by_hash(&hash);
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Clears the set, removing all values.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_ValueSetIndex_nativeClear(
    env: JNIEnv,
    _: JClass,
    set_handle: Handle,
) {
    let res = panic::catch_unwind(|| match *utils::cast_object::<IndexType>(set_handle) {
        IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        IndexType::ForkIndex(ref mut set) => {
            set.clear();
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}
