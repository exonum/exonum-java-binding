use std::any::TypeId;
use std::sync::Mutex;
use std::collections::BTreeMap;

use utils::Handle;

lazy_static! {
    static ref HANDLES_MAP: Mutex<BTreeMap<Handle, TypeId>> = Mutex::new(BTreeMap::new());
}

pub fn add_handle<T: 'static>(handle: Handle) {
    assert_ne!(handle, 0);
    assert!(
        HANDLES_MAP
            .lock()
            .unwrap()
            .insert(handle, TypeId::of::<T>())
            .is_none(),
        "Trying to add the same handle for the second time"
    );
}

pub fn remove_handle<T: 'static>(handle: Handle) {
    check_handle::<T>(handle);
    HANDLES_MAP.lock().unwrap().remove(&handle);
}

pub fn check_handle<T: 'static>(handle: Handle) {
    match HANDLES_MAP.lock().unwrap().get(&handle) {
        Some(type_id) => {
            assert_eq!(
                type_id,
                &TypeId::of::<T>(),
                "Wrong type id for '{}' handle",
                handle
            );
        }
        None => panic!("Invalid handle value: '{}'", handle),
    }
}
