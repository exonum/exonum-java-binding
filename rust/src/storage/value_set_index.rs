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

/// Destroys underlying `ValueSetIndex` object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_ValueSetIndex_nativeFree(
    env: JNIEnv,
    _: JClass,
    list_handle: Handle,
) {
    utils::drop_object::<IndexType>(&env, list_handle);
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

// TODO: `contains_by_hash`.

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

// TODO: remove_by_hash

/// Clears the set, removing all values.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_ValueSetIndex_nativeClear(
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
