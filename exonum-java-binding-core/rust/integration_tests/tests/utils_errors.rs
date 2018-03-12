extern crate java_bindings;
#[macro_use]
extern crate lazy_static;

mod util;

use java_bindings::{DumbExecutor, Executor};
use java_bindings::jni::{JNIEnv, JavaVM};
use java_bindings::jni::errors::{ErrorKind, Result as JNIResult};
use java_bindings::utils::{check_error_on_exception, get_and_clear_java_exception, get_class_name,
                           panic_on_exception};

use std::sync::Arc;

use util::create_vm_for_tests;

const ERROR_CLASS: &str = "java/lang/Error";
const OOM_ERROR_CLASS: &str = "java/lang/OutOfMemoryError";
const EXCEPTION_CLASS: &str = "java/lang/Exception";
const ARITHMETIC_EXCEPTION_CLASS: &str = "java/lang/ArithmeticException";
const ARITHMETIC_EXCEPTION_CLASS_FQN: &str = "java.lang.ArithmeticException";

lazy_static! {
    pub static ref VM: Arc<JavaVM> = Arc::new(create_vm_for_tests());
    pub static ref EXECUTOR: DumbExecutor = DumbExecutor { vm: VM.clone() };
}

#[test]
#[should_panic(expected="Java exception: java.lang.Exception")]
fn panic_on_exception_catch_exact_class() {
    EXECUTOR.with_attached(|env: &JNIEnv| {
        Ok(panic_on_exception(env, throw(env, EXCEPTION_CLASS)))
    })
        .unwrap();
}

#[test]
#[should_panic(expected="Java exception: java.lang.ArithmeticException")]
fn panic_on_exception_catch_subclass() {
    EXECUTOR.with_attached(|env: &JNIEnv| {
        Ok(panic_on_exception(env, throw(env, ARITHMETIC_EXCEPTION_CLASS)))
    })
        .unwrap();
}

#[test]
#[should_panic(expected="JNI error: ")]
fn panic_on_exception_catch_jni_error() {
    EXECUTOR.with_attached(|env: &JNIEnv| {
        Ok(panic_on_exception(env, make_jni_error()))
    })
        .unwrap();
}

#[test]
fn panic_on_exception_dont_catch_good_result() {
    EXECUTOR.with_attached(|env: &JNIEnv| {
        Ok(panic_on_exception(env, Ok(())))
    })
        .unwrap();
}

#[test]
#[should_panic(expected="Java exception: java.lang.Error")]
fn check_error_on_exception_catch_javaerror_exact_class() {
    EXECUTOR.with_attached(|env: &JNIEnv| {
        Ok(check_error_on_exception(env, throw(env, ERROR_CLASS)))
    })
        .unwrap().unwrap();
}

#[test]
#[should_panic(expected="Java exception: java.lang.OutOfMemoryError")]
fn check_error_on_exception_catch_javaerror_subclass() {
    EXECUTOR.with_attached(|env: &JNIEnv| {
        Ok(check_error_on_exception(env, throw(env, OOM_ERROR_CLASS)))
    })
        .unwrap().unwrap();
}

#[test]
fn check_error_on_exception_catch_javaexception_exact_class() {
    EXECUTOR.with_attached(|env: &JNIEnv| {
        Ok(check_error_on_exception(env, throw(env, EXCEPTION_CLASS))
            .map_err(|e| assert!(e.starts_with("Java exception: java.lang.Exception")))
            .expect_err("An exception should lead to an error"))
    })
        .unwrap();
}

#[test]
fn check_error_on_exception_catch_javaexception_subclass() {
    EXECUTOR.with_attached(|env: &JNIEnv| {
        Ok(check_error_on_exception(env, throw(env, ARITHMETIC_EXCEPTION_CLASS))
            .map_err(|e| assert!(e.starts_with("Java exception: java.lang.ArithmeticException")))
            .expect_err("An exception should lead to an error"))
    })
        .unwrap();
}

#[test]
#[should_panic(expected="JNI error: ")]
fn check_error_on_exception_catch_jni_error() {
    EXECUTOR.with_attached(|env: &JNIEnv| {
        Ok(check_error_on_exception(env, make_jni_error()))
    })
        .unwrap().unwrap();
}

#[test]
fn check_error_on_exception_dont_catch_good_result() {
    EXECUTOR.with_attached(|env: &JNIEnv| {
        Ok(check_error_on_exception(env, Ok(())))
    })
        .unwrap().unwrap();
}

#[test]
fn get_and_clear_java_exception_valid_object() {
    EXECUTOR.with_attached(|env: &JNIEnv| {
        // Error should be handled, not raised
        throw(env, ARITHMETIC_EXCEPTION_CLASS).unwrap_err();
        let exception = get_and_clear_java_exception(env);
        assert_eq!(get_class_name(env, exception)?, ARITHMETIC_EXCEPTION_CLASS_FQN);
        Ok(())
    })
        .unwrap();
}

#[test]
#[should_panic(expected="Exception object is null")]
fn get_and_clear_java_exception_null() {
    EXECUTOR.with_attached(|env: &JNIEnv| {
        // Error should be handled, not raised
        throw(env, ARITHMETIC_EXCEPTION_CLASS).unwrap_err();
        Ok(())
    })
        .unwrap();
    EXECUTOR.with_attached(|env: &JNIEnv| {
        let _exception = get_and_clear_java_exception(env);
        Ok(())
    })
        .unwrap();
}

fn throw(env: &JNIEnv, e: &str) -> JNIResult<()> {
    env.throw((e, ""))?;
    Err(ErrorKind::JavaException.into())
}

fn make_jni_error() -> JNIResult<()> {
    Err(ErrorKind::Msg("Custom test error".to_string()).into())
}
