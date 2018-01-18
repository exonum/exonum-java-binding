use std::any::TypeId;
use std::sync::RwLock;
use std::collections::HashMap;

use utils::Handle;

lazy_static! {
    static ref HANDLES_MAP: RwLock<HashMap<Handle, HandleInfo>> = RwLock::new(HashMap::new());
}

struct HandleInfo {
    type_id: TypeId,
    is_owned: bool,
}

impl HandleInfo {
    fn new(type_id: TypeId, is_owned: bool) -> Self {
        Self { type_id, is_owned }
    }
}

/// Adds handle to the resource manager.
fn add_handle_impl<T: 'static>(handle: Handle, is_owned: bool) {
    assert_ne!(handle, 0);
    assert!(
        HANDLES_MAP
            .write()
            .expect("Unable to obtain write-lock")
            .insert(handle, HandleInfo::new(TypeId::of::<T>(), is_owned))
            .is_none(),
        "Trying to add the same handle for the second time: {:X}, handle"
    )
}

/// Removes handle from the resource manager.
pub fn remove_handle_impl<T: 'static>(handle: Handle, is_owned: bool) {
    check_handle::<T>(handle, is_owned);
    HANDLES_MAP
        .write()
        .expect("Unable to obtain write-lock")
        .remove(&handle);
}

/// Adds owned handle to the resource manager.
pub fn add_handle<T: 'static>(handle: Handle) {
    add_handle_impl::<T>(handle, true);
}

/// Removes owned handle from the resource manager.
pub fn remove_handle<T: 'static>(handle: Handle) {
    remove_handle_impl::<T>(handle, true);
}

/// Adds non-owned handle to the resource manager.
pub fn register_handle<T: 'static>(handle: Handle) {
    add_handle_impl::<T>(handle, false);
}

/// Removes non-owned handle from the resource manager.
pub fn unregister_handle<T: 'static>(handle: Handle) {
    remove_handle_impl::<T>(handle, false);
}

pub fn check_handle<T: 'static>(handle: Handle, is_owned: bool) {
    match HANDLES_MAP
        .read()
        .expect("Unable to obtain read-lock")
        .get(&handle) {
        Some(info) => {
            let actual_type_id = TypeId::of::<T>();
            assert_eq!(
                info.type_id,
                actual_type_id,
                "Wrong type id for '{:X}' handle",
                handle
            );
            assert_eq!(
                is_owned,
                info.is_owned,
                "Error: '{:X}' handle should be {}owned",
                handle,
                if info.is_owned { "" } else { "non-" }
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
    use std::i64;
    use super::*;

    enum T {}
    const INVALID_HANDLE: Handle = i64::MAX;

    // Unique ("valid") handles should be used in the each test because `HANDLES_MAP` is a shared
    // state and tests are run concurrently.
    const MANAGE_HANDLES_FIRST_HANDLE: Handle = 1000;
    const MANAGE_HANDLES_SECOND_HANDLE: Handle = 2000;
    const MANAGE_HANDLES_NON_OWNED_HANDLE: Handle = 3000;
    const DUPLICATED_HANDLE: Handle = 4000;
    const WRONG_TYPE_HANDLE: Handle = 5000;
    const WRONG_OWNERSHIP_HANDLE: Handle = 6000;

    #[test]
    fn manage_handles() {
        assert_eq!(0, known_handles());

        enum T1 {}
        add_handle::<T1>(MANAGE_HANDLES_FIRST_HANDLE);
        check_handle::<T1>(MANAGE_HANDLES_FIRST_HANDLE, true);

        enum T2 {}
        add_handle::<T2>(MANAGE_HANDLES_SECOND_HANDLE);
        check_handle::<T2>(MANAGE_HANDLES_SECOND_HANDLE, true);

        remove_handle::<T2>(MANAGE_HANDLES_SECOND_HANDLE);

        // Reuse handle value.
        add_handle::<T1>(MANAGE_HANDLES_SECOND_HANDLE);
        check_handle::<T1>(MANAGE_HANDLES_SECOND_HANDLE, true);

        enum T3 {}
        register_handle::<T3>(MANAGE_HANDLES_NON_OWNED_HANDLE);
        check_handle::<T3>(MANAGE_HANDLES_NON_OWNED_HANDLE, false);

        remove_handle::<T1>(MANAGE_HANDLES_FIRST_HANDLE);
        remove_handle::<T1>(MANAGE_HANDLES_SECOND_HANDLE);
        unregister_handle::<T3>(MANAGE_HANDLES_NON_OWNED_HANDLE);
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
        check_handle::<T>(0, true);
    }

    #[test]
    #[should_panic(expected = "Invalid handle value")]
    fn check_invalid_handle() {
        check_handle::<T>(INVALID_HANDLE, true);
    }

    #[test]
    #[should_panic(expected = "Wrong type id for")]
    fn check_wrong_type_handle() {
        add_handle::<T>(WRONG_TYPE_HANDLE);
        enum OtherT {}
        check_handle::<OtherT>(WRONG_TYPE_HANDLE, true);
    }

    #[test]
    #[should_panic(expected = "handle should be")]
    fn check_wrong_ownrship_handle() {
        add_handle::<T>(WRONG_OWNERSHIP_HANDLE);
        register_handle::<T>(WRONG_OWNERSHIP_HANDLE);
    }
}
