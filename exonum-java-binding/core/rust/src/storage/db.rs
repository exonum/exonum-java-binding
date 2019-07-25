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

use handle::{self, Handle};

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

pub(crate) struct View {
    // The `owned` field is used, but its value only needed for the drop stage,
    // so `Box<Fork>`/`Box<Snapshot>` will be dropped when an instance of `View` leaves the scope.
    _owned: Option<ViewOwned>,
    reference: ViewRef,
}

enum ViewOwned {
    Snapshot(Box<Snapshot>),
    Fork(Box<Fork>),
}

pub(crate) enum ViewRef {
    Snapshot(&'static Snapshot),
    Fork(&'static Fork),
}

impl View {
    pub fn from_owned_snapshot(snapshot: Box<Snapshot>) -> Self {
        View {
            // Make a "self-reference" to a value stored in the `owned` field.
            reference: unsafe { ViewRef::from_snapshot(&*snapshot) },
            _owned: Some(ViewOwned::Snapshot(snapshot)),
        }
    }

    pub fn from_owned_fork(fork: Fork) -> Self {
        // Box a `Fork` value to make sure it will not be moved later
        // and will not break the `reference` field.
        let fork = Box::new(fork);
        View {
            // Make a "self-reference" to a value stored in the `owned` field.
            reference: unsafe { ViewRef::from_fork(&*fork) },
            _owned: Some(ViewOwned::Fork(fork)),
        }
    }

    // Will be used in #ECR-242
    #[allow(dead_code)]
    pub fn from_ref_snapshot(snapshot: &Snapshot) -> Self {
        View {
            reference: unsafe { ViewRef::from_snapshot(snapshot) },
            _owned: None,
        }
    }

    // Will be used in #ECR-242
    #[allow(dead_code)]
    pub fn from_ref_fork(fork: &Fork) -> Self {
        View {
            reference: unsafe { ViewRef::from_fork(fork) },
            _owned: None,
        }
    }

    pub fn get(&mut self) -> &mut ViewRef {
        &mut self.reference
    }

    /// Unwraps the stored Fork from the View, panics if it's not possible.
    pub fn into_fork(self) -> Box<Fork> {
        if let Some(view_owned) = self._owned {
            match view_owned {
                ViewOwned::Snapshot(_) => panic!("`into_fork` called on Snapshot"),
                ViewOwned::Fork(fork) => fork,
            }
        } else {
            panic!("`into_fork` called on non-owning View");
        }
    }

    /// Returns `true` iff `into_fork` conversion is possible.
    pub fn can_convert_into_fork(&self) -> bool {
        match self._owned {
            None => false,
            Some(ViewOwned::Snapshot(_)) => false,
            Some(ViewOwned::Fork(_)) => true,
        }
    }
}

impl ViewRef {
    unsafe fn from_fork(fork: &Fork) -> Self {
        // Make a provided reference `'static`.
        ViewRef::Fork(&*(fork as *const Fork))
    }

    unsafe fn from_snapshot(snapshot: &Snapshot) -> Self {
        // Make a provided reference `'static`.
        ViewRef::Snapshot(&*(snapshot as *const Snapshot))
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

    const TEST_VALUE: i32 = 42;

    #[test]
    fn create_view_with_owned_fork() {
        let db = setup_database();
        let fork = db.fork();
        let mut view = View::from_owned_fork(fork);
        check_ref_fork(&mut view);
        check_owned_ref(&mut view);
    }

    #[test]
    fn create_view_with_owned_snapshot() {
        let db = setup_database();
        let snapshot = db.snapshot();
        let mut view = View::from_owned_snapshot(snapshot);
        check_ref_snapshot(&mut view);
        check_owned_ref(&mut view);
    }

    #[test]
    fn create_view_with_ref_fork() {
        let db = setup_database();
        let fork = db.fork();
        let mut view = View::from_ref_fork(&fork);
        check_ref_fork(&mut view);
        assert!(view._owned.is_none());
    }

    #[test]
    fn create_view_with_ref_snapshot() {
        let db = setup_database();
        let snapshot = db.snapshot();
        let mut view = View::from_ref_snapshot(&*snapshot);
        check_ref_snapshot(&mut view);
        assert!(view._owned.is_none());
    }

    // Creates database with a prepared state.
    fn setup_database() -> TemporaryDB {
        let db = TemporaryDB::new();
        let fork = db.fork();
        entry(&fork).set(TEST_VALUE);
        db.merge(fork.into_patch()).unwrap();
        db
    }

    fn entry<T>(view: T) -> Entry<T, i32>
    where
        T: IndexAccess,
    {
        Entry::new("test", view)
    }

    fn check_ref_fork(view: &mut View) {
        match *view.get() {
            ViewRef::Fork(fork) => check_value(fork),
            _ => panic!("View::reference expected to be Fork"),
        }
    }

    fn check_ref_snapshot(view: &mut View) {
        match *view.get() {
            ViewRef::Snapshot(snapshot) => check_value(snapshot),
            _ => panic!("View::reference expected to be Snapshot"),
        }
    }

    fn check_value<T: IndexAccess>(view: T) {
        assert_eq!(Some(TEST_VALUE), entry(view).get())
    }

    fn check_owned_ref(view: &mut View) {
        #[derive(Eq, PartialEq, Debug)]
        enum Ptr {
            Fork(*const Fork),
            Snapshot(*const Snapshot),
        }

        let reference = match *view.get() {
            ViewRef::Fork(ref f) => Ptr::Fork(&**f),
            ViewRef::Snapshot(ref s) => Ptr::Snapshot(&**s),
        };

        let owned = match *view._owned.as_ref().unwrap() {
            ViewOwned::Fork(ref f) => Ptr::Fork(&**f),
            ViewOwned::Snapshot(ref s) => Ptr::Snapshot(&**s),
        };

        assert_eq!(owned, reference);
    }
}
