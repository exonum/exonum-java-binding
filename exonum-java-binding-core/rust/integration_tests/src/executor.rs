use java_bindings::{JniExecutor, JniErrorKind};
use java_bindings::jni::JavaVM;
use java_bindings::jni::sys::jint;
use example_proxy::AtomicIntegerProxy;

use std::sync::{Arc, Barrier};
use std::thread::spawn;

/// Checks if detached native thread attaches and detaches as it should when calls to
/// `with_attached` appears to be nested. After the nested function call ends, thread should stay
/// attached, and after the outer one ends, normally thread should be detached
/// (an exception is `HackyExecutor`).
/// But this function doesn't check last condition, leaving this check to the user.
pub fn check_nested_attach<E: JniExecutor>(vm: &JavaVM, executor: E) {
    check_detached(vm);
    executor
        .with_attached(|_| {
            check_attached(vm);
            executor.with_attached(|_| {
                check_attached(vm);
                Ok(())
            })?;
            check_attached(vm);
            Ok(())
        })
        .unwrap();
}

pub fn check_attached(vm: &JavaVM) {
    assert!(is_attached(vm));
}

pub fn check_detached(vm: &JavaVM) {
    assert!(!is_attached(vm));
}

pub fn is_attached(vm: &JavaVM) -> bool {
    vm.get_env()
        .map(|_| true)
        .or_else(|jni_err| match jni_err.0 {
            JniErrorKind::ThreadDetached => Ok(false),
            _ => Err(jni_err),
        })
        .expect("An unexpected JNI error occurred")
}

pub fn test_executor<E: JniExecutor>(executor: E) {
    let mut atomic = AtomicIntegerProxy::new(executor, 0).unwrap();
    assert_eq!(0, atomic.get().unwrap());
    assert_eq!(1, atomic.increment_and_get().unwrap());
    assert_eq!(3, atomic.add_and_get(2).unwrap());
    assert_eq!(3, atomic.get().unwrap());
}

pub fn test_executor_in_another_thread<E: JniExecutor + 'static>(executor: E) {
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

pub fn test_executor_in_concurrent_threads<E: JniExecutor + 'static>(
    executor: E,
    thread_num: usize,
) {
    const ITERS_PER_THREAD: usize = 10_000;

    let mut atomic = AtomicIntegerProxy::new(executor.clone(), 0).unwrap();
    let barrier = Arc::new(Barrier::new(thread_num));
    let mut threads = Vec::new();

    for _ in 0..thread_num {
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
    let expected = (ITERS_PER_THREAD * thread_num) as jint;
    assert_eq!(expected, atomic.get().unwrap());
}
