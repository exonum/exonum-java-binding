use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jlong, jboolean};

use std::panic;

use exonum::storage2::{LevelDB, Database, Snapshot, Fork};
use utils;

/// Returns pointer to created `LevelDB` object or zero if panic occurs.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_storage_DB_LevelDB_nativeCreateLevelDB(_env: JNIEnv,
                                                                         _: JClass,
                                                                         _path: JString)
                                                                         -> jlong {
    panic::catch_unwind(|| {
        unimplemented!()
        // TODO: `leveldb::options::Options` should be reexported.
        // TODO: Pass open options.
        // let path = env.get_string(path).expect("Couldn't get java string!").into();
        // Box::into_raw(Box::new(LevelDB::open(path))) as jlong
    })
            .unwrap_or_default()
}

/// Destroys underlying `LevelDB` object and frees memory. Return `false` if panic occurs.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_storage_DB_LevelDB_nativeFreeLevelDB(_: JNIEnv,
                                                                       _: JClass,
                                                                       db: jlong)
                                                                       -> jboolean {
    utils::drop_object::<LevelDB>(db)
}

/// Returns pointer to created `Snapshot` object or zero if panic occurs.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_storage_DB_LevelDB_nativeLookupSnapshot(_: JNIEnv,
                                                                          _: JClass,
                                                                          db: jlong)
                                                                          -> jlong {
    let res = panic::catch_unwind(|| {
        let db = utils::cast_object::<LevelDB>(db);
        // Additional boxing here is needed because "trait object" is represented by two pointers
        // and cannot be cast into `jlong`.
        Box::into_raw(Box::new(db.snapshot())) as jlong
    });
    res.unwrap_or_default()
}

/// Destroys underlying `Snapshot` object and frees memory. Return `false` if panic occurs.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_storage_DB_LevelDB_nativeFreeSnapshot(_: JNIEnv,
                                                                        _: JClass,
                                                                        db: jlong)
                                                                        -> jboolean {
    utils::drop_object::<Box<Snapshot>>(db)
}

/// Returns pointer to created `Fork` object or zero if panic occurs.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_storage_DB_LevelDB_nativeLookupFork(_: JNIEnv,
                                                                      _: JClass,
                                                                      db: jlong)
                                                                      -> jlong {
    panic::catch_unwind(|| {
                            let db = utils::cast_object::<LevelDB>(db);
                            Box::into_raw(Box::new(db.fork())) as jlong
                        })
            .unwrap_or_default()
}

/// Destroys underlying `Fork` object and frees memory. Return `false` if panic occurs.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_storage_DB_LevelDB_nativeFreeFork(_: JNIEnv,
                                                                    _: JClass,
                                                                    db: jlong)
                                                                    -> jboolean {
    utils::drop_object::<Fork>(db)
}
