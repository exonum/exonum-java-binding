use jni::JNIEnv;
use jni::objects::JClass;
use exonum::storage::{Snapshot, Fork};

use utils::{self, Handle};

pub(crate) type Key = Vec<u8>;
pub(crate) type Value = Vec<u8>;


/// A `View` is a wrapper for `Snapshot` or `Fork`, which makes it possible to distinguish them
/// on the rust side, and transfer them as a raw pointer via the java side.
///
/// Additionally, the `View` type is used in two scenarios now:
/// - it holds owned value of `Fork` or `Snapshot` and destroys it in the end,
///    in the case when the java side creates and owns it;
/// - it just holds a reference, when one is provided from the rust side.
///
/// For both scenarios there is only a reference needed for underlying storage indexes, so
/// an optional `owned` part is private.
///
/// For storage API we need a reference, so we create it to the owned part. But since there is no
/// way in Rust to make a `View` value not movable. Furthermore, it have to be moved from the stack
/// to the heap in order to be converted into `Handle` for the java side. So a `Fork` value
/// should be placed in the heap to prevent its movement after creating a reference to it.

pub(crate) struct View {
    // The `owned` field is used, it is not a fake, but its value only needed for the drop stage,
    // so `Box<Fork>`/`Box<Snapshot>` will be dropped when an instance of `View` leaves the scope.
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
        // Make a "self-reference" to a value in the `owned` field
        let snapshot_ref = unsafe { &*(&*snapshot as *const Snapshot) };
        let reference = ViewRef::Snapshot(snapshot_ref);
        let owned = Some(ViewOwned::Snapshot(snapshot));
        View { owned, reference }
    }

    pub fn from_owned_fork(fork: Fork) -> Self {
        // Box a `Fork` value to make sure it will not be moved later and will not break a reference
        let mut fork = Box::new(fork);
        // Make a "self-reference" to a value in the `owned` field
        let fork_ref = unsafe { &mut *(&mut *fork as *mut Fork) };
        let reference = ViewRef::Fork(fork_ref);
        let owned = Some(ViewOwned::Fork(fork));
        View { owned, reference }
    }

    // Will be removed in #ECR-242
    #[allow(dead_code)]
    pub fn from_ref_snapshot(snapshot: &Snapshot) -> Self {
        // Make a provided reference `'static`
        let snapshot_ref = unsafe { &*(&*snapshot as *const Snapshot) };
        View {
            owned: None,
            reference: ViewRef::Snapshot(snapshot_ref),
        }
    }

    // Will be removed in #ECR-242
    #[allow(dead_code)]
    pub fn from_ref_fork(fork: &mut Fork) -> Self {
        // Make a provided reference `'static`
        let fork_ref = unsafe { &mut *(fork as *mut Fork) };
        View {
            owned: None,
            reference: ViewRef::Fork(fork_ref),
        }
    }

    pub fn get(&mut self) -> &mut ViewRef {
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
    use exonum::storage::{Database, Entry, MemoryDB};
    use super::*;

    const TEST_VALUE: i32 = 42;

    // Creates database with a prepared state.
    fn setup_database() -> MemoryDB {
        let mut db = MemoryDB::new();
        let mut fork = db.fork();
        entry(&mut fork).set(TEST_VALUE);
        let patch = fork.into_patch();
        db.merge(patch).unwrap();
        db
    }

    fn entry<T>(t: T) -> Entry<T, i32> {
        Entry::new("test", t)
    }

    #[test]
    fn create_view_with_owned_fork() {
        let db = setup_database();
        let fork = db.fork();
        let mut view = View::from_owned_fork(fork);
        match *view.get() {
            ViewRef::Fork(ref mut fork) => assert_eq!(Some(TEST_VALUE), entry(fork).get()),
            _ => unreachable!(),
        }
        match view.owned {
            Some(ViewOwned::Fork(mut fork)) => {
                assert_eq!(Some(TEST_VALUE), entry(&mut *fork).get())
            }
            _ => unreachable!(),
        }
    }

    #[test]
    fn create_view_with_owned_snapshot() {
        let db = setup_database();
        let snapshot = db.snapshot();
        let mut view = View::from_owned_snapshot(snapshot);
        match *view.get() {
            ViewRef::Snapshot(snapshot) => assert_eq!(Some(TEST_VALUE), entry(snapshot).get()),
            _ => unreachable!(),
        }
        match view.owned {
            Some(ViewOwned::Snapshot(snapshot)) => {
                assert_eq!(Some(TEST_VALUE), entry(snapshot).get())
            }
            _ => unreachable!(),
        }
    }

    #[test]
    fn create_view_with_ref_fork() {
        let db = setup_database();
        let mut fork = db.fork();
        let mut view = View::from_ref_fork(&mut fork);
        match *view.get() {
            ViewRef::Fork(ref mut fork) => assert_eq!(Some(TEST_VALUE), entry(fork).get()),
            _ => unreachable!(),
        }
        assert!(view.owned.is_none());
    }

    #[test]
    fn create_view_with_ref_snapshot() {
        let db = setup_database();
        let snapshot = db.snapshot();
        let mut view = View::from_ref_snapshot(&*snapshot);
        match *view.get() {
            ViewRef::Snapshot(snapshot) => assert_eq!(Some(TEST_VALUE), entry(snapshot).get()),
            _ => unreachable!(),
        }
        assert!(view.owned.is_none());
    }
}
