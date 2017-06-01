use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jlong;

use std::panic;

use exonum::storage2::{LevelDB, Database, Snapshot, Fork};
use utils;

/// Returns pointer to created `LevelDB` object.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_storage_DB_LevelDB_nativeCreateLevelDB(env: JNIEnv,
                                                                         _: JClass,
                                                                         _path: JString)
                                                                         -> jlong {
    let res = panic::catch_unwind(|| {
        unimplemented!()
        // TODO: `leveldb::options::Options` should be reexported.
        // TODO: Pass open options.
        // let path = env.get_string(path).expect("Couldn't get java string!").into();
        // Box::into_raw(Box::new(LevelDB::open(path))) as jlong
    });
    utils::unwrap_or_exception(env, res)
}

/// Destroys underlying `LevelDB` object and frees memory.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_storage_DB_LevelDB_nativeFreeLevelDB(env: JNIEnv,
                                                                       _: JClass,
                                                                       db: jlong) {
    utils::drop_object::<LevelDB>(env, db);
}

/// Returns pointer to created `Snapshot` object.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_storage_DB_LevelDB_nativeLookupSnapshot(env: JNIEnv,
                                                                          _: JClass,
                                                                          db: jlong)
                                                                          -> jlong {
    let res = panic::catch_unwind(|| {
        let db = utils::cast_object::<LevelDB>(db);
        // Additional boxing here is needed because "trait object" is represented by two pointers
        // and cannot be cast into `jlong`.
        Box::into_raw(Box::new(db.snapshot())) as jlong
    });
    utils::unwrap_or_exception(env, res)
}

/// Destroys underlying `Snapshot` object and frees memory.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_storage_DB_LevelDB_nativeFreeSnapshot(env: JNIEnv,
                                                                        _: JClass,
                                                                        db: jlong) {
    utils::drop_object::<Box<Snapshot>>(env, db);
}

/// Returns pointer to created `Fork` object.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_storage_DB_LevelDB_nativeLookupFork(env: JNIEnv,
                                                                      _: JClass,
                                                                      db: jlong)
                                                                      -> jlong {
    let res = panic::catch_unwind(|| {
                                      let db = utils::cast_object::<LevelDB>(db);
                                      Box::into_raw(Box::new(db.fork())) as jlong
                                  });
    utils::unwrap_or_exception(env, res)
}

/// Destroys underlying `Fork` object and frees memory.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_storage_DB_LevelDB_nativeFreeFork(env: JNIEnv,
                                                                    _: JClass,
                                                                    db: jlong) {
    utils::drop_object::<Fork>(env, db);
}
