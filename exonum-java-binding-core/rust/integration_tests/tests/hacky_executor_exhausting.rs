extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;

use integration_tests::vm::create_vm_for_tests;
use java_bindings::{JniExecutor, HackyExecutor};
use java_bindings::jni::JavaVM;

use std::thread::spawn;

lazy_static! {
    pub static ref VM: JavaVM = create_vm_for_tests();
}

#[test]
#[should_panic(expected = "The limit on thread attachment is exhausted (limit is 3)")]
fn exhausted_thread_limit() {
    const THREAD_NUM: usize = 4;

    let executor = HackyExecutor::new(&VM, THREAD_NUM - 1);
    for _ in 0..THREAD_NUM - 1 {
        let executor = executor.clone();
        let jh = spawn(move || executor.with_attached(|_| Ok(())).unwrap());
        jh.join().unwrap();
    }
    executor.with_attached(|_| Ok(())).unwrap();
}
