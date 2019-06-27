extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;

use integration_tests::executor::check_nested_attach;
use integration_tests::vm::create_vm_for_tests;
use java_bindings::jni::JavaVM;
use java_bindings::Executor;

use std::sync::Arc;
use std::thread::spawn;

lazy_static! {
    pub static ref VM: Arc<JavaVM> = create_vm_for_tests();
    pub static ref EXECUTOR: Executor = Executor::new(VM.clone());
}

/// Checks if nested attaches are working properly and threads detach themselves
/// on exit.
#[test]
fn nested_attach() {
    assert_eq!(VM.threads_attached(), 0);
    let thread = spawn(|| {
        assert_eq!(VM.threads_attached(), 0);
        check_nested_attach(&VM, EXECUTOR.clone());
        assert_eq!(VM.threads_attached(), 1);
    });
    thread.join().unwrap();
    assert_eq!(VM.threads_attached(), 0);
}
