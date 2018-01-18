/// See actual implementation (`imp.rs`) for the documentation.

use utils::Handle;

pub fn add_handle<T: 'static>(_: Handle) {}
pub fn remove_handle<T: 'static>(_: Handle) {}
pub fn register_handle<T: 'static>(_: Handle) {}
pub fn unregister_handle<T: 'static>(_: Handle) {}
pub fn check_handle<T: 'static>(_: Handle, _: bool) {}
pub fn known_handles() -> usize {
    0
}
