use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::{jlong, jboolean};

use std::panic;

use exonum::storage2::{MemoryDB, Database, Snapshot, Fork};
use utils;

/// Returns pointer to created `MemoryDB` object or zero if panic occurs.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_storage_DB_MemoryDB_nativeCreateMemoryDB(_: JNIEnv,
                                                                           _: JClass)
                                                                           -> jlong {
    panic::catch_unwind(|| Box::into_raw(Box::new(MemoryDB::new())) as jlong).unwrap_or_default()
}

/// Destroys underlying `MemoryDB` object and frees memory. Return `false` if panic occurs.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_storage_DB_MemoryDB_nativeFreeMemoryDB(_: JNIEnv,
                                                                         _: JClass,
                                                                         db: jlong)
                                                                         -> jboolean {
    utils::drop_object::<MemoryDB>(db)
}

/// Returns pointer to created `Snapshot` object or zero if panic occurs.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_storage_DB_MemoryDB_nativeLookupSnapshot(_: JNIEnv,
                                                                           _: JClass,
                                                                           db: jlong)
                                                                           -> jlong {
    panic::catch_unwind(|| {
        let db = utils::cast_object::<MemoryDB>(db);
        // Additional boxing here is needed because "trait object" is represented by two pointers
        // and cannot be cast into `jlong`.
        Box::into_raw(Box::new(db.snapshot())) as jlong
    })
            .unwrap_or_default()
}

/// Destroys underlying `Snapshot` object and frees memory. Return `false` if panic occurs.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_storage_DB_MemoryDB_nativeFreeSnapshot(_: JNIEnv,
                                                                         _: JClass,
                                                                         db: jlong)
                                                                         -> jboolean {
    utils::drop_object::<Box<Snapshot>>(db)
}

/// Returns pointer to created `Fork` object or zero if panic occurs.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_storage_DB_MemoryDB_nativeLookupFork(_: JNIEnv,
                                                                       _: JClass,
                                                                       db: jlong)
                                                                       -> jlong {
    panic::catch_unwind(|| {
                            let db = utils::cast_object::<MemoryDB>(db);
                            Box::into_raw(Box::new(db.fork())) as jlong
                        })
            .unwrap_or_default()
}

/// Destroys underlying `Fork` object and frees memory. Return `false` if panic occurs.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_storage_DB_MemoryDB_nativeFreeFork(_: JNIEnv,
                                                                     _: JClass,
                                                                     db: jlong)
                                                                     -> jboolean {
    utils::drop_object::<Fork>(db)
}
