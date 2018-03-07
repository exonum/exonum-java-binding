extern crate java_bindings;
#[macro_use]
extern crate lazy_static;

mod util;

use java_bindings::{DumbExecutor, Executor, TransactionProxy};
use java_bindings::exonum::blockchain::Transaction;
use java_bindings::exonum::encoding::serialize::json::ExonumJson;
use java_bindings::exonum::messages::{MessageBuffer, RawMessage};
use java_bindings::exonum::storage::{Database, Entry, MemoryDB};
use java_bindings::jni::JavaVM;
use java_bindings::jni::objects::{AutoLocal, JObject, JValue};
use java_bindings::serde_json::Value;

use std::sync::Arc;

use util::create_vm_for_tests_with_fake_classes;

const ENTRY_NAME: &str = "test_entry";
const ENTRY_VALUE: &str = "test_value";
const INFO_JSON: &str = r#""test_info""#;
const INFO_VALUE: &str = r"test_info";

const TRANSACTION_ADAPTER_CLASS: &str =
    "com/exonum/binding/service/adapters/UserTransactionAdapter";
const NATIVE_FACADE_CLASS: &str = "com/exonum/binding/fakes/NativeFacade";

const ERROR_CLASS: &str = "java/lang/Error";
const ARITHMETIC_EXCEPTION_CLASS: &str = "java/lang/ArithmeticException";

lazy_static! {
    pub static ref VM: Arc<JavaVM> = Arc::new(create_vm_for_tests_with_fake_classes());
    pub static ref EXECUTOR: DumbExecutor = DumbExecutor { vm: VM.clone() };
}

#[test]
pub fn verify_valid_transaction() {
    let valid_tx = create_transaction_mock(EXECUTOR.clone(), true);
    assert_eq!(true, valid_tx.verify());
}

#[test]
pub fn verify_invalid_transaction() {
    let invalid_tx = create_transaction_mock(EXECUTOR.clone(), false);
    assert_eq!(false, invalid_tx.verify());
}

#[test]
#[should_panic(expected="Java exception: java.lang.ArithmeticException")]
pub fn verify_should_panic_if_java_exception_occured() {
    let panic_tx = create_throwing_mock(EXECUTOR.clone(), ARITHMETIC_EXCEPTION_CLASS);
    panic_tx.verify();
}

#[test]
pub fn execute_valid_transaction() {
    let db = MemoryDB::new();
    {
        let snapshot = db.snapshot();
        let entry = create_entry(&*snapshot);
        assert_eq!(None, entry.get());
    }
    {
        let mut fork = db.fork();
        let valid_tx = create_transaction_mock(EXECUTOR.clone(), true);
        let result = valid_tx.execute(&mut fork);
        assert_eq!(result, ());
        db.merge(fork.into_patch()).expect("Failed to merge transaction");
    }
    // Check the transaction has successfully written the expected value into the entry index.
    let snapshot = db.snapshot();
    let entry = create_entry(&*snapshot);
    assert_eq!(Some(String::from(ENTRY_VALUE)), entry.get());
}

#[test]
#[should_panic(expected="Java exception: java.lang.Error")]
pub fn execute_should_panic_if_java_error_occurred() {
    let panic_tx = create_throwing_mock(EXECUTOR.clone(), ERROR_CLASS);
    let db = MemoryDB::new();
    let mut fork = db.fork();
    panic_tx.execute(&mut fork);
}

#[test]
// TODO Change behaviour to "return_err" with Exonum 0.6 [https://jira.bf.local/browse/ECR-912].
#[should_panic(expected="Java exception: java.lang.ArithmeticException")]
pub fn execute_should_panic_if_java_exception_occurred() {
    let panic_tx = create_throwing_mock(EXECUTOR.clone(), ARITHMETIC_EXCEPTION_CLASS);
    let db = MemoryDB::new();
    let mut fork = db.fork();
    panic_tx.execute(&mut fork);
}

#[test]
pub fn json_serialize() {
    let valid_tx = create_transaction_mock(EXECUTOR.clone(), true);
    assert_eq!(valid_tx.serialize_field().unwrap(), Value::String(INFO_VALUE.into()));
}

#[test]
// This test expects that a fake Java transaction class will throw an exception from `info()`.
#[ignore]
#[should_panic(expected="Java exception: java.lang.Error")]
pub fn json_serialize_should_panic_if_java_error_occurred() {
    let panic_tx = create_throwing_mock(EXECUTOR.clone(), ERROR_CLASS);
    panic_tx.serialize_field().unwrap();
}

#[test]
// This test expects that a fake Java transaction class will throw an exception from `info()`.
#[ignore]
pub fn json_serialize_should_return_err_if_java_exception_occurred() {
    let panic_tx = create_throwing_mock(EXECUTOR.clone(), ARITHMETIC_EXCEPTION_CLASS);

    let err = panic_tx.serialize_field()
        .expect_err("This transaction should be serialized with an error!");
    // FIXME string representation of an error
    assert_eq!(err.description(), "");
}

fn create_transaction_mock<E: Executor>(executor: E, valid: bool) -> TransactionProxy<E> {
    let (java_tx_mock, raw) = executor.with_attached(|env| {
        let value = env.new_string(ENTRY_VALUE)?;
        let info = env.new_string(INFO_JSON)?;
        let java_tx_mock = env.call_static_method(
            NATIVE_FACADE_CLASS,
            "createTransaction",
            format!("(ZLjava/lang/String;Ljava/lang/String;)L{};", TRANSACTION_ADAPTER_CLASS),
            &[
                JValue::from(valid),
                JValue::from(JObject::from(value)),
                JValue::from(JObject::from(info))
            ],
        )?
            .l()?;
        let java_tx_mock = env.new_global_ref(AutoLocal::new(env, java_tx_mock).as_obj())?;
        // TODO remove this stub and get a real byte buffer
        let raw = RawMessage::new(MessageBuffer::from_vec(vec![]));
        Ok((java_tx_mock, raw))
    }).unwrap();

    unsafe { TransactionProxy::from_global_ref(executor, java_tx_mock, raw) }
}

fn create_throwing_mock<E: Executor>(executor: E, exception_class: &str) -> TransactionProxy<E> {
    let (java_tx_mock, raw) = executor.with_attached(|env| {
        let exception = env.find_class(exception_class)?;
        let java_tx_mock = env.call_static_method(
            NATIVE_FACADE_CLASS,
            "createThrowingTransaction",
            format!("(Ljava/lang/Class;)L{};", TRANSACTION_ADAPTER_CLASS),
            &[JValue::from(JObject::from(exception.into_inner()))],
        )?
            .l()?;
        let java_tx_mock = env.new_global_ref(AutoLocal::new(env, java_tx_mock).as_obj())?;
        let raw = RawMessage::new(MessageBuffer::from_vec(vec![]));
        Ok((java_tx_mock, raw))
    }).unwrap();

    unsafe { TransactionProxy::from_global_ref(executor, java_tx_mock, raw) }
}

fn create_entry<V>(view: V) -> Entry<V, String> {
    Entry::new(ENTRY_NAME, view)
}
