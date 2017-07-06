use jni::JNIEnv;
use jni::objects::{JClass, JString};

use std::panic;

use exonum::storage::{LevelDB, Database};
use utils::{self, Handle};
use super::db::View;

/// Returns pointer to created `LevelDB` object.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_db_LevelDb_nativeCreate(
    env: JNIEnv,
    _: JClass,
    _path: JString,
) -> Handle {
    let res = panic::catch_unwind(|| {
        unimplemented!()
        // TODO: `leveldb::options::Options` should be reexported.
        // TODO: Pass open options.
        // let path = env.get_string(path).expect("Couldn't get java string!").into();
        // utils::to_handle(LevelDB::open(path)))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Destroys underlying `LevelDB` object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_db_LevelDb_nativeFree(
    env: JNIEnv,
    _: JClass,
    db_handle: Handle,
) {
    utils::drop_handle::<LevelDB>(&env, db_handle);
}

/// Returns pointer to created `Snapshot` object.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_db_LevelDb_nativeLookupSnapshot(
    env: JNIEnv,
    _: JClass,
    db_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let db = utils::cast_handle::<LevelDB>(db_handle);
        utils::to_handle(View::Snapshot(db.snapshot()))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns pointer to created `Fork` object.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_db_LevelDb_nativeLookupFork(
    env: JNIEnv,
    _: JClass,
    db_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let db = utils::cast_handle::<LevelDB>(db_handle);
        utils::to_handle(View::Fork(db.fork()))
    });
    utils::unwrap_exc_or_default(&env, res)
}
