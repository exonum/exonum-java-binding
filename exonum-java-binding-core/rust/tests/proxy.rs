extern crate jni;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;

use java_bindings::DumbExecutor;
use jni::JavaVM;
use jni::sys::jint;

use std::sync::{Arc, Barrier};
use std::thread::spawn;

mod example_proxy;
use example_proxy::*;

#[path="../src/test_util.rs"]
mod test_util;
use test_util::create_vm;

lazy_static! {
    pub static ref VM: Arc<JavaVM> = Arc::new(create_vm(true));
}

#[test]
pub fn it_works() {
    let executor = DumbExecutor { vm: VM.clone() };
    let mut atomic = AtomicIntegerProxy::new(executor, 0).unwrap();
    assert_eq!(0, atomic.get().unwrap());
    assert_eq!(1, atomic.increment_and_get().unwrap());
    assert_eq!(3, atomic.add_and_get(2).unwrap());
    assert_eq!(3, atomic.get().unwrap());
}

#[test]
pub fn it_works_in_another_thread() {
    let executor = DumbExecutor { vm: VM.clone() };
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

    let executor = DumbExecutor { vm: VM.clone() };
    let mut atomic = AtomicIntegerProxy::new(executor, 0).unwrap();
    let barrier = Arc::new(Barrier::new(THREAD_NUM));
    let mut threads = Vec::new();

    for _ in 0..THREAD_NUM {
        let barrier = barrier.clone();
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
