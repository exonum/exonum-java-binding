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

use exonum::{
    blockchain::{BlockProof, IndexProof, Schema},
    helpers::Height,
    runtime::SnapshotExt,
};
use exonum_merkledb::{
    access::{Access, AccessError},
    generic::{ErasedAccess, GenericAccess, GenericRawAccess},
    Fork, IndexAddress,
};
use jni::{
    objects::{JClass, JObject},
    sys::{jbyteArray, jlong, jstring},
    JNIEnv,
};

use std::{num::NonZeroU64, panic, rc::Rc};

use handle::{self, Handle};
use utils;

/// Prolongs lifetime of the GenericRawAccess.
///
/// The caller is responsible for validation of lifetime of the passed `raw_access`.
pub(crate) unsafe fn into_erased_access<'a, T>(raw_access: T) -> ErasedAccess<'static>
where
    T: Into<GenericRawAccess<'a>>,
{
    let generic_raw_access: GenericRawAccess = raw_access.into();
    // prolong the lifetime
    let generic_raw_access: GenericRawAccess<'static> = std::mem::transmute(generic_raw_access);
    ErasedAccess::from(generic_raw_access)
}

pub trait EjbAccessExt {
    /// Returns `true` iff `into_fork` conversion is possible.
    fn can_convert_into_fork(&self) -> bool;
    /// Unwraps the stored Fork from the Access, panics if it's not possible.
    fn into_fork(self) -> Fork;
    /// Creates checkpoint for the owned Fork instance.
    ///
    /// Panics if it is not possible (`EjbAccessExt::can_rollback` returns false).
    fn create_checkpoint(&mut self);
    /// Rollbacks owned Fork to the latest checkpoint.
    /// If no checkpoint was created (`create_checkpoint` method was never called),
    /// rollbacks all changes in Fork.
    ///
    /// Does not affect database, but only a specific Fork instance.
    ///
    /// Panics if it is not possible (`EjbAccessExt::can_rollback` returns false).
    fn rollback(&mut self);
    /// Returns `true` iff `create_checkpoint` and `rollback` methods available.
    fn can_rollback(&self) -> bool;
    /// Returns a proof of existence for the index with the specified name.
    ///
    /// Returns `None` if index was not initialized or does not exist.
    fn proof_for_index(&self, index_name: &str) -> Option<IndexProof>;
    /// Returns a proof of existence for the block with the specified height.
    ///
    /// Returns `None` if the block does not exist.
    fn proof_for_block(&self, height: u64) -> Option<BlockProof>;
    /// Returns `IndexMetadata::identifier` - a unique identifier of the index.
    ///
    /// Returns `None` if the index does not exist.
    fn find_index_id(&self, index_address: IndexAddress)
        -> Result<Option<NonZeroU64>, AccessError>;
}

impl<'a> EjbAccessExt for ErasedAccess<'a> {
    fn can_convert_into_fork(&self) -> bool {
        match self {
            GenericAccess::Raw(GenericRawAccess::OwnedFork(_)) => true,
            _ => false,
        }
    }

    fn into_fork(self) -> Fork {
        match self {
            GenericAccess::Raw(GenericRawAccess::OwnedFork(fork)) => Rc::try_unwrap(fork).unwrap(),
            _ => panic!(
                "'into_fork' called on non-owning access or Snapshot: {:?}",
                self
            ),
        }
    }

    fn create_checkpoint(&mut self) {
        match self {
            GenericAccess::Raw(GenericRawAccess::OwnedFork(ref mut fork)) => {
                let fork = Rc::get_mut(fork).unwrap();
                fork.flush();
            }
            _ => panic!(
                "'create_checkpoint' called on non-owning access or Snapshot: {:?}",
                self
            ),
        }
    }

    fn rollback(&mut self) {
        match self {
            GenericAccess::Raw(GenericRawAccess::OwnedFork(ref mut fork)) => {
                let fork = Rc::get_mut(fork).unwrap();
                fork.rollback();
            }
            _ => panic!(
                "'rollback' called on non-owning access or Snapshot: {:?}",
                self
            ),
        }
    }

    fn can_rollback(&self) -> bool {
        match self {
            GenericAccess::Raw(GenericRawAccess::OwnedFork(_)) => true,
            _ => false,
        }
    }

    fn proof_for_index(&self, index_name: &str) -> Option<IndexProof> {
        match self {
            GenericAccess::Raw(raw) => match raw {
                GenericRawAccess::Snapshot(snapshot) => snapshot.proof_for_index(index_name),
                GenericRawAccess::OwnedSnapshot(snapshot) => snapshot.proof_for_index(index_name),
                _ => panic!(
                    "'proof_for_index' called on non-Snapshot access: {:?}",
                    self
                ),
            },
            _ => panic!(
                "'proof_for_index' called on non-Snapshot access: {:?}",
                self
            ),
        }
    }

    fn proof_for_block(&self, height: u64) -> Option<BlockProof> {
        match self {
            GenericAccess::Raw(raw) => match raw {
                GenericRawAccess::Snapshot(snapshot) => {
                    Schema::new(*snapshot).block_and_precommits(Height(height))
                }
                GenericRawAccess::OwnedSnapshot(snapshot) => {
                    Schema::new(snapshot.as_ref()).block_and_precommits(Height(height))
                }
                _ => panic!(
                    "'proof_for_block' called on non-Snapshot access: {:?}",
                    self
                ),
            },
            _ => panic!(
                "'proof_for_block' called on non-Snapshot access: {:?}",
                self
            ),
        }
    }

    fn find_index_id(
        &self,
        index_address: IndexAddress,
    ) -> Result<Option<NonZeroU64>, AccessError> {
        let metadata = self.clone().get_index_metadata(index_address)?;
        Ok(metadata.map(|metadata| metadata.identifier()))
    }
}

/// Returns the unique id of the index.
///
/// Returns 0 if the index does not exist.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_database_AbstractAccess_nativeFindIndexId(
    env: JNIEnv,
    _: JObject,
    access_handle: Handle,
    name: jstring,
    id_in_group: jbyteArray,
) -> jlong {
    let res = panic::catch_unwind(|| {
        let access = handle::cast_handle::<ErasedAccess>(access_handle);
        let address = utils::convert_to_index_address(&env, name, id_in_group)?;
        match access.find_index_id(address).unwrap() {
            Some(id) => Ok(id.get() as jlong),
            None => Ok(0 as jlong),
        }
    });
    utils::unwrap_exc_or(&env, res, 0 as jlong)
}

/// Destroys underlying `Snapshot` or `Fork` object and frees memory.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_storage_database_Accesses_nativeFree(
    env: JNIEnv,
    _: JClass,
    access_handle: Handle,
) {
    handle::drop_handle::<ErasedAccess>(&env, access_handle);
}

#[cfg(test)]
mod tests {
    use exonum_merkledb::{
        access::{Access, AccessExt},
        generic::ErasedAccess,
        Database, Entry, TemporaryDB,
    };

    use super::*;

    const FIRST_TEST_VALUE: i32 = 42;
    const SECOND_TEST_VALUE: i32 = 57;

    #[test]
    fn snapshot_ref_access() {
        let db = setup_database();
        let snapshot = db.snapshot();
        let access = unsafe { into_erased_access(&*snapshot)};
        check_value(access.clone(), FIRST_TEST_VALUE);
        assert!(!access.can_convert_into_fork());
        assert!(!access.can_rollback());
    }

    #[test]
    fn snapshot_owned_access() {
        let db = setup_database();
        let snapshot = db.snapshot();
        let access = unsafe { into_erased_access(snapshot)};
        check_value(access.clone(), FIRST_TEST_VALUE);
        assert!(!access.can_convert_into_fork());
        assert!(!access.can_rollback());
    }

    #[test]
    fn fork_ref_access() {
        let db = setup_database();
        let fork = db.fork();
        let access = unsafe { into_erased_access(&fork)};
        check_fork(access.clone());
        assert!(!access.can_convert_into_fork());
        assert!(!access.can_rollback());
    }

    #[test]
    fn fork_owned_access() {
        let db = setup_database();
        let fork = db.fork();
        let access = unsafe { into_erased_access(fork)};
        check_fork(access.clone());
        assert!(access.can_convert_into_fork());
        assert!(access.can_rollback());
    }

    #[test]
    fn rollback() {
        let db = setup_database();
        let fork = db.fork();
        let mut access = unsafe { into_erased_access(fork)};
        // create checkpoint that will be used later to restore Fork's state
        access.create_checkpoint();
        // change stored value to SECOND_TEST_VALUE
        check_fork(access.clone());
        check_value(access.clone(), SECOND_TEST_VALUE);

        access.rollback();
        // Fork's state restored to the checkpoint
        check_value(access.clone(), FIRST_TEST_VALUE);
    }

    #[test]
    fn convert_fork_into_patch() {
        let db = TemporaryDB::new();
        let fork = db.fork();
        let access = unsafe { into_erased_access(fork)};
        let _patch = access.into_fork().into_patch();
    }

    fn check_fork(access: ErasedAccess) {
        check_value(access.clone(), FIRST_TEST_VALUE);
        {
            let mut index = entry(access.clone());
            index.set(SECOND_TEST_VALUE);
        }
        check_value(access.clone(), SECOND_TEST_VALUE);
    }

    // Creates database with a prepared state.
    fn setup_database() -> TemporaryDB {
        let db = TemporaryDB::new();
        let fork = db.fork();
        entry(&fork).set(FIRST_TEST_VALUE);
        db.merge(fork.into_patch()).unwrap();
        db
    }

    fn check_value(access: ErasedAccess, expected: i32) {
        let value = entry(access).get();
        assert_eq!(Some(expected), value);
    }

    fn entry<T>(access: T) -> Entry<T::Base, i32>
    where
        T: Access,
    {
        access.get_entry("test")
    }
}
