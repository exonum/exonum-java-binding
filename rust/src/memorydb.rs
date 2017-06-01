use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::jlong;

use std::panic;

use exonum::storage2::{MemoryDB, Database, Snapshot, Fork};
use utils;

/// Returns pointer to created `MemoryDB` object.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_storage_DB_MemoryDB_nativeCreateMemoryDB(env: JNIEnv,
                                                                           _: JClass)
                                                                           -> jlong {
    let res = panic::catch_unwind(|| Box::into_raw(Box::new(MemoryDB::new())) as jlong);
    utils::unwrap_or_exception(env, res)
}

/// Destroys underlying `MemoryDB` object and frees memory.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_storage_DB_MemoryDB_nativeFreeMemoryDB(env: JNIEnv,
                                                                         _: JClass,
                                                                         db: jlong) {
    utils::drop_object::<MemoryDB>(env, db);
}

/// Returns pointer to created `Snapshot` object.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_storage_DB_MemoryDB_nativeLookupSnapshot(env: JNIEnv,
                                                                           _: JClass,
                                                                           db: jlong)
                                                                           -> jlong {
    let res = panic::catch_unwind(|| {
        let db = utils::cast_object::<MemoryDB>(db);
        // Additional boxing here is needed because "trait object" is represented by two pointers
        // and cannot be cast into `jlong`.
        Box::into_raw(Box::new(db.snapshot())) as jlong
    });
    utils::unwrap_or_exception(env, res)
}

/// Destroys underlying `Snapshot` object and frees memory.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_storage_DB_MemoryDB_nativeFreeSnapshot(env: JNIEnv,
                                                                         _: JClass,
                                                                         db: jlong) {
    utils::drop_object::<Box<Snapshot>>(env, db);
}

/// Returns pointer to created `Fork` object.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_storage_DB_MemoryDB_nativeLookupFork(env: JNIEnv,
                                                                       _: JClass,
                                                                       db: jlong)
                                                                       -> jlong {
    let res = panic::catch_unwind(|| {
                                      let db = utils::cast_object::<MemoryDB>(db);
                                      Box::into_raw(Box::new(db.fork())) as jlong
                                  });
    utils::unwrap_or_exception(env, res)
}

/// Destroys underlying `Fork` object and frees memory.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_storage_DB_MemoryDB_nativeFreeFork(env: JNIEnv,
                                                                     _: JClass,
                                                                     db: jlong) {
    utils::drop_object::<Fork>(env, db);
}
