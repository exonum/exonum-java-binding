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

use chashmap::CHashMap;
use std::any::TypeId;

use utils::Handle;

const MAP_CAPACITY: usize = 512;

lazy_static! {
    static ref HANDLES: HandlesStorage = HandlesStorage::with_capacity(MAP_CAPACITY);
}

/// Encapsulates key-value storage that can be used in concurrent environment.
struct HandlesStorage {
    inner: CHashMap<Handle, HandleInfo>,
}

impl HandlesStorage {
    pub fn with_capacity(capacity: usize) -> Self {
        HandlesStorage {
            inner: CHashMap::with_capacity(capacity),
        }
    }

    /// Adds given handle to the storage.
    ///
    /// # Panics
    ///
    /// Panics if handle is equal to zero or it is already present in the storage.
    fn add_handle<T: 'static>(&self, handle: Handle, ownership: HandleOwnershipType) {
        assert_ne!(handle, 0);
        self.inner.alter(handle, |old_value| match old_value {
            Some(_) => panic!(
                "Trying to add the same handle for the second time: {:X}",
                handle
            ),
            None => Some(HandleInfo::new(TypeId::of::<T>(), ownership)),
        })
    }

    /// Removes given handle from the storage.
    ///
    /// # Panics
    ///
    /// See `check_handle` for details.
    fn remove_handle<T: 'static>(&self, handle: Handle, ownership: HandleOwnershipType) {
        match self.inner.remove(&handle) {
            Some(old_value) => {
                Self::check_handle_type_and_ownership::<T>(handle, &old_value, Some(ownership))
            }
            None => panic!("Invalid handle value: '{:X}'", handle),
        }
    }

    /// Checks given handle for validity.
    ///
    /// # Panics
    ///
    /// Panics if handle is unknown or its type or ownership model is wrong.
    fn check_handle<T: 'static>(&self, handle: Handle, ownership: Option<HandleOwnershipType>) {
        match self.inner.get(&handle) {
            Some(handle_info) => {
                Self::check_handle_type_and_ownership::<T>(handle, &*handle_info, ownership)
            }
            None => panic!("Invalid handle value: '{:X}'", handle),
        }
    }

    /// Returns the number of entries in the handles storage
    fn len(&self) -> usize {
        self.inner.len()
    }

    // Panics in case of type or ownership model of specified handle is incorrect.
    fn check_handle_type_and_ownership<T: 'static>(
        handle: Handle,
        handle_info: &HandleInfo,
        ownership: Option<HandleOwnershipType>,
    ) {
        let actual_object_type = TypeId::of::<T>();
        assert_eq!(
            handle_info.object_type, actual_object_type,
            "Wrong type id for '{:X}' handle",
            handle
        );

        if let Some(ownership) = ownership {
            assert_eq!(
                ownership, handle_info.ownership,
                "Error: '{:X}' handle should be {:?}",
                handle, handle_info.ownership
            );
        }
    }
}

/// Represents `Handle` ownership model.
#[derive(Debug, PartialEq, Eq)]
enum HandleOwnershipType {
    /// A handle to a native object, owned by the Java side.
    JavaOwned,
    /// A handle to a native object, owned by the native side, and temporarily made available to
    /// the Java side.
    NativeOwned,
}

/// Information associated with handle.
#[derive(Debug)]
struct HandleInfo {
    object_type: TypeId,
    ownership: HandleOwnershipType,
}

impl HandleInfo {
    fn new(object_type: TypeId, ownership: HandleOwnershipType) -> Self {
        Self {
            object_type,
            ownership,
        }
    }
}

/// Adds Java-owned handle to the resource manager.
///
/// # Panics
///
/// See `add_handle_impl` for the details.
pub fn add_handle<T: 'static>(handle: Handle) {
    HANDLES.add_handle::<T>(handle, HandleOwnershipType::JavaOwned);
}

/// Removes Java-owned handle from the resource manager.
///
/// # Panics
///
/// See `remove_handle_impl` for the details.
pub fn remove_handle<T: 'static>(handle: Handle) {
    HANDLES.remove_handle::<T>(handle, HandleOwnershipType::JavaOwned);
}

/// Adds native-owned handle to the resource manager.
///
/// # Panics
///
/// See `add_handle_impl` for the details.
pub fn register_handle<T: 'static>(handle: Handle) {
    HANDLES.add_handle::<T>(handle, HandleOwnershipType::NativeOwned);
}

/// Removes native-owned handle from the resource manager.
///
/// # Panics
///
/// See `remove_handle_impl` for the details.
pub fn unregister_handle<T: 'static>(handle: Handle) {
    HANDLES.remove_handle::<T>(handle, HandleOwnershipType::NativeOwned);
}

/// Checks given handle for validity.
///
/// # Panics
///
/// See `check_handle_impl` for the details.
pub fn check_handle<T: 'static>(handle: Handle) {
    HANDLES.check_handle::<T>(handle, None);
}

/// Returns the number of known handles.
pub fn known_handles() -> usize {
    HANDLES.len()
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::i64;

    enum T {}
    enum HandleFooType {}
    enum HandleBarType {}
    const HANDLE_FOO: Handle = 1000;
    const HANDLE_BAR: Handle = 2000;
    const INVALID_HANDLE: Handle = i64::MAX;
    const ZERO_HANDLE: Handle = 0;

    #[test]
    fn check_handle_type_and_ownership_ok() {
        let handle_info = prepare_handle_info();
        HandlesStorage::check_handle_type_and_ownership::<HandleFooType>(
            HANDLE_FOO,
            &handle_info,
            Some(HandleOwnershipType::NativeOwned),
        );
    }

    #[test]
    fn check_handle_type_and_no_ownership_ok() {
        let handle_info = prepare_handle_info();
        HandlesStorage::check_handle_type_and_ownership::<HandleFooType>(
            HANDLE_FOO,
            &handle_info,
            None,
        );
    }

    #[test]
    #[should_panic(expected = "Wrong type id for")]
    fn check_handle_type_wrong() {
        let handle_info = prepare_handle_info();
        HandlesStorage::check_handle_type_and_ownership::<HandleBarType>(
            HANDLE_BAR,
            &handle_info,
            None,
        );
    }

    #[test]
    #[should_panic(expected = "handle should be")]
    fn check_handle_ownership_wrong() {
        let handle_info = prepare_handle_info();
        HandlesStorage::check_handle_type_and_ownership::<HandleFooType>(
            HANDLE_FOO,
            &handle_info,
            Some(HandleOwnershipType::JavaOwned),
        );
    }

    #[test]
    fn manage_handles() {
        let storage = prepare_storage();
        assert_eq!(storage.len(), 0);

        // Add Java-owned handle.
        storage.add_handle::<HandleFooType>(HANDLE_FOO, HandleOwnershipType::JavaOwned);
        storage.check_handle::<HandleFooType>(HANDLE_FOO, Some(HandleOwnershipType::JavaOwned));
        storage.check_handle::<HandleFooType>(HANDLE_FOO, None);
        assert_eq!(storage.len(), 1);

        // Add Native-owned handle.
        storage.add_handle::<HandleBarType>(HANDLE_BAR, HandleOwnershipType::NativeOwned);
        storage.check_handle::<HandleBarType>(HANDLE_BAR, Some(HandleOwnershipType::NativeOwned));
        storage.check_handle::<HandleBarType>(HANDLE_BAR, None);
        assert_eq!(storage.len(), 2);

        storage.remove_handle::<HandleFooType>(HANDLE_FOO, HandleOwnershipType::JavaOwned);
        assert_eq!(storage.len(), 1);
        storage.remove_handle::<HandleBarType>(HANDLE_BAR, HandleOwnershipType::NativeOwned);
        assert_eq!(storage.len(), 0);
    }

    #[test]
    #[should_panic(expected = "assertion failed: `(left != right)`\n  left: `0`,\n right: `0`")]
    fn add_zero_handle() {
        let storage = prepare_storage();
        storage.add_handle::<T>(ZERO_HANDLE, HandleOwnershipType::NativeOwned);
    }

    #[test]
    #[should_panic(expected = "Trying to add the same handle for the second time")]
    fn add_duplicated_handle() {
        let storage = prepare_storage();
        storage.add_handle::<HandleFooType>(HANDLE_FOO, HandleOwnershipType::JavaOwned);
        storage.add_handle::<HandleFooType>(HANDLE_FOO, HandleOwnershipType::JavaOwned);
    }

    #[test]
    #[should_panic(expected = "Invalid handle value")]
    fn remove_zero_handle() {
        let storage = prepare_storage();
        storage.remove_handle::<T>(ZERO_HANDLE, HandleOwnershipType::JavaOwned);
    }

    #[test]
    #[should_panic(expected = "Invalid handle value")]
    fn remove_invalid_handle() {
        let storage = prepare_storage();
        storage.remove_handle::<T>(INVALID_HANDLE, HandleOwnershipType::NativeOwned);
    }

    #[test]
    #[should_panic(expected = "Invalid handle value")]
    fn check_zero_handle() {
        let storage = prepare_storage();
        storage.check_handle::<T>(ZERO_HANDLE, None);
    }

    #[test]
    #[should_panic(expected = "Invalid handle value")]
    fn check_invalid_handle() {
        let storage = prepare_storage();
        storage.check_handle::<T>(INVALID_HANDLE, None);
    }

    fn prepare_storage() -> HandlesStorage {
        HandlesStorage::with_capacity(0)
    }

    fn prepare_handle_info() -> HandleInfo {
        HandleInfo::new(
            TypeId::of::<HandleFooType>(),
            HandleOwnershipType::NativeOwned,
        )
    }
}
