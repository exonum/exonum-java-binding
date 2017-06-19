use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jlong;

use std::panic;

use exonum::storage::{LevelDB, Database};
use utils;
use super::db::View;

/// Returns pointer to created `LevelDB` object.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_binding_storage_db_LevelDb_nativeCreateLevelDb(
    env: JNIEnv,
    _: JClass,
    _path: JString,
) -> jlong {
    let res = panic::catch_unwind(|| {
        unimplemented!()
        // TODO: `leveldb::options::Options` should be reexported.
        // TODO: Pass open options.
        // let path = env.get_string(path).expect("Couldn't get java string!").into();
        // Box::into_raw(Box::new(LevelDB::open(path))) as jlong
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Destroys underlying `LevelDB` object and frees memory.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_binding_storage_db_LevelDb_nativeFreeLevelDb(
    env: JNIEnv,
    _: JClass,
    db: jlong,
) {
    utils::drop_object::<LevelDB>(&env, db);
}

/// Returns pointer to created `Snapshot` object.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_binding_storage_db_LevelDb_nativeLookupSnapshot(
    env: JNIEnv,
    _: JClass,
    db: jlong,
) -> jlong {
    let res = panic::catch_unwind(|| {
        let db = utils::cast_object::<LevelDB>(db);
        Box::into_raw(Box::new(View::Snapshot(db.snapshot()))) as jlong
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns pointer to created `Fork` object.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_binding_storage_db_LevelDb_nativeLookupFork(
    env: JNIEnv,
    _: JClass,
    db: jlong,
) -> jlong {
    let res = panic::catch_unwind(|| {
        let db = utils::cast_object::<LevelDB>(db);
        Box::into_raw(Box::new(View::Fork(db.fork()))) as jlong
    });
    utils::unwrap_exc_or_default(&env, res)
}
