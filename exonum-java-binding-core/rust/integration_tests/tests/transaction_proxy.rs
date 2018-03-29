extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;

use integration_tests::mock::transaction::{create_mock_transaction_proxy,
                                           create_throwing_mock_transaction_proxy, ENTRY_NAME,
                                           ENTRY_VALUE, INFO_VALUE};
use integration_tests::vm::create_vm_for_tests_with_fake_classes;
use java_bindings::DumbExecutor;
use java_bindings::exonum::blockchain::Transaction;
use java_bindings::exonum::encoding::serialize::json::ExonumJson;
use java_bindings::exonum::storage::{Database, Entry, MemoryDB};
use java_bindings::jni::JavaVM;

use std::sync::Arc;

const ARITHMETIC_EXCEPTION_CLASS: &str = "java/lang/ArithmeticException";
const OOM_ERROR_CLASS: &str = "java/lang/OutOfMemoryError";

lazy_static! {
    pub static ref VM: Arc<JavaVM> = Arc::new(create_vm_for_tests_with_fake_classes());
    pub static ref EXECUTOR: DumbExecutor = DumbExecutor { vm: VM.clone() };
}

#[test]
pub fn verify_valid_transaction() {
    let valid_tx = create_mock_transaction_proxy(EXECUTOR.clone(), true);
    assert_eq!(true, valid_tx.verify());
}

#[test]
pub fn verify_invalid_transaction() {
    let invalid_tx = create_mock_transaction_proxy(EXECUTOR.clone(), false);
    assert_eq!(false, invalid_tx.verify());
}

#[test]
#[should_panic(expected = "Java exception: java.lang.ArithmeticException")]
pub fn verify_should_panic_if_java_exception_occured() {
    let panic_tx =
        create_throwing_mock_transaction_proxy(EXECUTOR.clone(), ARITHMETIC_EXCEPTION_CLASS);
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
        let valid_tx = create_mock_transaction_proxy(EXECUTOR.clone(), true);
        let result = valid_tx.execute(&mut fork);
        assert_eq!(result, ());
        db.merge(fork.into_patch()).expect(
            "Failed to merge transaction",
        );
    }
    // Check the transaction has successfully written the expected value into the entry index.
    let snapshot = db.snapshot();
    let entry = create_entry(&*snapshot);
    assert_eq!(Some(String::from(ENTRY_VALUE)), entry.get());
}

#[test]
#[should_panic(expected = "Java exception: java.lang.OutOfMemoryError")]
pub fn execute_should_panic_if_java_error_occurred() {
    let panic_tx = create_throwing_mock_transaction_proxy(EXECUTOR.clone(), OOM_ERROR_CLASS);
    let db = MemoryDB::new();
    let mut fork = db.fork();
    panic_tx.execute(&mut fork);
}

#[test]
// TODO Change behaviour to "return_err" with Exonum 0.6 [https://jira.bf.local/browse/ECR-912].
#[should_panic(expected = "Java exception: java.lang.ArithmeticException")]
pub fn execute_should_panic_if_java_exception_occurred() {
    let panic_tx =
        create_throwing_mock_transaction_proxy(EXECUTOR.clone(), ARITHMETIC_EXCEPTION_CLASS);
    let db = MemoryDB::new();
    let mut fork = db.fork();
    panic_tx.execute(&mut fork);
}

#[test]
pub fn json_serialize() {
    let valid_tx = create_mock_transaction_proxy(EXECUTOR.clone(), true);
    assert_eq!(valid_tx.serialize_field().unwrap(), *INFO_VALUE);
}

#[test]
#[should_panic(expected = "Java exception: java.lang.OutOfMemoryError")]
pub fn json_serialize_should_panic_if_java_error_occurred() {
    let panic_tx = create_throwing_mock_transaction_proxy(EXECUTOR.clone(), OOM_ERROR_CLASS);
    panic_tx.serialize_field().unwrap();
}

#[test]
pub fn json_serialize_should_return_err_if_java_exception_occurred() {
    let panic_tx =
        create_throwing_mock_transaction_proxy(EXECUTOR.clone(), ARITHMETIC_EXCEPTION_CLASS);
    let err = panic_tx.serialize_field().expect_err(
        "This transaction should be serialized with an error!",
    );
    assert!(err.description().starts_with(
        "Java exception: java.lang.ArithmeticException",
    ));
}

fn create_entry<V>(view: V) -> Entry<V, String> {
    Entry::new(ENTRY_NAME, view)
}
