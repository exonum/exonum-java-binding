use std::any::TypeId;
use std::collections::HashMap;
use std::sync::RwLock;

use utils::Handle;

lazy_static! {
    static ref HANDLES_MAP: RwLock<HashMap<Handle, HandleInfo>> = RwLock::new(HashMap::new());
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

/// Adds given handle to the resource manager.
///
/// # Panics
///
/// Panics if handle is equal to zero or it is already present in the resource manager.
fn add_handle_impl<T: 'static>(handle: Handle, ownership: HandleOwnershipType) {
    assert_ne!(handle, 0);
    assert!(
        HANDLES_MAP
            .write()
            .expect("Unable to obtain write-lock")
            .insert(handle, HandleInfo::new(TypeId::of::<T>(), ownership))
            .is_none(),
        "Trying to add the same handle for the second time: {:X}, handle"
    )
}

/// Removes the given handle from the resource manager.
///
/// # Panics
///
/// See `check_handle_impl` for details.
fn remove_handle_impl<T: 'static>(handle: Handle, ownership: HandleOwnershipType) {
    check_handle_impl::<T>(handle, Some(ownership));
    // Return value is ignored because `check_handle_impl` already checks that handle is present.
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
fn check_handle_impl<T: 'static>(handle: Handle, ownership: Option<HandleOwnershipType>) {
    match HANDLES_MAP
        .read()
        .expect("Unable to obtain read-lock")
        .get(&handle) {
        Some(info) => {
            let actual_object_type = TypeId::of::<T>();
            assert_eq!(
                info.object_type,
                actual_object_type,
                "Wrong type id for '{:X}' handle",
                handle
            );

            match ownership {
                Some(val) => {
                    assert_eq!(
                        val,
                        info.ownership,
                        "Error: '{:X}' handle should be {:?}",
                        handle,
                        info.ownership
                    );
                }
                None => (),
            }
        }
        None => panic!("Invalid handle value: '{:X}'", handle),
    }
}

/// Adds Java-owned handle to the resource manager.
///
/// # Panics
///
/// See `add_handle_impl` for the details.
pub fn add_handle<T: 'static>(handle: Handle) {
    add_handle_impl::<T>(handle, HandleOwnershipType::JavaOwned);
}

/// Removes Java-owned handle from the resource manager.
///
/// # Panics
///
/// See `remove_handle_impl` for the details.
pub fn remove_handle<T: 'static>(handle: Handle) {
    remove_handle_impl::<T>(handle, HandleOwnershipType::JavaOwned);
}

/// Adds native-owned handle to the resource manager.
///
/// # Panics
///
/// See `add_handle_impl` for the details.
pub fn register_handle<T: 'static>(handle: Handle) {
    assert_ne!(handle, 0);
    add_handle_impl::<T>(handle, HandleOwnershipType::NativeOwned);
}

/// Removes native-owned handle from the resource manager.
///
/// # Panics
///
/// See `remove_handle_impl` for the details.
pub fn unregister_handle<T: 'static>(handle: Handle) {
    remove_handle_impl::<T>(handle, HandleOwnershipType::NativeOwned);
}

/// Checks given handle for validity.
///
/// # Panics
///
/// See `check_handle_impl` for the details.
pub fn check_handle<T: 'static>(handle: Handle) {
    check_handle_impl::<T>(handle, None);
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
        // Add Java-owned handle.
        enum T1 {}
        add_handle::<T1>(MANAGE_HANDLES_FIRST_HANDLE);
        check_handle::<T1>(MANAGE_HANDLES_FIRST_HANDLE);
        check_handle_impl::<T1>(
            MANAGE_HANDLES_FIRST_HANDLE,
            Some(HandleOwnershipType::JavaOwned),
        );

        // Add second Java-owned handle.
        enum T2 {}
        add_handle::<T2>(MANAGE_HANDLES_SECOND_HANDLE);
        check_handle::<T2>(MANAGE_HANDLES_SECOND_HANDLE);
        check_handle_impl::<T2>(
            MANAGE_HANDLES_SECOND_HANDLE,
            Some(HandleOwnershipType::JavaOwned),
        );

        remove_handle::<T2>(MANAGE_HANDLES_SECOND_HANDLE);

        // Reuse handle value.
        add_handle::<T1>(MANAGE_HANDLES_SECOND_HANDLE);
        check_handle::<T1>(MANAGE_HANDLES_SECOND_HANDLE);
        check_handle_impl::<T1>(
            MANAGE_HANDLES_SECOND_HANDLE,
            Some(HandleOwnershipType::JavaOwned),
        );

        // Add native-owned handle.
        enum T3 {}
        register_handle::<T3>(MANAGE_HANDLES_NON_OWNED_HANDLE);
        check_handle::<T3>(MANAGE_HANDLES_NON_OWNED_HANDLE);
        check_handle_impl::<T3>(
            MANAGE_HANDLES_NON_OWNED_HANDLE,
            Some(HandleOwnershipType::NativeOwned),
        );

        // Remove all handles.
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

    #[test]
    #[should_panic(expected = "handle should be")]
    fn check_wrong_ownrship_handle() {
        add_handle::<T>(WRONG_OWNERSHIP_HANDLE);
        unregister_handle::<T>(WRONG_OWNERSHIP_HANDLE);
    }
}
