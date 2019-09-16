/*
 * Copyright 2019 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//! Wrappers and helper functions around Java pointers. Used for memory management
//! between native and Java.

use jni::sys::jlong;
use jni::JNIEnv;

use std::marker::PhantomData;
use std::panic;

pub mod resource_manager;
use super::utils::unwrap_exc_or_default;

/// Raw pointer passed to and from Java-side.
pub type Handle = jlong;

/// Wrapper for a non-owned handle.
///
/// Created handle must not outlive the reference it was created with.
///
/// Calls `handle.resource_manager::unregister_handle` in the `Drop` implementation.
pub struct NonOwnedHandle<T: 'static> {
    handle: Handle,
    is_mutable: bool,
    handle_type: PhantomData<T>,
}

impl<T> NonOwnedHandle<T> {
    /// Converts reference to a new immutable `NonOwnedHandle`.
    ///
    /// Created `NonOwnedHandle` must not outlive provided reference.
    pub fn new(reference: &T) -> Self {
        let handle = value as *const T as Handle;
        resource_manager::register_handle::<T>(handle);
        Self {
            handle,
            is_mutable: false,
            handle_type: PhantomData,
        }
    }

    /// Converts reference to a new mutable `NonOwnedHandle`.
    ///
    /// Created `NonOwnedHandle` must not outlive provided reference.
    pub fn new_mut(value: &mut T) -> Self {
        let handle = value as *mut T as Handle;
        resource_manager::register_handle::<T>(handle);
        Self {
            handle,
            is_mutable: true,
            handle_type: PhantomData,
        }
    }

    /// Returns a reference which was used to construct a handle.
    pub fn get(&self) -> &T {
        cast_handle(self.handle)
    }

    /// Returns a mutable reference which was used to construct a handle.
    ///
    /// Panics if `NonOwnedHandle` was created from immutable reference.
    pub fn get_mut(&mut self) -> &mut T {
        if !self.is_mutable {
            panic!("Attempt to access mutable reference from immutable non-owned handle");
        }
        cast_handle(self.handle)
    }

    /// Returns underlying `Handle` value.
    pub fn as_handle(&self) -> Handle {
        self.handle
    }
}

impl<T> Drop for NonOwnedHandle<T> {
    fn drop(&mut self) {
        resource_manager::unregister_handle::<T>(self.handle);
    }
}

/// Returns a handle (a raw pointer) to the given Java-owned object allocated in the heap. This
/// handle must be freed by the `drop_handle` function call.
pub fn to_handle<T: 'static>(val: T) -> Handle {
    let handle = Box::into_raw(Box::new(val)) as Handle;
    resource_manager::add_handle::<T>(handle);
    handle
}

/// "Converts" a handle to the object reference.
///
/// # Panics
///
/// Panics if the handle is equal to zero.
///
/// # Notes
///
/// Additional validity checks are performed if "resource-manager" feature is enabled.
pub fn cast_handle<T>(handle: Handle) -> &'static mut T {
    assert_ne!(handle, 0, "Invalid handle value");

    resource_manager::check_handle::<T>(handle);

    let ptr = handle as *mut T;
    unsafe { &mut *ptr }
}

/// Converts a handle into an owned value.
/// The ownership of the object is transferred from Java to Rust and Rust side is
/// responsible for cleaning.
///
/// # Panics
///
/// Panics if the handle is not valid, or if it identifies a native-owned object.
pub fn acquire_handle_ownership<T: 'static>(handle: Handle) -> Box<T> {
    resource_manager::remove_handle::<T>(handle);
    unsafe { Box::from_raw(handle as *mut T) }
}

/// Destroys the Java-owned native object identified by the given handle.
///
/// # Panics
///
/// Panics if the handle is not valid, or if it identifies a native-owned object.
pub fn drop_handle<T: 'static>(env: &JNIEnv, handle: Handle) {
    let res = panic::catch_unwind(|| unsafe {
        resource_manager::remove_handle::<T>(handle);
        Box::from_raw(handle as *mut T);
        Ok(())
    });
    unwrap_exc_or_default(env, res);
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

    #[test]
    #[should_panic(expected = "Attempt to access mutable reference from immutable")]
    fn mutable_ref_from_immutable_throws_error() {
        let i = &mut 0;
        let mut handle = NonOwnedHandle::new(i);

        let _ = handle.get_mut();
    }
}
