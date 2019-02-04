/*
 * Copyright 2019 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;

use integration_tests::{
    mock::transaction::{
        create_mock_transaction_proxy, create_throwing_exec_exception_mock_transaction_proxy,
        create_throwing_mock_transaction_proxy, AUTHOR_PK_ENTRY_NAME, ENTRY_VALUE, INFO_VALUE,
        TEST_ENTRY_NAME, TX_HASH_ENTRY_NAME,
    },
    vm::create_vm_for_tests_with_fake_classes,
};

use java_bindings::{
    exonum::{
        blockchain::{Transaction, TransactionContext, TransactionError, TransactionErrorType},
        crypto::{Hash, PublicKey},
        messages::RawTransaction,
        storage::{Database, Entry, Fork, MemoryDB, Snapshot},
    },
    jni::JavaVM,
    serde_json, MainExecutor,
};

use std::sync::Arc;

const ARITHMETIC_EXCEPTION_CLASS: &str = "java/lang/ArithmeticException";
const OOM_ERROR_CLASS: &str = "java/lang/OutOfMemoryError";

lazy_static! {
    static ref VM: Arc<JavaVM> = create_vm_for_tests_with_fake_classes();
    pub static ref EXECUTOR: MainExecutor = MainExecutor::new(VM.clone());
}

#[test]
// TODO: reenable these tests after ECR-2789
#[ignore]
fn execute_valid_transaction() {
    let db = MemoryDB::new();
    {
        let snapshot = db.snapshot();
        let entry = create_test_entry(&*snapshot);
        assert_eq!(None, entry.get());
    }
    {
        let mut fork = db.fork();
        let (valid_tx, raw) = create_mock_transaction_proxy(EXECUTOR.clone());
        {
            let tx_context = create_transaction_context(&mut fork, raw);
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
    let entry = create_test_entry(&*snapshot);
    assert_eq!(Some(String::from(ENTRY_VALUE)), entry.get());
}

#[test]
#[ignore]
#[should_panic(expected = "Java exception: java.lang.OutOfMemoryError")]
fn execute_should_panic_if_java_error_occurred() {
    let (panic_tx, raw) = create_throwing_mock_transaction_proxy(EXECUTOR.clone(), OOM_ERROR_CLASS);
    let db = MemoryDB::new();
    let mut fork = db.fork();
    let tx_context = create_transaction_context(&mut fork, raw);
    panic_tx.execute(tx_context).unwrap();
}

#[test]
#[ignore]
#[should_panic(expected = "Java exception: java.lang.ArithmeticException")]
fn execute_should_panic_if_java_exception_occurred() {
    let (panic_tx, raw) =
        create_throwing_mock_transaction_proxy(EXECUTOR.clone(), ARITHMETIC_EXCEPTION_CLASS);
    let db = MemoryDB::new();
    let mut fork = db.fork();
    let tx_context = create_transaction_context(&mut fork, raw);
    panic_tx.execute(tx_context).unwrap();
}

#[test]
#[ignore]
fn execute_should_return_err_if_tx_exec_exception_occurred() {
    let err_code: i8 = 1;
    let err_message = "Expected exception";
    let (invalid_tx, raw) = create_throwing_exec_exception_mock_transaction_proxy(
        EXECUTOR.clone(),
        false,
        err_code,
        Some(err_message),
    );
    let db = MemoryDB::new();
    let mut fork = db.fork();
    let tx_context = create_transaction_context(&mut fork, raw);
    let err = invalid_tx
        .execute(tx_context)
        .map_err(TransactionError::from)
        .expect_err("This transaction should be executed with an error!");
    assert_eq!(err.error_type(), TransactionErrorType::Code(err_code as u8));
    assert!(err.description().unwrap().starts_with(err_message));
}

#[test]
#[ignore]
fn execute_should_return_err_if_tx_exec_exception_subclass_occurred() {
    let err_code: i8 = 2;
    let err_message = "Expected exception subclass";
    let (invalid_tx, raw) = create_throwing_exec_exception_mock_transaction_proxy(
        EXECUTOR.clone(),
        true,
        err_code,
        Some(err_message),
    );
    let db = MemoryDB::new();
    let mut fork = db.fork();
    let tx_context = create_transaction_context(&mut fork, raw);
    let err = invalid_tx
        .execute(tx_context)
        .map_err(TransactionError::from)
        .expect_err("This transaction should be executed with an error!");
    assert_eq!(err.error_type(), TransactionErrorType::Code(err_code as u8));
    assert!(err.description().unwrap().starts_with(err_message));
}

#[test]
#[ignore]
fn execute_should_return_err_if_tx_exec_exception_occurred_no_message() {
    let err_code: i8 = 3;
    let (invalid_tx, raw) = create_throwing_exec_exception_mock_transaction_proxy(
        EXECUTOR.clone(),
        false,
        err_code,
        None,
    );
    let db = MemoryDB::new();
    let mut fork = db.fork();
    let tx_context = create_transaction_context(&mut fork, raw);
    let err = invalid_tx
        .execute(tx_context)
        .map_err(TransactionError::from)
        .expect_err("This transaction should be executed with an error!");
    assert_eq!(err.error_type(), TransactionErrorType::Code(err_code as u8));
    assert!(err.description().is_none());
}

#[test]
#[ignore]
fn execute_should_return_err_if_tx_exec_exception_subclass_occurred_no_message() {
    let err_code: i8 = 4;
    let (invalid_tx, raw) = create_throwing_exec_exception_mock_transaction_proxy(
        EXECUTOR.clone(),
        true,
        err_code,
        None,
    );
    let db = MemoryDB::new();
    let mut fork = db.fork();
    let tx_context = create_transaction_context(&mut fork, raw);
    let err = invalid_tx
        .execute(tx_context)
        .map_err(TransactionError::from)
        .expect_err("This transaction should be executed with an error!");
    assert_eq!(err.error_type(), TransactionErrorType::Code(err_code as u8));
    assert!(err.description().is_none());
}

#[test]
fn json_serialize() {
    let valid_tx = create_mock_transaction_proxy(EXECUTOR.clone());
    assert_eq!(serde_json::to_value(&valid_tx.0).unwrap(), *INFO_VALUE);
}

#[test]
#[should_panic(expected = "Java exception: java.lang.OutOfMemoryError")]
fn json_serialize_should_panic_if_java_error_occurred() {
    let panic_tx = create_throwing_mock_transaction_proxy(EXECUTOR.clone(), OOM_ERROR_CLASS);
    serde_json::to_string(&panic_tx.0).unwrap();
}

#[test]
fn json_serialize_should_return_exception_details_if_java_exception_occurred() {
    let invalid_tx =
        create_throwing_mock_transaction_proxy(EXECUTOR.clone(), ARITHMETIC_EXCEPTION_CLASS);
    let value = serde_json::to_value(&invalid_tx.0).unwrap();
    let err_msg = value.as_str().unwrap();
    assert!(err_msg.starts_with("Java exception: java.lang.ArithmeticException"));
}

#[test]
#[ignore]
fn passing_transaction_context() {
    let db = MemoryDB::new();
    let (tx_hash, author_pk) = {
        let mut fork = db.fork();
        let (valid_tx, raw) = create_mock_transaction_proxy(EXECUTOR.clone());
        // get transaction hash and author public key from mock transaction
        let (tx_hash, author_pk) = {
            let context = create_transaction_context(&mut fork, raw);
            let (tx_hash, author_pk) = (context.tx_hash(), context.author());

            // execute transaction
            valid_tx.execute(context).unwrap();

            (tx_hash, author_pk)
        };
        db.merge(fork.into_patch()).unwrap();
        (tx_hash, author_pk)
    };
    let snapshot = db.snapshot();
    let tx_hash_entry: Entry<_, Hash> = Entry::new(TX_HASH_ENTRY_NAME, &snapshot);
    assert_eq!(tx_hash_entry.get().unwrap(), tx_hash);
    let author_pk_entry: Entry<_, PublicKey> = Entry::new(AUTHOR_PK_ENTRY_NAME, &snapshot);
    assert_eq!(author_pk_entry.get().unwrap(), author_pk);
}

fn create_test_entry<V>(view: V) -> Entry<V, String>
where
    V: AsRef<Snapshot + 'static>,
{
    Entry::new(TEST_ENTRY_NAME, view)
}

fn create_transaction_context(fork: &mut Fork, raw: RawTransaction) -> TransactionContext {
    let (service_id, service_transaction) = (raw.service_id(), raw.service_transaction());
    let (pk, sk) = java_bindings::exonum::crypto::gen_keypair();
    let signed_transaction = java_bindings::exonum::messages::Message::sign_transaction(
        service_transaction,
        service_id,
        pk,
        &sk,
    );
    TransactionContext::new(fork, &signed_transaction)
}
