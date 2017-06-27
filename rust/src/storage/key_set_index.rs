use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::{jboolean, jbyteArray};

use std::panic;

use exonum::storage::{Snapshot, Fork, KeySetIndex};
use utils::{self, Handle};
use super::db::{View, Key};

type Index<T> = KeySetIndex<T, Key>;

enum IndexType {
    SnapshotIndex(Index<&'static Snapshot>),
    ForkIndex(Index<&'static mut Fork>),
}

/// Returns pointer to created `KeySetIndex` object.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_com_exonum_binding_index_KeySetIndex_nativeCreate(
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

/// Destroys underlying `KeySetIndex` object and frees memory.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_com_exonum_binding_index_KeySetIndex_nativeFree(
    env: JNIEnv,
    _: JClass,
    set_handle: Handle,
) {
    utils::drop_object::<IndexType>(&env, set_handle);
}

/// Returns `true` if the set contains the specified value.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_com_exonum_binding_index_KeySetIndex_nativeContains(
    env: JNIEnv,
    _: JClass,
    value: jbyteArray,
    set_handle: Handle,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let value = env.convert_byte_array(value).unwrap()[0];
        (match *utils::cast_object::<IndexType>(set_handle) {
             IndexType::SnapshotIndex(ref set) => set.contains(&value),
             IndexType::ForkIndex(ref set) => set.contains(&value),
         }) as jboolean
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Inserts value in the set.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_com_exonum_binding_index_KeySetIndex_nativeInsert(
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
            let value = env.convert_byte_array(value).unwrap()[0];
            set.insert(value);
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Removes value from the set.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_com_exonum_binding_index_KeySetIndex_nativeRemove(
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
            let value = env.convert_byte_array(value).unwrap()[0];
            set.remove(&value);
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Clears the set, removing all values.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_com_exonum_binding_index_KeySetIndex_nativeClear(
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
