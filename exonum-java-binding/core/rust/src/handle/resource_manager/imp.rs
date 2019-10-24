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

use std::{any::TypeId, collections::HashMap, sync::RwLock};

use super::super::Handle;

lazy_static! {
    static ref HANDLES_MAP: RwLock<HashMap<Handle, TypeId>> = RwLock::new(HashMap::new());
}

/// Adds given handle to the resource manager.
///
/// # Panics
///
/// Panics if handle is equal to zero or it is already present in the resource manager.
pub fn add_handle<T: 'static>(handle: Handle) {
    assert_ne!(handle, 0);
    assert!(
        HANDLES_MAP
            .write()
            .expect("Unable to obtain write-lock")
            .insert(handle, TypeId::of::<T>())
            .is_none(),
        "Trying to add the same handle for the second time: {:X}",
        handle
    )
}

/// Removes the given handle from the resource manager.
///
/// # Panics
///
/// See `check_handle_impl` for details.
pub fn remove_handle<T: 'static>(handle: Handle) {
    check_handle::<T>(handle);
    // Return value is ignored because `check_handle` already checks that handle is present.
    HANDLES_MAP
        .write()
        .expect("Unable to obtain write-lock")
        .remove(&handle);
}

/// Checks given handle for validity.
///
/// # Panics
///
/// Panics if handle is unknown or its type or ownership model is wrong.
pub fn check_handle<T: 'static>(handle: Handle) {
    match HANDLES_MAP
        .read()
        .expect("Unable to obtain read-lock")
        .get(&handle)
    {
        Some(type_id) => {
            let actual_object_type = TypeId::of::<T>();
            assert_eq!(
                *type_id, actual_object_type,
                "Wrong type id for '{:X}' handle",
                handle
            );
        }
        None => panic!("Invalid handle value: '{:X}'", handle),
    }
}

/// Returns the number of known handles.
pub fn known_handles() -> usize {
    HANDLES_MAP
        .read()
        .expect("Unable to obtain read-lock")
        .len()
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::i64;

    enum T {}
    const INVALID_HANDLE: Handle = i64::MAX;

    // Unique ("valid") handles should be used in the each test because `HANDLES_MAP` is a shared
    // state and tests are run concurrently.
    const MANAGE_HANDLES_FIRST_HANDLE: Handle = 1000;
    const MANAGE_HANDLES_SECOND_HANDLE: Handle = 2000;
    const DUPLICATED_HANDLE: Handle = 3000;
    const WRONG_TYPE_HANDLE: Handle = 4000;

    #[test]
    fn manage_handles() {
        // Add handle.
        enum T1 {}
        add_handle::<T1>(MANAGE_HANDLES_FIRST_HANDLE);
        check_handle::<T1>(MANAGE_HANDLES_FIRST_HANDLE);

        // Add second handle.
        enum T2 {}
        add_handle::<T2>(MANAGE_HANDLES_SECOND_HANDLE);
        check_handle::<T2>(MANAGE_HANDLES_SECOND_HANDLE);

        remove_handle::<T2>(MANAGE_HANDLES_SECOND_HANDLE);

        // Reuse handle value.
        add_handle::<T1>(MANAGE_HANDLES_SECOND_HANDLE);
        check_handle::<T1>(MANAGE_HANDLES_SECOND_HANDLE);

        // Remove all handles.
        remove_handle::<T1>(MANAGE_HANDLES_FIRST_HANDLE);
        remove_handle::<T1>(MANAGE_HANDLES_SECOND_HANDLE);
    }

    #[test]
    #[should_panic(expected = "assertion failed: `(left != right)`\n  left: `0`,\n right: `0`")]
    fn add_zero_handle() {
        add_handle::<T>(0);
    }

    #[test]
    #[should_panic(expected = "Trying to add the same handle for the second time")]
    fn add_duplicated_handle() {
        add_handle::<T>(DUPLICATED_HANDLE);
        add_handle::<T>(DUPLICATED_HANDLE);
    }

    #[test]
    #[should_panic(expected = "Invalid handle value")]
    fn remove_zero_handle() {
        remove_handle::<T>(0);
    }

    #[test]
    #[should_panic(expected = "Invalid handle value")]
    fn remove_invalid_handle() {
        remove_handle::<T>(INVALID_HANDLE);
    }

    #[test]
    #[should_panic(expected = "Invalid handle value")]
    fn check_zero_handle() {
        check_handle::<T>(0);
    }

    #[test]
    #[should_panic(expected = "Invalid handle value")]
    fn check_invalid_handle() {
        check_handle::<T>(INVALID_HANDLE);
    }

    #[test]
    #[should_panic(expected = "Wrong type id for")]
    fn check_wrong_type_handle() {
        add_handle::<T>(WRONG_TYPE_HANDLE);
        enum OtherT {}
        check_handle::<OtherT>(WRONG_TYPE_HANDLE);
    }
}
