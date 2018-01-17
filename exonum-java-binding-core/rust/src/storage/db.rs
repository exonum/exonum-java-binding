use jni::JNIEnv;
use jni::objects::JClass;
use exonum::storage::{Snapshot, Fork};

use utils::{self, Handle};

pub(crate) type Key = Vec<u8>;
pub(crate) type Value = Vec<u8>;

// Raw pointer to the `View` is returned to the java side, so in rust functions that take back
// `Snapshot` or`Fork` it will be possible to distinguish them.
pub(crate) enum View {
    Snapshot(&'static Snapshot),
    Fork(&'static mut Fork),
}

struct Pair (Handle, usize);

impl View {
    pub fn from_owned_snapshot(snapshot: Box<Snapshot>) -> Self {
        let snapshot_ref = unsafe { ::std::mem::transmute(&*snapshot) };
        let handle = utils::to_handle::<Box<Snapshot>>(snapshot);
        let handle_2 = unsafe { ::std::mem::transmute_copy::<_, Pair>(&snapshot_ref) }.0;
        assert_eq!(handle, handle_2);
        View::Snapshot(snapshot_ref)
    }

    pub fn from_owned_fork(fork: Fork) -> Self {
        let fork = utils::to_handle::<Fork>(fork);
        let fork = unsafe { ::std::mem::transmute(fork) };
        View::Fork(fork)
    }

    fn drop_owned(env: &JNIEnv, view: &View) {
        match *view {
            View::Snapshot(ref pair) => {
                let handle = unsafe { ::std::mem::transmute_copy::<_, Pair>(pair) }.0;
                if utils::has_handle(handle) {
                    utils::drop_handle::<Box<Snapshot>>(env, handle);
                }
            }
            View::Fork(ref handle) => {
                let handle = unsafe { ::std::mem::transmute_copy(handle) };
                if utils::has_handle(handle) {
                    utils::drop_handle::<Fork>(env, handle);
                }
            }
        };
    }
}

/// Destroys underlying `Snapshot` or `Fork` object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_storage_database_Views_nativeFree(
    env: JNIEnv,
    _: JClass,
    view_handle: Handle,
) {
    View::drop_owned(&env, utils::cast_handle(view_handle));
    utils::drop_handle::<View>(&env, view_handle);
}
