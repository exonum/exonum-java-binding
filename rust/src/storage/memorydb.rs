use jni::JNIEnv;
use jni::objects::{JClass, JObject};

use std::panic;

use exonum::storage::{MemoryDB, Database};
use utils::{self, Handle};
use super::db::View;

/// Returns pointer to created `MemoryDB` object.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_proxy_MemoryDb_nativeCreate(
    env: JNIEnv,
    _: JClass,
) -> Handle {
    let res = panic::catch_unwind(|| utils::to_handle(MemoryDB::new()));
    utils::unwrap_exc_or_default(&env, res)
}

/// Destroys underlying `MemoryDB` object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_proxy_MemoryDb_nativeFree(
    env: JNIEnv,
    _: JObject,
    db_handle: Handle,
) {
    utils::drop_handle::<MemoryDB>(&env, db_handle);
}

/// Returns pointer to created `Snapshot` object.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_proxy_MemoryDb_nativeCreateSnapshot(
    env: JNIEnv,
    _: JObject,
    db_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let db = utils::cast_handle::<MemoryDB>(db_handle);
        utils::to_handle(View::Snapshot(db.snapshot()))
    });
    utils::unwrap_exc_or_default(&env, res)
}

/// Returns pointer to created `Fork` object.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_proxy_MemoryDb_nativeCreateFork(
    env: JNIEnv,
    _: JObject,
    db_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let db = utils::cast_handle::<MemoryDB>(db_handle);
        utils::to_handle(View::Fork(db.fork()))
    });
    utils::unwrap_exc_or_default(&env, res)
}
