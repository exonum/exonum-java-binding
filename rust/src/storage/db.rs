use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::jlong;

use exonum::storage::{Snapshot, Fork};
use utils;

// TODO: Temporary solution, should be replaced by the same typedef as `Value`.
pub type Key = u8;
pub type Value = Vec<u8>;

// Raw pointer to the `View` is returned to the java side, so in rust functions that take back
// `Snapshot` or`Fork` it will be possible to distinguish them.
pub enum View {
    Snapshot(Box<Snapshot>),
    Fork(Fork),
}

/// Destroys underlying `Snapshot` or `Fork` object and frees memory.
#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_exonum_binding_storage_connector_Views_nativeFree(
    env: JNIEnv,
    _: JClass,
    db: jlong,
) {
    utils::drop_object::<View>(&env, db);
}
