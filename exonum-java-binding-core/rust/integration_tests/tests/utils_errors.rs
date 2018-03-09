extern crate java_bindings;
#[macro_use]
extern crate lazy_static;

mod util;

use java_bindings::{DumbExecutor, Executor};
use java_bindings::jni::{JNIEnv, JavaVM};
use java_bindings::jni::errors::{ErrorKind, Result as JNIResult};
use java_bindings::utils::panic_on_exception;

use std::sync::Arc;

use util::create_vm_for_tests;

const EXCEPTION_CLASS: &str = "java/lang/Exception";
const ARITHMETIC_EXCEPTION_CLASS: &str = "java/lang/ArithmeticException";

lazy_static! {
    pub static ref VM: Arc<JavaVM> = Arc::new(create_vm_for_tests());
    pub static ref EXECUTOR: DumbExecutor = DumbExecutor { vm: VM.clone() };
}

#[test]
#[should_panic(expected="Java exception: java.lang.Exception")]
fn catch_exact_class() {
    EXECUTOR.with_attached(|env: &JNIEnv| {
        Ok(panic_on_exception(env, throw(env, EXCEPTION_CLASS)))
    })
        .unwrap();
}

#[test]
#[should_panic(expected="Java exception: java.lang.ArithmeticException")]
fn catch_subclass() {
    EXECUTOR.with_attached(|env: &JNIEnv| {
        Ok(panic_on_exception(env, throw(env, ARITHMETIC_EXCEPTION_CLASS)))
    })
        .unwrap();
}

#[test]
fn dont_catch_without_exception() {
    EXECUTOR.with_attached(|env: &JNIEnv| {
        Ok(panic_on_exception(env, Ok(())))
    })
        .unwrap();
}

fn throw(env: &JNIEnv, e: &str) -> JNIResult<()> {
    env.throw((e, ""))?;
    Err(ErrorKind::JavaException.into())
}
