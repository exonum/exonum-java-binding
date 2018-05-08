extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;

use integration_tests::example_proxy::AtomicIntegerProxy;
use integration_tests::executor::check_nested_attach_hacky;
use integration_tests::vm::create_vm_for_tests;
use java_bindings::HackyExecutor;
use java_bindings::jni::JavaVM;
use java_bindings::jni::sys::jint;

use std::sync::{Arc, Barrier};
use std::thread::spawn;

lazy_static! {
    pub static ref VM: JavaVM = create_vm_for_tests();
    pub static ref HACKY_EXECUTOR: HackyExecutor = HackyExecutor::new(&VM, 13);
}

#[test]
pub fn it_works() {
    let executor = HackyExecutor::new(&VM, 1);
    let mut atomic = AtomicIntegerProxy::new(executor, 0).unwrap();
    assert_eq!(0, atomic.get().unwrap());
    assert_eq!(1, atomic.increment_and_get().unwrap());
    assert_eq!(3, atomic.add_and_get(2).unwrap());
    assert_eq!(3, atomic.get().unwrap());
}

#[test]
pub fn it_works_in_another_thread() {
    let executor = HackyExecutor::new(&VM, 2);
    let mut atomic = AtomicIntegerProxy::new(executor, 0).unwrap();
    assert_eq!(0, atomic.get().unwrap());
    let jh = spawn(move || {
        assert_eq!(1, atomic.increment_and_get().unwrap());
        assert_eq!(3, atomic.add_and_get(2).unwrap());
        atomic
    });
    let mut atomic = jh.join().unwrap();
    assert_eq!(3, atomic.get().unwrap());
}

#[test]
pub fn it_works_in_concurrent_threads() {
    const ITERS_PER_THREAD: usize = 10_000;
    const THREAD_NUM: usize = 8;

    let executor = HackyExecutor::new(&VM, THREAD_NUM + 1);
    let mut atomic = AtomicIntegerProxy::new(executor, 0).unwrap();
    let barrier = Arc::new(Barrier::new(THREAD_NUM));
    let mut threads = Vec::new();

    for _ in 0..THREAD_NUM {
        let barrier = Arc::clone(&barrier);
        let mut atomic = atomic.clone();
        let jh = spawn(move || {
            barrier.wait();
            for _ in 0..ITERS_PER_THREAD {
                atomic.increment_and_get().unwrap();
            }
        });
        threads.push(jh);
    }
    for jh in threads {
        jh.join().unwrap();
    }
    let expected = (ITERS_PER_THREAD * THREAD_NUM) as jint;
    assert_eq!(expected, atomic.get().unwrap());
}

#[test]
pub fn nested_attach() {
    let executor = HackyExecutor::new(&VM, 1);
    check_nested_attach_hacky(&VM, executor);
}
