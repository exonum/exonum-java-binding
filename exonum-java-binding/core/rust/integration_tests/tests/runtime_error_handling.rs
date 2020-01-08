extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;
#[macro_use]
extern crate exonum_derive;

use integration_tests::vm::create_vm_for_tests_with_classes;
use java_bindings::{
    exonum::{
        self,
        runtime::ExecutionError,
        runtime::{ErrorKind, ErrorMatch},
    },
    jni::{
        objects::{JObject, JThrowable},
        JNIEnv, JavaVM,
    },
    jni_call_default, jni_call_transaction, Error, Executor, JniResult,
};

use std::sync::Arc;

lazy_static! {
    static ref VM: Arc<JavaVM> = create_vm_for_tests_with_classes();
    pub static ref EXECUTOR: Executor = Executor::new(VM.clone());
}

const ARITHMETIC_EXCEPTION_CLASS: &str = "java/lang/ArithmeticException";
const ILLEGAL_ARGUMENT_EXCEPTION_CLASS: &str = "java/lang/IllegalArgumentException";
const TRANSACTION_EXECUTION_EXCEPTION_CLASS: &str =
    "com/exonum/binding/core/transaction/TransactionExecutionException";
const UNEXPECTED_TRANSACTION_EXECUTION_EXCEPTION_CLASS: &str =
    "com/exonum/binding/core/runtime/UnexpectedTransactionExecutionException";
const STRING_CLASS: &str = "java/lang/String";
const EXPECTED_DESCRIPTION: &str = "EXPECTED_DESCRIPTION";
const EXPECTED_CODE: u8 = FakeServiceError::Error as u8;

#[derive(Debug, ExecutionFail)]
enum FakeServiceError {
    Error = 10,
}

#[test]
fn jni_call_default_jni_error() {
    let expected = ErrorMatch::from_fail(&Error::JniError)
        .with_description_containing("Invalid constructor return type (must be void)");
    assert_jni_call_default_errors(&EXECUTOR, expected, |env| {
        // use invalid constructor signature to force JNI error
        env.new_object(STRING_CLASS, "()I", &[])?;
        Ok(())
    });
}

#[test]
fn jni_call_default_any_java_exception() {
    let expected = ErrorMatch::from_fail(&Error::JavaException)
        .with_description_containing(EXPECTED_DESCRIPTION);
    assert_jni_call_default_errors(&EXECUTOR, expected, |env| {
        env.throw_new(ARITHMETIC_EXCEPTION_CLASS, EXPECTED_DESCRIPTION)?;
        wait_for_thrown_exception(env)?;
        Ok(())
    });
}

#[test]
fn jni_call_default_illegal_argument_exception() {
    let expected = ErrorMatch::from_fail(&Error::IllegalArgument)
        .with_description_containing(EXPECTED_DESCRIPTION);
    assert_jni_call_default_errors(&EXECUTOR, expected, |env| {
        env.throw_new(ILLEGAL_ARGUMENT_EXCEPTION_CLASS, EXPECTED_DESCRIPTION)?;
        wait_for_thrown_exception(env)?;
        Ok(())
    });
}

#[test]
fn jni_call_transaction_jni_error() {
    let expected = ErrorMatch::from_fail(&Error::JniError)
        .with_description_containing("Invalid constructor return type (must be void)");
    assert_jni_call_transaction_errors(&EXECUTOR, expected, |env| {
        // use invalid constructor signature to force JNI error
        env.new_object(STRING_CLASS, "()I", &[])?;
        Ok(())
    });
}

#[test]
fn jni_call_transaction_any_java_exception() {
    let expected = ErrorMatch::from_fail(&Error::JavaException)
        .with_description_containing(EXPECTED_DESCRIPTION);
    assert_jni_call_transaction_errors(&EXECUTOR, expected, |env| {
        env.throw_new(ARITHMETIC_EXCEPTION_CLASS, EXPECTED_DESCRIPTION)?;
        wait_for_thrown_exception(env)?;
        Ok(())
    });
}

#[test]
fn jni_call_transaction_illegal_argument_exception() {
    let expected = ErrorMatch::from_fail(&Error::IllegalArgument)
        .with_description_containing(EXPECTED_DESCRIPTION);
    assert_jni_call_transaction_errors(&EXECUTOR, expected, |env| {
        env.throw_new(ILLEGAL_ARGUMENT_EXCEPTION_CLASS, EXPECTED_DESCRIPTION)?;
        wait_for_thrown_exception(env)?;
        Ok(())
    });
}

#[test]
fn jni_call_transaction_transaction_execution_exception() {
    let expected = ErrorMatch::from_fail(&FakeServiceError::Error)
        .with_description_containing(EXPECTED_DESCRIPTION);
    let err = jni_call_transaction(&EXECUTOR, |env| {
        let exception = create_transaction_execution_exception(env)?;
        env.throw(JThrowable::from(exception))?;
        wait_for_thrown_exception(env)?;
        Ok(())
    })
    .unwrap_err();
    assert_eq!(err, expected);
}

#[test]
fn jni_call_transaction_unexpected_transaction_execution_exception() {
    let err = jni_call_transaction(&EXECUTOR, |env| {
        let cause = create_any_java_exception(env)?;
        let exception = env.new_object(
            UNEXPECTED_TRANSACTION_EXECUTION_EXCEPTION_CLASS,
            "(Ljava/lang/Throwable;)V",
            &[cause.into()],
        )?;
        env.throw(JThrowable::from(exception))?;
        wait_for_thrown_exception(env)?;
        Ok(())
    })
    .unwrap_err();
    assert_eq!(err.kind(), ErrorKind::Unexpected);
    assert_eq!(err.description(), EXPECTED_DESCRIPTION);
}

fn create_transaction_execution_exception<'a>(env: &'a JNIEnv) -> JniResult<JObject<'a>> {
    let code = EXPECTED_CODE;
    let description: JObject = env.new_string(EXPECTED_DESCRIPTION)?.into();
    env.new_object(
        TRANSACTION_EXECUTION_EXCEPTION_CLASS,
        "(BLjava/lang/String;)V",
        &[code.into(), description.into()],
    )
}

fn create_any_java_exception<'a>(env: &'a JNIEnv) -> JniResult<JObject<'a>> {
    let description = JObject::from(env.new_string(EXPECTED_DESCRIPTION)?);
    env.new_object(
        ARITHMETIC_EXCEPTION_CLASS,
        "(Ljava/lang/String;)V",
        &[description.into()],
    )
}

fn assert_jni_call_default_errors<F, R>(executor: &Executor, expected_error: ErrorMatch, closure: F)
where
    F: FnOnce(&JNIEnv) -> JniResult<R>,
    R: std::fmt::Debug,
{
    let result = jni_call_default(executor, closure);
    assert_result_is_expected_error(expected_error, result);
}

fn assert_jni_call_transaction_errors<F, R>(
    executor: &Executor,
    expected_error: ErrorMatch,
    closure: F,
) where
    F: FnOnce(&JNIEnv) -> JniResult<R>,
    R: std::fmt::Debug,
{
    let result = jni_call_transaction(executor, closure);
    assert_result_is_expected_error(expected_error, result);
}

fn assert_result_is_expected_error<R>(expected_error: ErrorMatch, result: Result<R, ExecutionError>)
where
    R: std::fmt::Debug,
{
    assert!(result.is_err());
    let err = result.unwrap_err();
    assert_eq!(err, expected_error);
}

/// Performs some trivial action using JNI to wait for Java exception being thrown.
fn wait_for_thrown_exception<'a>(env: &'a JNIEnv) -> JniResult<JObject<'a>> {
    env.new_object(STRING_CLASS, "()V", &[])
}
