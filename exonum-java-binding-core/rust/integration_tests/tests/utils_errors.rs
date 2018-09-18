extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;

use integration_tests::vm::create_vm_for_tests_with_fake_classes;
use java_bindings::{
    jni::{
        objects::{JThrowable, JValue},
        sys::jbyte,
        JNIEnv, JavaVM,
    },
    utils::{
        check_error_on_exception, check_transaction_execution_result, get_and_clear_java_exception,
        get_class_name, get_exception_message, panic_on_exception,
    },
    JniErrorKind, JniExecutor, JniResult, MainExecutor,
};
use std::sync::Arc;

const ERROR_CLASS: &str = "java/lang/Error";
const OOM_ERROR_CLASS: &str = "java/lang/OutOfMemoryError";
const EXCEPTION_CLASS: &str = "java/lang/Exception";
const ARITHMETIC_EXCEPTION_CLASS: &str = "java/lang/ArithmeticException";
const ARITHMETIC_EXCEPTION_CLASS_FQN: &str = "java.lang.ArithmeticException";
const CUSTOM_EXCEPTION_MESSAGE: &str = "Test exception message";
const CLASS_TX_EXEC_EXCEPTION: &str =
    "com/exonum/binding/transaction/TransactionExecutionException";
const CLASS_TX_EXEC_EXCEPTION_SUBCLASS: &str = "com/exonum/binding/fakes/test/TestTxExecException";

lazy_static! {
    static ref VM: Arc<JavaVM> = create_vm_for_tests_with_fake_classes();
    pub static ref EXECUTOR: MainExecutor = MainExecutor::new(VM.clone());
}

#[test]
#[should_panic(expected = "Java exception: java.lang.Exception")]
fn panic_on_exception_catch_exact_class() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            panic_on_exception(env, throw(env, EXCEPTION_CLASS));
            Ok(())
        }).unwrap();
}

#[test]
#[should_panic(expected = "Java exception: java.lang.ArithmeticException")]
fn panic_on_exception_catch_subclass() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            panic_on_exception(env, throw(env, ARITHMETIC_EXCEPTION_CLASS));
            Ok(())
        }).unwrap();
}

#[test]
#[should_panic(expected = "JNI error: ")]
fn panic_on_exception_catch_jni_error() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            panic_on_exception(env, make_jni_error());
            Ok(())
        }).unwrap();
}

#[test]
fn panic_on_exception_dont_catch_good_result() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            panic_on_exception(env, Ok(()));
            Ok(())
        }).unwrap();
}

#[test]
#[should_panic(expected = "Java exception: java.lang.Error")]
fn check_error_on_exception_catch_java_error_exact_class() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| Ok(check_error_on_exception(env, throw(env, ERROR_CLASS))))
        .unwrap()
        .unwrap();
}

#[test]
#[should_panic(expected = "Java exception: java.lang.OutOfMemoryError")]
fn check_error_on_exception_catch_java_error_subclass() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            Ok(check_error_on_exception(env, throw(env, OOM_ERROR_CLASS)))
        }).unwrap()
        .unwrap();
}

#[test]
fn check_error_on_exception_catch_java_exception_exact_class() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            check_error_on_exception(env, throw(env, EXCEPTION_CLASS))
                .map_err(|e| assert!(e.starts_with("Java exception: java.lang.Exception")))
                .expect_err("An exception should lead to an error");
            Ok(())
        }).unwrap();
}

#[test]
fn check_error_on_exception_catch_java_exception_subclass() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            check_error_on_exception(env, throw(env, ARITHMETIC_EXCEPTION_CLASS))
                .map_err(|e| {
                    assert!(e.starts_with("Java exception: java.lang.ArithmeticException"))
                }).expect_err("An exception should lead to an error");
            Ok(())
        }).unwrap();
}

#[test]
#[should_panic(expected = "JNI error: ")]
fn check_error_on_exception_catch_jni_error() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| Ok(check_error_on_exception(env, make_jni_error())))
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
#[should_panic(expected = "Java exception: java.lang.Error")]
fn check_transaction_execution_result_panic_on_java_error_exact_class() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            Ok(check_transaction_execution_result(
                env,
                throw(env, ERROR_CLASS),
            ))
        }).unwrap()
        .unwrap();
}

#[test]
#[should_panic(expected = "Java exception: java.lang.OutOfMemoryError")]
fn check_transaction_execution_result_panic_on_java_error_subclass() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            Ok(check_transaction_execution_result(
                env,
                throw(env, OOM_ERROR_CLASS),
            ))
        }).unwrap()
        .unwrap();
}

#[test]
#[should_panic(expected = "Java exception: java.lang.Exception")]
fn check_transaction_execution_result_panic_on_java_exception_exact_class() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            Ok(check_transaction_execution_result(
                env,
                throw(env, EXCEPTION_CLASS),
            ))
        }).unwrap()
        .unwrap();
}

#[test]
#[should_panic(expected = "Java exception: java.lang.ArithmeticException")]
fn check_transaction_execution_result_panic_on_java_exception_subclass() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            Ok(check_transaction_execution_result(
                env,
                throw(env, ARITHMETIC_EXCEPTION_CLASS),
            ))
        }).unwrap()
        .unwrap();
}

#[test]
fn check_transaction_execution_result_catch_java_expected_exception_exact_class() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            check_transaction_execution_result(
                env,
                throw_tx_exec_exception(env, CLASS_TX_EXEC_EXCEPTION, 0),
            ).map_err(|e| {
                assert!(e.starts_with(
                    "Java exception: com.exonum.binding.transaction.TransactionExecutionException"
                ))
            }).expect_err("An exception should lead to an error");
            Ok(())
        }).unwrap();
}

#[test]
fn check_transaction_execution_result_catch_java_expected_exception_subclass() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            check_transaction_execution_result(
                env,
                throw_tx_exec_exception(env, CLASS_TX_EXEC_EXCEPTION_SUBCLASS, 0),
            ).map_err(|e| {
                assert!(e.starts_with(
                    "Java exception: com.exonum.binding.fakes.test.TestTxExecException"
                ))
            }).expect_err("An exception should lead to an error");
            Ok(())
        }).unwrap();
}

#[test]
#[should_panic(expected = "JNI error: ")]
fn check_transaction_execution_result_panic_on_jni_error() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| Ok(check_transaction_execution_result(env, make_jni_error())))
        .unwrap()
        .unwrap();
}

#[test]
fn check_transaction_execution_result_dont_catch_good_result() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| Ok(check_transaction_execution_result(env, Ok(()))))
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
        }).unwrap();
}

#[test]
fn get_exception_message_without_message() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            let exception = env.new_object(ARITHMETIC_EXCEPTION_CLASS, "()V", &[])?;
            assert_eq!(get_exception_message(env, exception)?, None);
            Ok(())
        }).unwrap();
}

#[test]
fn get_exception_message_with_message() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            throw_with_message(env, ARITHMETIC_EXCEPTION_CLASS, CUSTOM_EXCEPTION_MESSAGE)
                .unwrap_err();
            let exception = get_and_clear_java_exception(env);
            assert_eq!(
                get_exception_message(env, exception)?,
                Some(CUSTOM_EXCEPTION_MESSAGE.to_string())
            );
            Ok(())
        }).unwrap();
}

#[test]
#[should_panic(expected = "No exception thrown")]
fn get_and_clear_java_exception_if_no_exception_occurred() {
    EXECUTOR
        .with_attached(|env: &JNIEnv| {
            let _exception = get_and_clear_java_exception(env);
            Ok(())
        }).unwrap();
}

fn throw(env: &JNIEnv, exception_class: &str) -> JniResult<()> {
    // FIXME throw an exception without a stub message https://jira.bf.local/browse/ECR-1998
    env.throw((exception_class, ""))?;
    Err(JniErrorKind::JavaException.into())
}

fn throw_with_message(env: &JNIEnv, exception_class: &str, message: &str) -> JniResult<()> {
    env.throw((exception_class, message))?;
    Err(JniErrorKind::JavaException.into())
}

fn throw_tx_exec_exception(env: &JNIEnv, class: &str, err_code: jbyte) -> JniResult<()> {
    let ex: JThrowable = env
        .new_object(class, "(B)V", &[JValue::from(err_code)])?
        .into();
    env.throw(ex)?;
    Err(JniErrorKind::JavaException.into())
}

fn make_jni_error() -> JniResult<()> {
    Err(JniErrorKind::Msg("Custom test error".to_string()).into())
}
