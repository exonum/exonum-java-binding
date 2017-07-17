use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::{jboolean, jbyteArray};

use std::panic;
use std::ptr;

use exonum::storage::{Snapshot, Fork, KeySetIndex};
use exonum::storage::key_set_index::KeySetIndexIter;
use utils::{self, Handle};
use super::db::{View, Key};

type Index<T> = KeySetIndex<T, Key>;

enum IndexType {
    SnapshotIndex(Index<&'static Snapshot>),
    ForkIndex(Index<&'static mut Fork>),
}

/// Returns pointer to created `KeySetIndex` object.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_KeySetIndex_nativeCreate(
    env: JNIEnv,
    _: JClass,
    prefix: jbyteArray,
    view_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let prefix = env.convert_byte_array(prefix).unwrap();
        utils::to_handle(match *utils::cast_handle(view_handle) {
            View::Snapshot(ref snapshot) => IndexType::SnapshotIndex(
                Index::new(prefix, &**snapshot),
            ),
            View::Fork(ref mut fork) => IndexType::ForkIndex(Index::new(prefix, fork)),
        })
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Destroys underlying `KeySetIndex` object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_KeySetIndex_nativeFree(
    env: JNIEnv,
    _: JClass,
    set_handle: Handle,
) {
    utils::drop_handle::<IndexType>(&env, set_handle);
}

/// Returns `true` if the set contains the specified value.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_KeySetIndex_nativeContains(
    env: JNIEnv,
    _: JClass,
    value: jbyteArray,
    set_handle: Handle,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let value = env.convert_byte_array(value).unwrap();
        (match *utils::cast_handle::<IndexType>(set_handle) {
             IndexType::SnapshotIndex(ref set) => set.contains(&value),
             IndexType::ForkIndex(ref set) => set.contains(&value),
         }) as jboolean
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns pointer to the iterator over set.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_KeySetIndex_nativeIter(
    env: JNIEnv,
    _: JClass,
    set_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        utils::to_handle(match *utils::cast_handle::<IndexType>(set_handle) {
            IndexType::SnapshotIndex(ref set) => set.iter(),
            IndexType::ForkIndex(ref set) => set.iter(),
        })
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns pointer to the iterator over set starting at the given key.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_KeySetIndex_nativeIterFrom(
    env: JNIEnv,
    _: JClass,
    from: jbyteArray,
    set_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let from = env.convert_byte_array(from).unwrap();
        utils::to_handle(match *utils::cast_handle::<IndexType>(set_handle) {
            IndexType::SnapshotIndex(ref set) => set.iter_from(&from),
            IndexType::ForkIndex(ref set) => set.iter_from(&from),
        })
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Inserts value in the set.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_KeySetIndex_nativeInsert(
    env: JNIEnv,
    _: JClass,
    value: jbyteArray,
    set_handle: Handle,
) {
    let res = panic::catch_unwind(|| match *utils::cast_handle::<IndexType>(set_handle) {
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
pub extern "system" fn Java_com_exonum_binding_index_KeySetIndex_nativeRemove(
    env: JNIEnv,
    _: JClass,
    value: jbyteArray,
    set_handle: Handle,
) {
    let res = panic::catch_unwind(|| match *utils::cast_handle::<IndexType>(set_handle) {
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

/// Clears the set, removing all values.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_KeySetIndex_nativeClear(
    env: JNIEnv,
    _: JClass,
    set_handle: Handle,
) {
    let res = panic::catch_unwind(|| match *utils::cast_handle::<IndexType>(set_handle) {
        IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        IndexType::ForkIndex(ref mut set) => {
            set.clear();
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Return next value from the iterator. Returns null pointer when iteration is finished.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_KeySetIndex_nativeIterNext(
    env: JNIEnv,
    _: JClass,
    iter_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let mut iter = utils::cast_handle::<KeySetIndexIter<Key>>(iter_handle);
        match iter.next() {
            Some(val) => env.byte_array_from_slice(&val).unwrap(),
            None => ptr::null_mut(),
        }
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Destroys underlying `KeySetIndex` iterator object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_index_KeySetIndex_nativeIterFree(
    env: JNIEnv,
    _: JClass,
    iter_handle: Handle,
) {
    utils::drop_handle::<KeySetIndexIter<Key>>(&env, iter_handle);
}
