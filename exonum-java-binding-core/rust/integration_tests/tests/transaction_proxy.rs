extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;

use integration_tests::{
    mock::transaction::{
        create_mock_transaction_proxy, create_throwing_exec_exception_mock_transaction_proxy,
        create_throwing_mock_transaction_proxy, ENTRY_NAME, ENTRY_VALUE, INFO_VALUE,
    },
    vm::create_vm_for_tests_with_fake_classes,
};

use java_bindings::{
    exonum::{
        blockchain::{Transaction, TransactionContext, TransactionError, TransactionErrorType},
        encoding::serialize::json::ExonumJson,
        messages::{Message, RawTransaction, ServiceTransaction},
        storage::{Database, Entry, Fork, MemoryDB, Snapshot},
    },
    jni::JavaVM,
    MainExecutor,
};

use std::sync::Arc;

const ARITHMETIC_EXCEPTION_CLASS: &str = "java/lang/ArithmeticException";
const OOM_ERROR_CLASS: &str = "java/lang/OutOfMemoryError";

lazy_static! {
    static ref VM: Arc<JavaVM> = create_vm_for_tests_with_fake_classes();
    pub static ref EXECUTOR: MainExecutor = MainExecutor::new(VM.clone());
}

#[test]
fn execute_valid_transaction() {
    let db = MemoryDB::new();
    {
        let snapshot = db.snapshot();
        let entry = create_entry(&*snapshot);
        assert_eq!(None, entry.get());
    }
    {
        let mut fork = db.fork();
        let valid_tx = create_mock_transaction_proxy(EXECUTOR.clone(), true);
        {
            let tx_context = create_transaction_context(&mut fork);
            valid_tx
                .execute(tx_context)
                .map_err(TransactionError::from)
                .unwrap_or_else(|err| {
                    panic!(
                        "Execution error: {:?}; {}",
                        err.error_type(),
                        err.description().unwrap_or_default()
                    )
                });
        }
        db.merge(fork.into_patch())
            .expect("Failed to merge transaction");
    }
    // Check the transaction has successfully written the expected value into the entry index.
    let snapshot = db.snapshot();
    let entry = create_entry(&*snapshot);
    assert_eq!(Some(String::from(ENTRY_VALUE)), entry.get());
}

#[test]
#[should_panic(expected = "Java exception: java.lang.OutOfMemoryError")]
fn execute_should_panic_if_java_error_occurred() {
    let panic_tx = create_throwing_mock_transaction_proxy(EXECUTOR.clone(), OOM_ERROR_CLASS);
    let db = MemoryDB::new();
    let mut fork = db.fork();
    let tx_context = create_transaction_context(&mut fork);
    panic_tx.execute(tx_context).unwrap();
}

#[test]
#[should_panic(expected = "Java exception: java.lang.ArithmeticException")]
fn execute_should_panic_if_java_exception_occurred() {
    let panic_tx =
        create_throwing_mock_transaction_proxy(EXECUTOR.clone(), ARITHMETIC_EXCEPTION_CLASS);
    let db = MemoryDB::new();
    let mut fork = db.fork();
    let tx_context = create_transaction_context(&mut fork);
    panic_tx.execute(tx_context).unwrap();
}

#[test]
fn execute_should_return_err_if_tx_exec_exception_occurred() {
    let err_code: i8 = 1;
    let err_message = "Expected exception";
    let invalid_tx = create_throwing_exec_exception_mock_transaction_proxy(
        EXECUTOR.clone(),
        false,
        err_code,
        Some(err_message),
    );
    let db = MemoryDB::new();
    let mut fork = db.fork();
    let tx_context = create_transaction_context(&mut fork);
    let err = invalid_tx
        .execute(tx_context)
        .map_err(TransactionError::from)
        .expect_err("This transaction should be executed with an error!");
    assert_eq!(err.error_type(), TransactionErrorType::Code(err_code as u8));
    assert!(err.description().unwrap().starts_with(err_message));
}

#[test]
fn execute_should_return_err_if_tx_exec_exception_subclass_occurred() {
    let err_code: i8 = 2;
    let err_message = "Expected exception subclass";
    let invalid_tx = create_throwing_exec_exception_mock_transaction_proxy(
        EXECUTOR.clone(),
        true,
        err_code,
        Some(err_message),
    );
    let db = MemoryDB::new();
    let mut fork = db.fork();
    let tx_context = create_transaction_context(&mut fork);
    let err = invalid_tx
        .execute(tx_context)
        .map_err(TransactionError::from)
        .expect_err("This transaction should be executed with an error!");
    assert_eq!(err.error_type(), TransactionErrorType::Code(err_code as u8));
    assert!(err.description().unwrap().starts_with(err_message));
}

#[test]
fn execute_should_return_err_if_tx_exec_exception_occurred_no_message() {
    let err_code: i8 = 3;
    let invalid_tx = create_throwing_exec_exception_mock_transaction_proxy(
        EXECUTOR.clone(),
        false,
        err_code,
        None,
    );
    let db = MemoryDB::new();
    let mut fork = db.fork();
    let tx_context = create_transaction_context(&mut fork);
    let err = invalid_tx
        .execute(tx_context)
        .map_err(TransactionError::from)
        .expect_err("This transaction should be executed with an error!");
    assert_eq!(err.error_type(), TransactionErrorType::Code(err_code as u8));
    assert!(err.description().is_none());
}

#[test]
fn execute_should_return_err_if_tx_exec_exception_subclass_occurred_no_message() {
    let err_code: i8 = 4;
    let invalid_tx = create_throwing_exec_exception_mock_transaction_proxy(
        EXECUTOR.clone(),
        true,
        err_code,
        None,
    );
    let db = MemoryDB::new();
    let mut fork = db.fork();
    let tx_context = create_transaction_context(&mut fork);
    let err = invalid_tx
        .execute(tx_context)
        .map_err(TransactionError::from)
        .expect_err("This transaction should be executed with an error!");
    assert_eq!(err.error_type(), TransactionErrorType::Code(err_code as u8));
    assert!(err.description().is_none());
}

fn create_entry<V>(view: V) -> Entry<V, String>
where
    V: AsRef<Snapshot + 'static>,
{
    Entry::new(ENTRY_NAME, view)
}

fn create_transaction_context(fork: &mut Fork) -> TransactionContext {
    let service_transaction = ServiceTransaction::from_raw_unchecked(0, vec![]);
    let (pk, sk) = java_bindings::exonum::crypto::gen_keypair();
    let signed_transaction =
        java_bindings::exonum::messages::Message::sign_transaction(service_transaction, 0, pk, &sk);
    TransactionContext::new(fork, &signed_transaction)
}
