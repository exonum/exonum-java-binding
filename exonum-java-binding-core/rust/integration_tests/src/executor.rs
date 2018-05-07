use java_bindings::{JniExecutor, JniErrorKind};
use java_bindings::jni::JavaVM;

/// Checks if detached native thread attaches and detaches as it should when calls to
/// `with_attached` appears to be nested. After the nested function call ends, thread should stay
/// attached, and after the outer one ends, thread should be detached
pub fn check_nested_attach_normal<E: JniExecutor>(vm: &JavaVM, executor: E) {
    check_nested_attach(vm, executor, true);
}

/// Same as `check_nested_attach_normal`, but in the end the thread should be attached,
/// since the "hacky" way is to never detach.
pub fn check_nested_attach_hacky<E: JniExecutor>(vm: &JavaVM, executor: E) {
    check_nested_attach(vm, executor, false);
}

fn check_nested_attach<E: JniExecutor>(vm: &JavaVM, executor: E, should_detach: bool) {
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
    if should_detach {
        check_detached(vm);
    } else {
        check_attached(vm);
    }
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
        .or_else(|jni_err| match jni_err.0 {
            JniErrorKind::ThreadDetached => Ok(false),
            _ => Err(jni_err),
        })
        .expect("An unexpected JNI error occurred")
}
