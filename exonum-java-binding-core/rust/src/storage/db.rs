use jni::JNIEnv;
use jni::objects::JClass;
use exonum::storage::{Snapshot, Fork};

use utils::{self, Handle};

pub(crate) type Key = Vec<u8>;
pub(crate) type Value = Vec<u8>;

/// Raw pointer to the `View` is returned to the java side, so in rust functions that take back
/// `Snapshot` or `Fork` it will be possible to distinguish them.
///
/// Why there is a separate reference?
/// Full `Box<Snapshot>`/`Box<Fork>` type may be saved as owned value to be able drop it in the end.
/// Short `&Snapshot`/`&Fork` type is provided by the core and we have to make `ViewRef` compatible.
///
/// Why `Fork` is boxed?
/// This is needed to make sure that it will not be moved after creating a reference.
/// `View` itself will be moved from the stack to the heap in order to be converted in `Handle`.

pub(crate) struct View {
    // The `owned` field is only needed for the drop stage,
    // so `Box<Fork/Snapshot>` will be dropped when an instance of `View` will leave the scope.
    #[allow(dead_code)]
    owned: Option<ViewOwned>,
    reference: ViewRef,
}

enum ViewOwned {
    Snapshot(Box<Snapshot>),
    Fork(Box<Fork>),
}

pub(crate) enum ViewRef {
    Snapshot(&'static Snapshot),
    Fork(&'static mut Fork),
}

impl View {
    pub fn from_owned_snapshot(snapshot: Box<Snapshot>) -> Self {
        // Make a reference to an owned data
        let snapshot_ref = unsafe { &*(&*snapshot as *const Snapshot) };
        let reference = ViewRef::Snapshot(snapshot_ref);
        let owned = Some(ViewOwned::Snapshot(snapshot));
        View { owned, reference }
    }

    pub fn from_owned_fork(fork: Fork) -> Self {
        // Box `Fork` to make sure it will not be moved later
        let mut fork = Box::new(fork);
        // Make a reference to an owned data
        let fork_ref = unsafe { &mut *(&mut *fork as *mut Fork) };
        let reference = ViewRef::Fork(fork_ref);
        let owned = Some(ViewOwned::Fork(fork));
        View { owned, reference }
    }

    pub fn from_ref_snapshot(snapshot: &Snapshot) -> Self {
        // Make a reference `'static`
        let snapshot_ref = unsafe { &*(&*snapshot as *const Snapshot) };
        View {
            owned: None,
            reference: ViewRef::Snapshot(snapshot_ref),
        }
    }

    pub fn from_ref_fork(fork: &mut Fork) -> Self {
        // Make a reference `'static`
        let fork_ref = unsafe { &mut *(fork as *mut Fork) };
        View {
            owned: None,
            reference: ViewRef::Fork(fork_ref),
        }
    }

    pub fn view_ref(&mut self) -> &mut ViewRef {
        &mut self.reference
    }
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


#[cfg(test)]
mod tests {
    use exonum::storage::{MemoryDB, Database};
    use super::*;

    #[test]
    fn create_view_owned_fork() {
        let db = MemoryDB::new();
        let fork = db.fork();
        let view = View::from_owned_fork(fork);
        assert!(view.owned.is_some());
    }

    #[test]
    fn create_view_owned_snapshot() {
        let db = MemoryDB::new();
        let snapshot = db.snapshot();
        let view = View::from_owned_snapshot(snapshot);
        assert!(view.owned.is_some());
    }

    #[test]
    fn create_view_ref_fork() {
        let db = MemoryDB::new();
        let mut fork = db.fork();
        let view = View::from_ref_fork(&mut fork);
        assert!(view.owned.is_none());
    }

    #[test]
    fn create_view_ref_snapshot() {
        let db = MemoryDB::new();
        let snapshot = db.snapshot();
        let view = View::from_ref_snapshot(&*snapshot);
        assert!(view.owned.is_none());
    }
}
