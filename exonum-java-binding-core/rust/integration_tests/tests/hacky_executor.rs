extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;

use std::sync::Arc;

use integration_tests::executor::{check_attached, check_nested_attach, test_concurrent_threads,
                                  test_serialized_threads, test_single_thread};
use integration_tests::vm::create_vm_for_tests;
use java_bindings::jni::JavaVM;
use java_bindings::HackyExecutor;

lazy_static! {
    pub static ref VM: Arc<JavaVM> = create_vm_for_tests();
}

#[test]
fn single_thread() {
    let executor = HackyExecutor::new(VM.clone(), 1);
    test_single_thread(executor);
}

#[test]
fn serialized_threads() {
    let executor = HackyExecutor::new(VM.clone(), 2);
    test_serialized_threads(executor);
}

#[test]
fn concurrent_threads() {
    const THREAD_NUM: usize = 8;
    let executor = HackyExecutor::new(VM.clone(), THREAD_NUM + 1);
    test_concurrent_threads(executor, THREAD_NUM)
}

#[test]
fn nested_attach() {
    let executor = HackyExecutor::new(VM.clone(), 1);
    check_nested_attach(&VM, executor);
    check_attached(&VM);
}
