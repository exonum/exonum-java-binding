use jni::JNIEnv;
use jni::sys::jlong;

use std::panic;

use super::exception;
use super::resource_manager;

// Raw pointer passed to and from Java-side.
pub type Handle = jlong;

// Returns handle (raw pointer) to the given object allocated in the heap.
pub fn to_handle<T: 'static>(val: T) -> Handle {
    let handle = Box::into_raw(Box::new(val)) as Handle;
    resource_manager::add_handle::<T>(handle);
    handle
}

// Panics if object is equal to zero.
pub fn cast_handle<T>(handle: Handle) -> &'static mut T {
    assert_ne!(handle, 0, "Invalid handle value");

    resource_manager::check_handle::<T>(handle);

    let ptr = handle as *mut T;
    unsafe { &mut *ptr }
}

// Constructs `Box` from raw pointer and immediately drops it.
pub fn drop_handle<T: 'static>(env: &JNIEnv, handle: Handle) {
    let res = panic::catch_unwind(|| unsafe {
        resource_manager::remove_handle::<T>(handle);
        Box::from_raw(handle as *mut T);
    });
    exception::unwrap_exc_or_default(env, res);
}

#[cfg(test)]
mod tests {
    use super::*;

    #[cfg(not(feature = "resource-manager"))]
    #[test]
    fn cast_simple_object() {
        static VALUE: i32 = 0;

        let mut object = Box::new(VALUE);
        let ptr = &mut *object as *mut i32;
        let casted = cast_handle::<i32>(ptr as jlong);
        assert_eq!(casted, &VALUE);
    }

    #[test]
    #[should_panic(expected = "Invalid handle value")]
    fn cast_zero_object() {
        let _ = cast_handle::<i32>(0);
    }
}
