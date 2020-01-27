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

use exonum_merkledb::{
    access::Prefixed,
    migration::{Migration, MigrationHelper, Scratchpad},
    Fork, Snapshot,
};
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
/// The View does not have a lifetime, so we use `unsafe` to prolong the lifetime of the reference
/// it was constructed with to 'static. If this original reference is destroyed (leaves the scope),
/// our prolonged reference stored inside View is no longer valid and will lead to SIGINT if we
/// use it. So we must carefully review all the places where View is constructed from references
/// to make sure View never outlives the original reference.
///
/// Java code must never store a handle to the `View::Ref*` variants for longer than
/// the method invocation.
#[derive(Debug)]
pub(crate) enum View {
    /// Immutable Fork view, constructed from `&Fork`.
    ///
    /// Created `View` must never outlive the reference it was created with,
    /// or `SIGINT` will occur.
    RefFork(&'static Fork),
    /// Mutable Fork view, constructed from `&mut Fork`.
    ///
    /// Created `View` must never outlive the reference it was created with,
    /// or `SIGINT` will occur.
    RefMutFork(&'static mut Fork),
    /// Immutable Snapshot view, constructed from `&Snapshot`. There is no need in mutable
    /// variant.
    ///
    /// Created `View` must never outlive the reference it was constructed with,
    /// or `SIGINT` will occur.
    RefSnapshot(&'static dyn Snapshot),
    /// Covers both `Snapshot` and `Fork` cases. Rust uses move semantic and single-ownership
    /// rule to guarantee that the `View` will be valid for the whole execution.
    ///
    /// This is the most safe `View` variant, no special care is needed when working with.
    Owned(ViewOwned),
}

pub(crate) enum EjbAccess {
    Raw(View),
    Migration(View, String),
    Scratchpad(View, String),
    Prefixed(View, String),
}

impl EjbAccess {
    pub fn migration(access: View, namespace: &str) -> Self {
        EjbAccess::Migration(access, namespace.to_string())
    }

    pub fn scratchpad(access: View, namespace: &str) -> Self {
        EjbAccess::Scratchpad(access, namespace.to_string())
    }

    pub fn prefixed(access: View, namespace: &str) -> Self {
        EjbAccess::Prefixed(access, namespace.to_string())
    }

    pub fn get_migration(&self) -> Migration<&Fork> {
        match self {
            EjbAccess::Migration(raw_view, ref namespace) => match raw_view {
                View::RefFork(fork) => Migration::new(namespace, fork),
                View::Owned(ViewOwned::Fork(fork)) => Migration::new(namespace, fork),
                _ => unimplemented!(),
            },
            _ => unimplemented!(),
        }
    }

    pub fn get_scratchpad(&self) -> Scratchpad<&Fork> {
        match self {
            EjbAccess::Scratchpad(raw_view, ref namespace) => match raw_view {
                View::RefFork(fork) => Scratchpad::new(namespace, fork),
                View::Owned(ViewOwned::Fork(fork)) => Scratchpad::new(namespace, fork),
                _ => unimplemented!(),
            },
            _ => unimplemented!(),
        }
    }

    pub fn get_prefixed(&self) -> EjbPrefixed<'_> {
        match self {
            EjbAccess::Prefixed(raw_view, ref namespace) => match raw_view {
                View::RefFork(fork) => EjbPrefixed::Mutable(Prefixed::new(namespace, fork)),
                View::RefMutFork(fork) => EjbPrefixed::Mutable(Prefixed::new(namespace, fork)),
                View::Owned(ViewOwned::Fork(fork)) => {
                    EjbPrefixed::Mutable(Prefixed::new(namespace, fork))
                }
                View::RefSnapshot(snapshot) => {
                    EjbPrefixed::Immutable(Prefixed::new(namespace, *snapshot))
                }
                View::Owned(ViewOwned::Snapshot(snapshot)) => {
                    EjbPrefixed::Immutable(Prefixed::new(namespace, snapshot))
                }
            },
            _ => unimplemented!(),
        }
    }

    pub fn get(&self) -> ViewRef<'_> {
        match self {
            EjbAccess::Raw(raw) => raw.get(),
            EjbAccess::Migration(_, _) => ViewRef::Migration(self.get_migration()),
            EjbAccess::Scratchpad(_, _) => ViewRef::Scratchpad(self.get_scratchpad()),
            EjbAccess::Prefixed(_, _) => ViewRef::Prefixed(self.get_prefixed()),
        }
    }
}

#[derive(Debug)]
pub(crate) enum ViewOwned {
    Snapshot(Box<dyn Snapshot>),
    Fork(Box<Fork>),
}

#[derive(Clone, Debug)]
pub(crate) enum EjbPrefixed<'a> {
    Immutable(Prefixed<'a, &'a dyn Snapshot>),
    Mutable(Prefixed<'a, &'a Fork>),
}

/// Hides the differences between owning and non-owning `View` variants
/// and simplifies the use of the indexes API.
#[derive(Clone, Debug)]
pub(crate) enum ViewRef<'a> {
    Snapshot(&'a dyn Snapshot),
    Fork(&'a Fork),
    Migration(Migration<'a, &'a Fork>),
    Scratchpad(Scratchpad<'a, &'a Fork>),
    Prefixed(EjbPrefixed<'a>),
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
        View::RefFork(unsafe { std::mem::transmute(fork) })
    }

    /// Creates `View::RefFork` variant.
    ///
    /// Created `View` must never outlive provided `fork` reference, or
    /// SIGINT will occur.
    ///
    /// Both indexes mutability and `&mut self` methods of `Fork` available.
    pub fn from_ref_mut_fork(fork: &mut Fork) -> Self {
        View::RefMutFork(unsafe { std::mem::transmute(fork) })
    }

    /// Returns temporary reference to the underlying `Fork` / `Snapshot` to simplify use
    /// in indexes operations.
    pub fn get(&self) -> ViewRef<'_> {
        match self {
            View::RefFork(fork_ref) => ViewRef::Fork(*fork_ref),
            View::RefMutFork(fork_ref) => ViewRef::Fork(*fork_ref),
            View::RefSnapshot(snapshot_ref) => ViewRef::Snapshot(*snapshot_ref),
            View::Owned(owned) => match owned {
                ViewOwned::Fork(fork) => ViewRef::Fork(&*fork),
                ViewOwned::Snapshot(snapshot) => ViewRef::Snapshot(&**snapshot),
            },
        }
    }

    /// Creates checkpoint for the owned Fork instance.
    ///
    /// Panics if it is not possible (`View::can_rollback` returns false).
    pub fn create_checkpoint(&mut self) {
        match self {
            View::Owned(ViewOwned::Fork(fork)) => fork.flush(),
            View::RefMutFork(fork) => fork.flush(),
            _ => panic!(
                "Cannot create checkpoint because this View does not support it: {:?}",
                self
            ),
        }
    }

    /// Rollbacks owned Fork to the latest checkpoint.
    /// If no checkpoint was created (`create_checkpoint` method was never called),
    /// rollbacks all changes in Fork.
    ///
    /// Does not affect database, but only a specific Fork instance.
    ///
    /// Panics if it is not possible (`View::can_rollback` returns false).
    pub fn rollback(&mut self) {
        match self {
            View::Owned(ViewOwned::Fork(fork)) => fork.rollback(),
            View::RefMutFork(fork) => fork.rollback(),
            _ => panic!(
                "Cannot rollback because this View does not support it: {:?}",
                self
            ),
        }
    }

    /// Unwraps the stored Fork from the View, panics if it's not possible.
    pub fn into_fork(self) -> Box<Fork> {
        if let View::Owned(ViewOwned::Fork(fork)) = self {
            fork
        } else {
            panic!(
                "`into_fork` called on non-owning View or Snapshot: {:?}",
                self
            );
        }
    }

    /// Returns `true` iff `into_fork` conversion is possible.
    pub fn can_convert_into_fork(&self) -> bool {
        match self {
            View::Owned(ViewOwned::Fork(_)) => true,
            _ => false,
        }
    }

    /// Returns `true` iff `create_checkpoint` and `rollback` methods available.
    pub fn can_rollback(&self) -> bool {
        match self {
            View::Owned(ViewOwned::Fork(_)) => true,
            View::RefMutFork(_) => true,
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
    use exonum_merkledb::{
        access::{Access, FromAccess},
        Database, Entry, TemporaryDB,
    };
    use {cast_handle, to_handle};

    const FIRST_TEST_VALUE: i32 = 42;
    const SECOND_TEST_VALUE: i32 = 57;

    #[test]
    fn snapshot_ref_view() {
        let db = setup_database();
        let snapshot = db.snapshot();
        let view = View::from_ref_snapshot(&*snapshot);
        check_snapshot(view.get());
        assert!(!view.can_convert_into_fork());
        assert!(!view.can_rollback());
    }

    #[test]
    fn snapshot_owned_view() {
        let db = setup_database();
        let snapshot = db.snapshot();
        let view = View::from_owned_snapshot(snapshot);
        check_snapshot(view.get());
        assert!(!view.can_convert_into_fork());
        assert!(!view.can_rollback());
    }

    #[test]
    fn fork_ref_view() {
        let db = setup_database();
        let fork = db.fork();
        let view = View::from_ref_fork(&fork);
        check_fork(view.get());
        assert!(!view.can_convert_into_fork());
        assert!(!view.can_rollback());
    }

    #[test]
    fn fork_mut_ref_view() {
        let db = setup_database();
        let mut fork = db.fork();
        let view = View::from_ref_mut_fork(&mut fork);
        check_fork(view.get());
        assert!(!view.can_convert_into_fork());
        assert!(view.can_rollback());
    }

    #[test]
    fn fork_owned_view() {
        let db = setup_database();
        let fork = db.fork();
        let view = View::from_owned_fork(fork);
        check_fork(view.get());
        assert!(view.can_convert_into_fork());
        assert!(view.can_rollback());
    }

    #[test]
    fn rollback() {
        let db = setup_database();
        let mut fork = db.fork();
        let mut view = View::from_ref_mut_fork(&mut fork);
        // create checkpoint that will be used later to restore Fork's state
        view.create_checkpoint();
        // change stored value to SECOND_TEST_VALUE
        check_fork(view.get());
        check_value(&view.get(), SECOND_TEST_VALUE);

        view.rollback();
        // Fork's state restored to the checkpoint
        assert!(check_value(&view.get(), FIRST_TEST_VALUE));
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
            View::RefMutFork(fork_ref) => {
                mock_method(fork_ref);
            }
            _ => unreachable!("Invalid variant of View, expected RefMutFork"),
        }
    }

    #[test]
    fn migration_support() {
        let db = TemporaryDB::new();
        // create migration
        let view = {
            let fork = db.fork();
            EjbAccess::migration(View::from_owned_fork(fork), "namespace")
        };

        // send to java and do stuff
        // ...
        // use migration:
        view.get_migration().create_tombstone("address");
    }

    #[test]
    fn scratchpad_support() {
        let db = setup_database();
        // create scratchpad
        let view = {
            let fork = db.fork();
            EjbAccess::scratchpad(View::from_owned_fork(fork), "namespace")
        };

        // send to java and do stuff
        // ...
        // use scratchpad:
        assert_eq!(check_value(&view.get(), FIRST_TEST_VALUE), false);
    }

    #[test]
    fn prefixed_support() {
        let db = setup_database();
        // create prefixed
        let view = {
            let fork = db.fork();
            EjbAccess::prefixed(View::from_owned_fork(fork), "namespace")
        };

        // send to java and do stuff
        // ...
        // use prefixed:
        assert_eq!(check_value(&view.get(), FIRST_TEST_VALUE), false);
    }

    #[test]
    fn migration_helper_support() {
        let db = TemporaryDB::new();

        struct MigrationHelperProxy {
            inner: &'static mut MigrationHelper,
        }

        impl MigrationHelperProxy {
            // represents native method which is called from Java
            pub fn scratchpad(handle: Handle) -> Handle {
                let helper_proxy: &mut MigrationHelperProxy = cast_handle(handle);
                to_handle(helper_proxy.inner.scratchpad())
            }
        }

        let migration_script = |helper: &mut MigrationHelper| {
            let proxy = MigrationHelperProxy {
                inner: unsafe { std::mem::transmute(helper) },
            };
            let handle = to_handle(proxy);
            // ...
            // sendHandleToJava(handle)
            // ...
            // java calls:
            let scratchpad: &mut Scratchpad<&Fork> =
                cast_handle(MigrationHelperProxy::scratchpad(handle));
            let index: Entry<_, i32> = Entry::from_access(*scratchpad, "address".into()).unwrap();
        };

        let mut helper = MigrationHelper::new(db, "namespace");
        migration_script(&mut helper);
    }

    fn check_snapshot(view_ref: ViewRef) {
        match view_ref {
            ViewRef::Snapshot(_) => assert!(check_value(&view_ref, FIRST_TEST_VALUE)),
            _ => unreachable!("Invalid variant of ViewRef, expected Snapshot"),
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
            _ => unreachable!("Invalid variant of ViewRef, expected Fork"),
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

    fn check_value(view_ref: &ViewRef, expected: i32) -> bool {
        let value = match *view_ref {
            ViewRef::Snapshot(snapshot) => entry(&*snapshot).get(),
            ViewRef::Fork(fork) => entry(&*fork).get(),
            ViewRef::Migration(migration) => entry(migration).get(),
            ViewRef::Scratchpad(scratchpad) => entry(scratchpad).get(),
            // wtf, Prefixed does not implement Copy, while `Migration` and `Scratchpad` does
            ViewRef::Prefixed(ref prefixed) => match prefixed {
                EjbPrefixed::Immutable(ref prefixed) => entry(prefixed.clone()).get(),
                EjbPrefixed::Mutable(ref prefixed) => entry(prefixed.clone()).get(),
            },
        };
        Some(expected) == value
    }

    fn entry<T>(view: T) -> Entry<T::Base, i32>
    where
        T: Access,
    {
        Entry::from_access(view, "test".into()).unwrap()
    }
}
