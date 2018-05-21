extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;

use integration_tests::vm::create_vm_for_tests;
use java_bindings::{JniExecutor, JniErrorKind, JniResult, MainExecutor};
use java_bindings::jni::{JavaVM, JNIEnv};
use java_bindings::utils::{check_error_on_exception, get_and_clear_java_exception, get_class_name,
                           panic_on_exception};

use std::sync::Arc;

const ERROR_CLASS: &str = "java/lang/Error";
const OOM_ERROR_CLASS: &str = "java/lang/OutOfMemoryError";
const EXCEPTION_CLASS: &str = "java/lang/Exception";
const ARITHMETIC_EXCEPTION_CLASS: &str = "java/lang/ArithmeticException";
const ARITHMETIC_EXCEPTION_CLASS_FQN: &str = "java.lang.ArithmeticException";

lazy_static! {
    static ref VM: Arc<JavaVM> = create_vm_for_tests();
    pub static ref EXECUTOR: MainExecutor = MainExecutor::new(VM.clone());
}

#[test]
#[should_panic(expected = "Java exception: java.lang.Exception")]
fn panic_on_exception_catch_exact_class() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            Ok(panic_on_exception(env, throw(env, EXCEPTION_CLASS)))
        })
        .unwrap();
}

#[test]
#[should_panic(expected = "Java exception: java.lang.ArithmeticException")]
fn panic_on_exception_catch_subclass() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            Ok(panic_on_exception(
                env,
                throw(env, ARITHMETIC_EXCEPTION_CLASS),
            ))
        })
        .unwrap();
}

#[test]
#[should_panic(expected = "JNI error: ")]
fn panic_on_exception_catch_jni_error() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| Ok(panic_on_exception(env, make_jni_error())))
        .unwrap();
}

#[test]
fn panic_on_exception_dont_catch_good_result() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| Ok(panic_on_exception(env, Ok(()))))
        .unwrap();
}

#[test]
#[should_panic(expected = "Java exception: java.lang.Error")]
fn check_error_on_exception_catch_java_error_exact_class() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            Ok(check_error_on_exception(env, throw(env, ERROR_CLASS)))
        })
        .unwrap()
        .unwrap();
}

#[test]
#[should_panic(expected = "Java exception: java.lang.OutOfMemoryError")]
fn check_error_on_exception_catch_java_error_subclass() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            Ok(check_error_on_exception(env, throw(env, OOM_ERROR_CLASS)))
        })
        .unwrap()
        .unwrap();
}

#[test]
fn check_error_on_exception_catch_java_exception_exact_class() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            Ok(
                check_error_on_exception(env, throw(env, EXCEPTION_CLASS))
                    .map_err(|e| {
                        assert!(e.starts_with("Java exception: java.lang.Exception"))
                    })
                    .expect_err("An exception should lead to an error"),
            )
        })
        .unwrap();
}

#[test]
fn check_error_on_exception_catch_java_exception_subclass() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            Ok(
                check_error_on_exception(env, throw(env, ARITHMETIC_EXCEPTION_CLASS))
                    .map_err(|e| {
                        assert!(e.starts_with(
                            "Java exception: java.lang.ArithmeticException",
                        ))
                    })
                    .expect_err("An exception should lead to an error"),
            )
        })
        .unwrap();
}

#[test]
#[should_panic(expected = "JNI error: ")]
fn check_error_on_exception_catch_jni_error() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            Ok(check_error_on_exception(env, make_jni_error()))
        })
        .unwrap()
        .unwrap();
}

#[test]
fn check_error_on_exception_dont_catch_good_result() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| Ok(check_error_on_exception(env, Ok(()))))
        .unwrap()
        .unwrap();
}

#[test]
fn get_and_clear_java_exception_if_exception_occurred() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            // Error should be handled, not raised
            throw(env, ARITHMETIC_EXCEPTION_CLASS).unwrap_err();
            let exception = get_and_clear_java_exception(env);
            assert_eq!(
                get_class_name(env, exception)?,
                ARITHMETIC_EXCEPTION_CLASS_FQN
            );
            assert!(!env.exception_check()?);
            Ok(())
        })
        .unwrap();
}

#[test]
#[should_panic(expected = "No exception thrown")]
fn get_and_clear_java_exception_if_no_exception_occurred() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            let _exception = get_and_clear_java_exception(env);
            Ok(())
        })
        .unwrap();
}

fn throw(env: &JNIEnv, e: &str) -> JniResult<()> {
    env.throw((e, ""))?;
    Err(JniErrorKind::JavaException.into())
}

fn make_jni_error() -> JniResult<()> {
    Err(JniErrorKind::Msg("Custom test error".to_string()).into())
}
