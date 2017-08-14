use jni::JNIEnv;
use jni::objects::JClass;

use exonum::storage::{Snapshot, Fork};
use utils::{self, Handle};

pub(crate) type Key = Vec<u8>;
pub(crate) type Value = Vec<u8>;

// Raw pointer to the `View` is returned to the java side, so in rust functions that take back
// `Snapshot` or`Fork` it will be possible to distinguish them.
pub(crate) enum View {
    Snapshot(Box<Snapshot>),
    Fork(Fork),
}

/// Destroys underlying `Snapshot` or `Fork` object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_database_Views_nativeFree(
    env: JNIEnv,
    _: JClass,
    view_handle: Handle,
) {
    utils::drop_handle::<View>(&env, view_handle);
}
