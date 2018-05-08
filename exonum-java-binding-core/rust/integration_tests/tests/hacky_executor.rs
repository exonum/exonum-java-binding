extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;

use integration_tests::executor::{check_attached, check_nested_attach, test_executor,
                                  test_executor_in_another_thread,
                                  test_executor_in_concurrent_threads};
use integration_tests::vm::create_vm_for_tests;
use java_bindings::HackyExecutor;
use java_bindings::jni::JavaVM;

lazy_static! {
    pub static ref VM: JavaVM = create_vm_for_tests();
}

#[test]
pub fn it_works() {
    let executor = HackyExecutor::new(&VM, 1);
    test_executor(executor);
}

#[test]
pub fn it_works_in_another_thread() {
    let executor = HackyExecutor::new(&VM, 2);
    test_executor_in_another_thread(executor);
}

#[test]
pub fn it_works_in_concurrent_threads() {
    const THREAD_NUM: usize = 8;
    let executor = HackyExecutor::new(&VM, THREAD_NUM + 1);
    test_executor_in_concurrent_threads(executor, THREAD_NUM)
}

#[test]
pub fn nested_attach() {
    let executor = HackyExecutor::new(&VM, 1);
    test_nested_attach(&VM, executor);
}

fn test_nested_attach<E: JniExecutor>(vm: &JavaVM, executor: E) {
    check_nested_attach(vm, executor);
    check_attached(vm);
}
