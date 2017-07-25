use std::any::TypeId;
use std::sync::RwLock;
use std::collections::HashMap;

use utils::Handle;

lazy_static! {
    static ref HANDLES_MAP: RwLock<HashMap<Handle, TypeId>> = RwLock::new(HashMap::new());
}

pub fn add_handle<T: 'static>(handle: Handle) {
    assert_ne!(handle, 0);
    assert!(
        HANDLES_MAP
            .write()
            .expect("Unable to make write-lock")
            .insert(handle, TypeId::of::<T>())
            .is_none(),
        "Trying to add the same handle for the second time: {:X}, handle"
    );
}

pub fn remove_handle<T: 'static>(handle: Handle) {
    check_handle::<T>(handle);
    HANDLES_MAP
        .write()
        .expect("Unable to make write-lock")
        .remove(&handle);
}

pub fn check_handle<T: 'static>(handle: Handle) {
    match HANDLES_MAP.read().expect("Unable to make read-lock").get(
        &handle,
    ) {
        Some(expected_type_id) => {
            let actual_type_id = &TypeId::of::<T>();
            assert_eq!(
                expected_type_id,
                actual_type_id,
                "Wrong type id for '{:X}' handle",
                handle
            );
        }
        None => panic!("Invalid handle value: '{:X}'", handle),
    }
}

#[cfg(test)]
mod tests {
    use std::i64;
    use super::*;

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
        enum T1 {}
        add_handle::<T1>(MANAGE_HANDLES_FIRST_HANDLE);
        check_handle::<T1>(MANAGE_HANDLES_FIRST_HANDLE);

        enum T2 {}
        add_handle::<T2>(MANAGE_HANDLES_SECOND_HANDLE);
        check_handle::<T2>(MANAGE_HANDLES_SECOND_HANDLE);

        remove_handle::<T2>(MANAGE_HANDLES_SECOND_HANDLE);

        // Reuse handle value.
        add_handle::<T1>(MANAGE_HANDLES_SECOND_HANDLE);
        check_handle::<T1>(MANAGE_HANDLES_SECOND_HANDLE);

        remove_handle::<T1>(MANAGE_HANDLES_FIRST_HANDLE);
        remove_handle::<T1>(MANAGE_HANDLES_SECOND_HANDLE);
    }

    #[test]
    #[should_panic(expected = "assertion failed: `(left != right)` (left: `0`, right: `0`)")]
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
