extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;

use integration_tests::executor::{check_detached, check_nested_attach, test_executor,
                                  test_executor_in_another_thread,
                                  test_executor_in_concurrent_threads};
use integration_tests::vm::create_vm_for_tests;
use java_bindings::DumbExecutor;
use java_bindings::jni::JavaVM;

lazy_static! {
    pub static ref VM: JavaVM = create_vm_for_tests();
    pub static ref EXECUTOR: DumbExecutor = DumbExecutor::new(&VM);
}

#[test]
pub fn it_works() {
    test_executor(&*EXECUTOR);
}

#[test]
pub fn it_works_in_another_thread() {
    test_executor_in_another_thread(&*EXECUTOR);
}

#[test]
pub fn it_works_in_concurrent_threads() {
    const THREAD_NUM: usize = 8;
    test_executor_in_concurrent_threads(&*EXECUTOR, THREAD_NUM)
}

#[test]
pub fn nested_attach() {
    test_nested_attach(&VM, &*EXECUTOR);
}

fn test_nested_attach<E: JniExecutor>(vm: &JavaVM, executor: E) {
    check_nested_attach(vm, executor);
    check_detached(vm);
}
