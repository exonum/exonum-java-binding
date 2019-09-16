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
/// As `View` does not have a lifetime, nothing protects us from dereferencing a freed reference,
/// the extreme caution must be taken in places where `View` is constructed and send to Java.
/// Java code must never store a `View` handle beyond the scope it was initially acquired
/// (except for `View::Owned`).
pub(crate) enum View {
    /// Special case for both `&Fork` and `&mut Fork`.
    ///
    /// Created `View` must never outlive the reference it was created with,
    /// or `SIGINT` will occur.
    RefFork(NonOwnedHandle<Fork>),
    /// As `Snapshot` is a trait-object, we can not use `NonOwnedHandle<Snapshot>` here.
    ///
    /// `View::from_ref_snapshot` makes any reference `&'static`, but the created `View`
    /// must never outlive the reference it was constructed with, or `SIGINT` will occur.
    RefSnapshot(&'static dyn Snapshot),
    /// Covers both `Snapshot` and `Fork` cases. Rust uses move semantic and single-ownership
    /// rule to guarantee that the `View` will be valid for the whole execution.
    ///
    /// This is the most safe `View` variant, no special care is needed when working with.
    Owned(ViewOwned),
}

pub(crate) enum ViewOwned {
    Snapshot(Box<dyn Snapshot>),
    Fork(Box<Fork>),
}

/// Hides the differences between owning and non-owning `View` variants
/// and simplifies the use of the indexes API.
#[derive(Clone)]
pub(crate) enum ViewRef<'a> {
    Snapshot(&'a dyn Snapshot),
    Fork(&'a Fork),
}

#[allow(clippy::useless_transmute)]
impl<'a> ViewRef<'a> {
    unsafe fn from_fork(fork: &'a Fork) -> Self {
        // Make a provided reference `'static`.
        ViewRef::Fork(std::mem::transmute(fork))
    }

    unsafe fn from_snapshot(snapshot: &'a dyn Snapshot) -> Self {
        // Make a provided reference `'static`.
        ViewRef::Snapshot(std::mem::transmute(snapshot))
    }
}

impl View {
    /// Creates `View::Owned(Snapshot)` variant. No special care needed.
    pub fn from_owned_snapshot(snapshot: Box<dyn Snapshot>) -> Self {
        View::Owned(ViewOwned::Snapshot(snapshot))
    }

    /// Creates `View::Owned(Fork)` variant. No special care needed.
    pub fn from_owned_fork(fork: Fork) -> Self {
        View::Owned(ViewOwned::Fork(Box::new(fork)))
    }

    /// Creates `View::RefSnapshot` variant.
    ///
    /// Created `View` must never outlive provided `snapshot` reference, or
    /// SIGINT will occur.
    pub fn from_ref_snapshot(snapshot: &dyn Snapshot) -> Self {
        View::RefSnapshot(unsafe { std::mem::transmute(snapshot) })
    }

    /// Creates `View::RefFork` variant.
    ///
    /// Created `View` must never outlive provided `fork` reference, or
    /// SIGINT will occur.
    ///
    /// Mutable indexes available, but not `&mut self` methods of `Fork`.
    pub fn from_ref_fork(fork: &Fork) -> Self {
        View::RefFork(NonOwnedHandle::new(fork))
    }

    /// Creates `View::RefFork` variant.
    ///
    /// Created `View` must never outlive provided `fork` reference, or
    /// SIGINT will occur.
    ///
    /// Both indexes mutability and `&mut self` methods of `Fork` available.
    // TODO: remove dead_code after ECR-3519
    #[allow(dead_code)]
    pub fn from_ref_mut_fork(fork: &mut Fork) -> Self {
        View::RefFork(NonOwnedHandle::new_mut(fork))
    }

    /// Returns temporary reference to the underlying `Fork` / `Snapshot` to simplify use
    /// in indexes operations.
    pub fn get(&self) -> ViewRef<'_> {
        match self {
            View::RefFork(handle) => ViewRef::Fork(handle.get()),
            View::RefSnapshot(snapshot_ref) => ViewRef::Snapshot(*snapshot_ref),
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
    use handle::{cast_handle, to_handle};

    const FIRST_TEST_VALUE: i32 = 42;
    const SECOND_TEST_VALUE: i32 = 57;

    #[test]
    fn snapshot_ref_view() {
        let db = setup_database();
        let snapshot = db.snapshot();
        let view = View::from_ref_snapshot(&*snapshot);
        check_snapshot(view.get());
        assert!(!view.can_convert_into_fork());
    }

    #[test]
    fn snapshot_owned_view() {
        let db = setup_database();
        let snapshot = db.snapshot();
        let view = View::from_owned_snapshot(snapshot);
        check_snapshot(view.get());
        assert!(!view.can_convert_into_fork());
    }

    #[test]
    fn fork_ref_view() {
        let db = setup_database();
        let fork = db.fork();
        let view = View::from_ref_fork(&fork);
        check_fork(view.get());
        assert!(!view.can_convert_into_fork());
    }

    #[test]
    fn fork_mut_ref_view() {
        let db = setup_database();
        let mut fork = db.fork();
        let view = View::from_ref_mut_fork(&mut fork);
        check_fork(view.get());
        assert!(!view.can_convert_into_fork());
    }

    #[test]
    fn fork_owned_view() {
        let db = setup_database();
        let fork = db.fork();
        let view = View::from_owned_fork(fork);
        check_fork(view.get());
        assert!(view.can_convert_into_fork());
    }

    #[test]
    fn convert_fork_into_patch() {
        let db = TemporaryDB::new();
        let fork = db.fork();
        let view = View::from_owned_fork(fork);
        let _patch = view.into_fork().into_patch();
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
            ViewRef::Snapshot(_) => check_value(&view_ref, FIRST_TEST_VALUE),
            _ => panic!(),
        }
    }

    fn check_fork(view_ref: ViewRef) {
        match view_ref {
            ViewRef::Fork(fork) => {
                check_value(&view_ref, FIRST_TEST_VALUE);
                {
                    let mut index = entry(fork);
                    index.set(SECOND_TEST_VALUE);
                }
                check_value(&view_ref, SECOND_TEST_VALUE);
            }
            _ => panic!(),
        }
    }

    // Creates database with a prepared state.
    fn setup_database() -> TemporaryDB {
        let db = TemporaryDB::new();
        let fork = db.fork();
        entry(&fork).set(FIRST_TEST_VALUE);
        db.merge(fork.into_patch()).unwrap();
        db
    }

    fn check_value(view_ref: &ViewRef, expected: i32) {
        let value = match *view_ref {
            ViewRef::Snapshot(snapshot) => entry(&*snapshot).get(),
            ViewRef::Fork(fork) => entry(&*fork).get(),
        };
        assert_eq!(Some(expected), value);
    }

    fn entry<T>(view: T) -> Entry<T, i32>
    where
        T: IndexAccess,
    {
        Entry::new("test", view)
    }
}
