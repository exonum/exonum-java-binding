use jni::sys::{jlong, jboolean};

use std::panic;

pub fn cast_object<T>(object: jlong) -> &'static mut T {
    let ptr = object as *mut T;
    unsafe { &mut *ptr }
}

// Constructs `Box` from raw pointer and immediately drops it. Returns false if panic occurs.
pub fn drop_object<T>(object: jlong) -> jboolean {
    panic::catch_unwind(|| unsafe {
                            Box::from_raw(object as *mut T);
                        })
            .is_ok() as jboolean
}
