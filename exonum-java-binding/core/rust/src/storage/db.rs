// Copyright 2018 The Exonum Team
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

use exonum_merkledb::{Fork, Snapshot};
use jni::{objects::JClass, JNIEnv};

use cast_handle;
use handle::{self, Handle, NonOwnedHandle};

pub(crate) type Key = Vec<u8>;
pub(crate) type Value = Vec<u8>;

/// A `View` is a wrapper for `Snapshot` or `Fork`, which makes it possible to distinguish them
/// on the rust side, and transfer them as a raw pointer to the java side.
///
/// The `View` type is used in two scenarios:
/// - it holds owned value of `Fork` or `Snapshot` and destroys it in the end,
///    in the case when the java side creates and owns it;
/// - it just holds a reference, when one is provided from the rust side.
///
/// For storage API we need a reference, so we create it to the owned part. But since there is no
/// way in Rust to make a `View` value not movable. Furthermore, it have to be moved from the stack
/// to the heap in order to be converted into `Handle` for the java side. So a `Fork` value
/// should be placed in the heap to prevent its movement after creating a reference to it.

pub(crate) enum View {
    /// TODO
    RefFork(NonOwnedHandle<Fork>),
    /// TODO
    RefSnapshot(&'static dyn Snapshot),
    /// TODO
    Owned(ViewOwned),
}

pub(crate) enum ViewOwned {
    Snapshot(Box<dyn Snapshot>),
    Fork(Box<Fork>),
}

#[derive(Clone)]
pub(crate) enum ViewRef {
    Snapshot(&'static dyn Snapshot),
    Fork(&'static Fork),
}

impl View {
    pub fn from_owned_snapshot(snapshot: Box<dyn Snapshot>) -> Self {
        View::Owned(ViewOwned::Snapshot(snapshot))
    }

    pub fn from_owned_fork(fork: Fork) -> Self {
        View::Owned(ViewOwned::Fork(Box::new(fork)))
    }

    pub fn from_ref_snapshot(snapshot: &dyn Snapshot) -> Self {
        View::RefSnapshot(unsafe { std::mem::transmute(snapshot) })
    }

    pub fn from_ref_fork(fork: &Fork) -> Self {
        View::RefFork(NonOwnedHandle::new(fork))
    }

    pub fn from_ref_mut_fork(fork: &mut Fork) -> Self {
        View::RefFork(NonOwnedHandle::new_mut(fork))
    }

    pub fn get(&self) -> ViewRef {
        match self {
            View::RefFork(handle) => {
                let fork = cast_handle::<Fork>(handle.as_handle());
                ViewRef::Fork(fork)
            }
            View::RefSnapshot(snapshot_ref) => {
                ViewRef::Snapshot(*snapshot_ref)
            },
            View::Owned(owned) => match owned {
                ViewOwned::Fork(fork) => unsafe { ViewRef::from_fork(&fork) },
                ViewOwned::Snapshot(snapshot) => unsafe { ViewRef::from_snapshot(&**snapshot) },
            },
        }
    }

    /// Unwraps the stored Fork from the View, panics if it's not possible.
    pub fn into_fork(self) -> Box<Fork> {
        if let View::Owned(ViewOwned::Fork(fork)) = self {
            fork
        } else {
            panic!("`into_fork` called on non-owning View or Snapshot");
        }
    }

    /// Returns `true` iff `into_fork` conversion is possible.
    pub fn can_convert_into_fork(&self) -> bool {
        match self {
            View::Owned(ViewOwned::Fork(_)) => true,
            _ => false,
        }
    }
}

impl ViewRef {
    unsafe fn from_fork(fork: &Fork) -> Self {
        // Make a provided reference `'static`.
        ViewRef::Fork(&*(fork as *const Fork))
    }

    unsafe fn from_snapshot(snapshot: &dyn Snapshot) -> Self {
        // Make a provided reference `'static`.
        ViewRef::Snapshot(&*(snapshot as *const dyn Snapshot))
    }
}

/// Destroys underlying `Snapshot` or `Fork` object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_database_Views_nativeFree(
    env: JNIEnv,
    _: JClass,
    view_handle: Handle,
) {
    handle::drop_handle::<View>(&env, view_handle);
}

#[cfg(test)]
mod tests {
    use super::*;
    use exonum_merkledb::{Database, Entry, IndexAccess, TemporaryDB};
    use to_handle;

    const TEST_VALUE: i32 = 42;

    #[test]
    fn snapshot_ref_view() {
        let db = setup_database();
        let snapshot = db.snapshot();
        let view = View::from_ref_snapshot(&*snapshot);
        check_snapshot(view.get())
    }

    #[test]
    fn snapshot_owned_view() {
        let db = setup_database();
        let snapshot = db.snapshot();
        let view = View::from_owned_snapshot(snapshot);
        check_snapshot(view.get())
    }

    #[test]
    fn fork_ref_view() {
        let db = setup_database();
        let fork = db.fork();
        let view = View::from_ref_fork(&fork);
        check_fork(view.get())
    }

    #[test]
    fn fork_owned_view() {
        let db = setup_database();
        let fork = db.fork();
        let view = View::from_owned_fork(fork);
        check_fork(view.get())
    }

    #[test]
    fn convert_fork_into_fork() {
        let db = TemporaryDB::new();
        let fork = db.fork();
        {
            let view = View::from_ref_fork(&fork);
            assert!(!view.can_convert_into_fork());
        }
        let view = View::from_owned_fork(fork);
        assert!(view.can_convert_into_fork());
        let _patch = view.into_fork().into_patch();
    }

    #[test]
    fn convert_snapshot_into_fork_forbidden() {
        let db = TemporaryDB::new();
        {
            let snapshot = db.snapshot();
            let view = View::from_owned_snapshot(snapshot);
            assert!(!view.can_convert_into_fork());
        }
        {
            let snapshot = db.snapshot();
            let view = View::from_ref_snapshot(&*snapshot);
            assert!(!view.can_convert_into_fork());
        }
    }

    #[test]
    fn get_mut_fork() {
        let db = TemporaryDB::new();
        let mut fork = db.fork();
        let view = View::from_ref_mut_fork(&mut fork);
        let mock_method = |_: &mut Fork| {};
        match view {
            View::RefFork(mut handle) => {
                let fork = handle.get_mut();
                mock_method(fork);
            }
            _ => panic!(),
        }
    }

    #[test]
    #[should_panic(expected = "Attempt to access mutable reference from immutable")]
    fn mutable_fork_from_immutable_throws_error() {
        let db = TemporaryDB::new();
        let fork = db.fork();
        // Simulate handles transfer to-from Java
        let handle = {
            let view = View::from_ref_fork(&fork);
            to_handle(view)
        };
        let view = cast_handle::<View>(handle);
        if let View::RefFork(fork_ref) = view {
            let _ = fork_ref.get_mut();
        } else {
            panic!()
        }
    }

    fn check_snapshot(view_ref: ViewRef) {
        match view_ref {
            ViewRef::Snapshot(snapshot) => check_value(snapshot, TEST_VALUE),
            _ => panic!(),
        }
    }

    fn check_fork(view_ref: ViewRef) {
        match view_ref {
            ViewRef::Fork(fork) => {
                check_value(fork, TEST_VALUE);
                {
                    let mut index = entry(fork);
                    index.set(0);
                }
                // Recreate index because changes might not be included in fork
                {
                    let index = entry(fork);
                    assert_eq!(Some(0), index.get())
                }
            }
            _ => panic!(),
        }
    }

    // Creates database with a prepared state.
    fn setup_database() -> TemporaryDB {
        let db = TemporaryDB::new();
        let fork = db.fork();
        entry(&fork).set(TEST_VALUE);
        db.merge(fork.into_patch()).unwrap();
        db
    }

    fn check_value<T: IndexAccess>(view: T, value: i32) {
        assert_eq!(Some(value), entry(view).get())
    }

    fn entry<T>(view: T) -> Entry<T, i32>
    where
        T: IndexAccess,
    {
        Entry::new("test", view)
    }
}
