use jni::JNIEnv;
use jni::objects::{JClass, JObject, JString};
use jni::sys::{jbyteArray, jboolean, jobject};

use std::panic;
use std::ptr;

use exonum::storage::{Snapshot, Fork, ValueSetIndex};
use exonum::storage::value_set_index::{ValueSetIndexIter, ValueSetIndexHashes};
use utils::{self, Handle, PairIter};
use super::db::{View, ViewRef, Value};
use super::indexes_metadata::{TableType, check_read, check_write};

type Index<T> = ValueSetIndex<T, Value>;

enum IndexType {
    SnapshotIndex(Index<&'static Snapshot>),
    ForkIndex(Index<&'static mut Fork>),
}

type Iter<'a> = PairIter<ValueSetIndexIter<'a, Value>>;

const JAVA_ENTRY_FQN: &str = "com/exonum/binding/storage/indices/ValueSetIndexProxy$EntryInternal";

/// Returns pointer to the created `ValueSetIndex` object.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ValueSetIndexProxy_nativeCreate(
    env: JNIEnv,
    _: JClass,
    name: JString,
    view_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let name = utils::convert_to_string(&env, name)?;
        Ok(utils::to_handle(
            match *utils::cast_handle::<View>(view_handle).get() {
                ViewRef::Snapshot(snapshot) => {
                    check_read(&name, TableType::ValueSet, &*snapshot);
                    IndexType::SnapshotIndex(Index::new(name, &*snapshot))
                }
                ViewRef::Fork(ref mut fork) => {
                    check_write(&name, TableType::ValueSet, fork);
                    IndexType::ForkIndex(Index::new(name, fork))
                }
            },
        ))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Destroys the underlying `ValueSetIndex` object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ValueSetIndexProxy_nativeFree(
    env: JNIEnv,
    _: JObject,
    set_handle: Handle,
) {
    utils::drop_handle::<IndexType>(&env, set_handle);
}

/// Returns `true` if the set contains the specified value.
#[no_mangle]
pub extern "C" fn Java_com_exonum_binding_storage_indices_ValueSetIndexProxy_nativeContains(
    env: JNIEnv,
    _: JObject,
    set_handle: Handle,
    value: jbyteArray,
) -> jboolean {
    let res = panic::catch_unwind(|| {
        let value = env.convert_byte_array(value)?;
        Ok(match *utils::cast_handle::<IndexType>(set_handle) {
            IndexType::SnapshotIndex(ref set) => set.contains(&value),
            IndexType::ForkIndex(ref set) => set.contains(&value),
        } as jboolean)
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns `true` if the set contains value with the specified hash.
#[no_mangle]
pub extern "C" fn Java_com_exonum_binding_storage_indices_ValueSetIndexProxy_nativeContainsByHash(
    env: JNIEnv,
    _: JObject,
    set_handle: Handle,
    hash: jbyteArray,
) -> jboolean{
    let res = panic::catch_unwind(|| {
        let hash = utils::convert_to_hash(&env, hash)?;
        Ok(match *utils::cast_handle::<IndexType>(set_handle) {
            IndexType::SnapshotIndex(ref set) => set.contains_by_hash(&hash),
            IndexType::ForkIndex(ref set) => set.contains_by_hash(&hash),
        } as jboolean)
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns the pointer to the iterator over a set that returns a pair of value and its hash.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ValueSetIndexProxy_nativeCreateIterator(
    env: JNIEnv,
    _: JObject,
    set_handle: Handle,
) -> Handle{
    let res = panic::catch_unwind(|| {
        let iter = match *utils::cast_handle::<IndexType>(set_handle) {
            IndexType::SnapshotIndex(ref set) => set.iter(),
            IndexType::ForkIndex(ref set) => set.iter(),
        };
        let iter = Iter::new(&env, iter, JAVA_ENTRY_FQN)?;
        Ok(utils::to_handle(iter))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns pointer to the iterator over set starting from the given hash.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ValueSetIndexProxy_nativeCreateIterFrom(
    env: JNIEnv,
    _: JObject,
    set_handle: Handle,
    from: jbyteArray,
) -> Handle{
    let res = panic::catch_unwind(|| {
        let from = utils::convert_to_hash(&env, from)?;
        let iter = match *utils::cast_handle::<IndexType>(set_handle) {
            IndexType::SnapshotIndex(ref set) => set.iter_from(&from),
            IndexType::ForkIndex(ref set) => set.iter_from(&from),
        };
        let iter = Iter::new(&env, iter, JAVA_ENTRY_FQN)?;
        Ok(utils::to_handle(iter))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns pointer to the iterator over set that returns hashes of values.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ValueSetIndexProxy_nativeCreateHashIterator(
    env: JNIEnv,
    _: JObject,
    set_handle: Handle,
) -> Handle{
    let res = panic::catch_unwind(|| {
        Ok(utils::to_handle(
            match *utils::cast_handle::<IndexType>(set_handle) {
                IndexType::SnapshotIndex(ref set) => set.hashes(),
                IndexType::ForkIndex(ref set) => set.hashes(),
            },
        ))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns pointer to the hash-iterator over set starting from the given hash.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ValueSetIndexProxy_nativeCreateHashIterFrom(
    env: JNIEnv,
    _: JObject,
    set_handle: Handle,
    from: jbyteArray,
) -> Handle{
    let res = panic::catch_unwind(|| {
        let from = utils::convert_to_hash(&env, from)?;
        Ok(utils::to_handle(
            match *utils::cast_handle::<IndexType>(set_handle) {
                IndexType::SnapshotIndex(ref set) => set.hashes_from(&from),
                IndexType::ForkIndex(ref set) => set.hashes_from(&from),
            },
        ))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Inserts value to the set.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ValueSetIndexProxy_nativeAdd(
    env: JNIEnv,
    _: JObject,
    set_handle: Handle,
    value: jbyteArray,
) {
    let res = panic::catch_unwind(|| match *utils::cast_handle::<IndexType>(set_handle) {
        IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        IndexType::ForkIndex(ref mut set) => {
            let value = env.convert_byte_array(value)?;
            set.insert(value);
            Ok(())
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Removes value from the set.
#[no_mangle]
pub extern "C" fn Java_com_exonum_binding_storage_indices_ValueSetIndexProxy_nativeRemove(
    env: JNIEnv,
    _: JObject,
    set_handle: Handle,
    value: jbyteArray,
) {
    let res = panic::catch_unwind(|| match *utils::cast_handle::<IndexType>(set_handle) {
        IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        IndexType::ForkIndex(ref mut set) => {
            let value = env.convert_byte_array(value)?;
            set.remove(&value);
            Ok(())
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Removes value with given hash from the set.
#[no_mangle]
pub extern "C" fn Java_com_exonum_binding_storage_indices_ValueSetIndexProxy_nativeRemoveByHash(
    env: JNIEnv,
    _: JObject,
    set_handle: Handle,
    hash: jbyteArray,
) {
    let res = panic::catch_unwind(|| match *utils::cast_handle::<IndexType>(set_handle) {
        IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        IndexType::ForkIndex(ref mut set) => {
            let hash = utils::convert_to_hash(&env, hash)?;
            set.remove_by_hash(&hash);
            Ok(())
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Clears the set, removing all values.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ValueSetIndexProxy_nativeClear(
    env: JNIEnv,
    _: JObject,
    set_handle: Handle,
) {
    let res = panic::catch_unwind(|| match *utils::cast_handle::<IndexType>(set_handle) {
        IndexType::SnapshotIndex(_) => {
            panic!("Unable to modify snapshot.");
        }
        IndexType::ForkIndex(ref mut set) => {
            set.clear();
            Ok(())
        }
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns next value from the iterator. Returns null pointer when iteration is finished.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ValueSetIndexProxy_nativeIteratorNext(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) -> jobject{
    let res = panic::catch_unwind(|| {
        let iterWrapper = utils::cast_handle::<Iter>(iter_handle);
        match iterWrapper.iter.next() {
            Some(val) => {
                let hash: JObject = utils::convert_hash(&env, &val.0)?.into();
                let value: JObject = env.byte_array_from_slice(&val.1)?.into();
                Ok(
                    env.new_object_by_id(
                        &iterWrapper.element_class,
                        iterWrapper.constructor_id,
                        &[hash.into(), value.into()],
                    )?
                        .into_inner(),
                )
            }
            None => Ok(ptr::null_mut()),
        }
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Destroys the underlying `ValueSetIndex` iterator object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ValueSetIndexProxy_nativeIteratorFree(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
){
    utils::drop_handle::<Iter>(&env, iter_handle);
}

/// Returns next value from the hash-iterator. Returns null pointer when iteration is finished.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ValueSetIndexProxy_nativeHashIteratorNext(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
) -> jbyteArray{
    let res = panic::catch_unwind(|| {
        let iter = utils::cast_handle::<ValueSetIndexHashes>(iter_handle);
        match iter.next() {
            Some(val) => utils::convert_hash(&env, &val),
            None => Ok(ptr::null_mut()),
        }
    });
    utils::unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Destroys the underlying `ValueSetIndex` hash-iterator object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_indices_ValueSetIndexProxy_nativeHashIteratorFree(
    env: JNIEnv,
    _: JObject,
    iter_handle: Handle,
){
    utils::drop_handle::<ValueSetIndexHashes>(&env, iter_handle);
}
