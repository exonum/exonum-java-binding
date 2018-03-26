use java_bindings::Executor;
use java_bindings::jni::JavaVM;
use java_bindings::jni::errors::ErrorKind;

pub fn call_recursively<E: Executor>(vm: &JavaVM, executor: E) {
    check_detached(vm);
    executor.with_attached(|_| {
        check_attached(vm);
        executor.with_attached(|_| {
            check_attached(vm);
            Ok(())
        })?;
        check_attached(vm);
        Ok(())
    })
        .unwrap();
    check_detached(vm);
}

fn check_attached(vm: &JavaVM) {
    assert!(is_attached(vm));
}

fn check_detached(vm: &JavaVM) {
    assert!(!is_attached(vm));
}

pub fn is_attached(vm: &JavaVM) -> bool {
    vm.get_env()
        .map(|_| true)
        .or_else(|jni_err| {
            match jni_err.0 {
                ErrorKind::ThreadDetached => Ok(false),
                _ => Err(jni_err),
            }
        })
        .expect("An unexpected JNI error occurred")
}
