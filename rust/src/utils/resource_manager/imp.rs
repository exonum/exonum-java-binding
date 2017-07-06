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
            .unwrap()
            .insert(handle, TypeId::of::<T>())
            .is_none(),
        "Trying to add the same handle for the second time"
    );
}

pub fn remove_handle<T: 'static>(handle: Handle) {
    check_handle::<T>(handle);
    HANDLES_MAP.write().unwrap().remove(&handle);
}

pub fn check_handle<T: 'static>(handle: Handle) {
    match HANDLES_MAP.read().unwrap().get(&handle) {
        Some(type_id) => {
            assert_eq!(
                type_id,
                &TypeId::of::<T>(),
                "Wrong type id for '{}' handle",
                handle
            );
        }
        None => panic!("Invalid handle value: '{:X}'", handle),
    }
}
