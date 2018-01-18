use jni::JNIEnv;
use jni::objects::JClass;
use exonum::storage::{Snapshot, Fork};

use std::collections::HashMap;
use std::sync::RwLock;

use utils::{self, Handle};

pub(crate) type Key = Vec<u8>;
pub(crate) type Value = Vec<u8>;

// Raw pointer to the `View` is returned to the java side, so in rust functions that take back
// `Snapshot` or`Fork` it will be possible to distinguish them.
pub(crate) enum View {
    Snapshot(&'static Snapshot),
    Fork(&'static mut Fork),
}

lazy_static! {
    // Raw pointers are used as identifiers
    static ref VIEW_MAP: RwLock<HashMap<usize, Handle>> = RwLock::new(HashMap::new());
}

struct Pair(usize, usize);

impl View {
    pub fn from_owned_snapshot(snapshot: Box<Snapshot>) -> Self {
        // Clone a fat reference
        let snapshot_ref = unsafe { ::std::mem::transmute(&*snapshot) };
        let view = View::Snapshot(snapshot_ref);
        let handle = utils::to_handle::<Box<Snapshot>>(snapshot);
        view.save_handle(handle);
        view
    }

    pub fn from_owned_fork(fork: Fork) -> Self {
        let handle = utils::to_handle::<Fork>(fork);
        let fork = unsafe { ::std::mem::transmute_copy(&handle) };
        let view = View::Fork(fork);
        view.save_handle(handle);
        view
    }

    #[cfg_attr(feature = "cargo-clippy", allow(needless_borrow))]
    fn drop_owned(env: &JNIEnv, view: &View) {
        if let Some(handle) = view.remove_handle() {
            match *view {
                View::Snapshot(_) => utils::drop_handle::<Box<Snapshot>>(env, handle),
                View::Fork(_) => utils::drop_handle::<Fork>(env, handle),
            };
        }
    }

    #[cfg_attr(feature = "cargo-clippy", allow(needless_borrow))]
    fn id(&self) -> usize {
        match *self {
            View::Snapshot(ref snapshot) => {
                unsafe { ::std::mem::transmute_copy::<_, Pair>(snapshot) }.0
            }
            View::Fork(ref fork) => unsafe { ::std::mem::transmute_copy(fork) },
        }
    }

    fn save_handle(&self, handle: Handle) {
        let id = self.id();
        assert_ne!(id, 0);
        assert!(
            VIEW_MAP
                .write()
                .expect("Unable to obtain write-lock")
                .insert(id, handle)
                .is_none(),
            "Trying to register the same id for the second time: {:X}",
            id
        );
    }

    fn remove_handle(&self) -> Option<Handle> {
        let id = self.id();
        VIEW_MAP
            .write()
            .expect("Unable to obtain write-lock")
            .remove(&id)
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
