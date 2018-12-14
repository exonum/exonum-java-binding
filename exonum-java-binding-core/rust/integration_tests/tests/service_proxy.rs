extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;

use integration_tests::mock::service::ServiceMockBuilder;
use integration_tests::mock::transaction::create_empty_raw_transaction;
use integration_tests::test_service::{
    create_test_map, create_test_service, INITIAL_ENTRY_KEY, INITIAL_ENTRY_VALUE,
};
use integration_tests::vm::create_vm_for_tests_with_fake_classes;
use java_bindings::exonum::blockchain::Service;
use java_bindings::exonum::crypto::hash;
use java_bindings::exonum::storage::{Database, MemoryDB};
use java_bindings::jni::JavaVM;
use java_bindings::serde_json::Value;
use java_bindings::utils::any_to_string;
use java_bindings::MainExecutor;
use std::panic::{catch_unwind, AssertUnwindSafe};
use std::sync::Arc;

lazy_static! {
    static ref VM: Arc<JavaVM> = create_vm_for_tests_with_fake_classes();
    pub static ref EXECUTOR: MainExecutor = MainExecutor::new(VM.clone());
}

const EXCEPTION_CLASS: &str = "java/lang/RuntimeException";
const OOM_ERROR_CLASS: &str = "java/lang/OutOfMemoryError";

const TEST_CONFIG_JSON: &str = r#""test config""#;
const TEST_CONFIG_NOT_JSON: &str = r#"()"#;

lazy_static! {
    static ref TEST_CONFIG_VALUE: Value = Value::String("test config".to_string());
}

#[test]
fn service_id() {
    let service_id: u16 = 24;
    let service = ServiceMockBuilder::new(EXECUTOR.clone())
        .id(service_id)
        .build();
    assert_eq!(service_id, service.service_id());
}

#[test]
fn service_id_negative() {
    // Check that value is converted between rust `u16` and java `short` without loss.
    let service_id: u16 = -24_i16 as u16; // 65512;
    let service = ServiceMockBuilder::new(EXECUTOR.clone())
        .id(service_id)
        .build();
    assert_eq!(service_id, service.service_id());
}

#[test]
fn service_name() {
    let service_name: &str = "test_service";
    let service = ServiceMockBuilder::new(EXECUTOR.clone())
        .name(service_name)
        .build();
    assert_eq!(service_name, service.service_name());
}

#[test]
fn state_hash() {
    let db = MemoryDB::new();
    let snapshot = db.snapshot();
    let hashes = [hash(&[1]), hash(&[2]), hash(&[3])];
    let service = ServiceMockBuilder::new(EXECUTOR.clone())
        .state_hashes(&hashes)
        .build();
    assert_eq!(&hashes, service.state_hash(&*snapshot).as_slice());
}

// FIXME: deserialize to protobuf and compare with protobuf buffer
/*
#[test]
fn tx_from_raw() {
    let (java_transaction, raw_message) = create_mock_transaction(&EXECUTOR, true);
    let service = ServiceMockBuilder::new(EXECUTOR.clone())
        .convert_transaction(java_transaction)
        .build();
    let executable_transaction = service
        .tx_from_raw(raw_message)
        .expect("Failed to convert transaction");
    assert_eq!(
        executable_transaction.serialize_field().unwrap(),
        *INFO_VALUE
    );
}
*/

#[test]
#[should_panic(expected = "Java exception: java.lang.OutOfMemoryError")]
fn tx_from_raw_should_panic_if_java_error_occurred() {
    let raw = create_empty_raw_transaction();
    let service = ServiceMockBuilder::new(EXECUTOR.clone())
        .convert_transaction_throwing(OOM_ERROR_CLASS)
        .build();
    service.tx_from_raw(raw).unwrap();
}

#[test]
fn tx_from_raw_should_return_err_if_java_exception_occurred() {
    let raw = create_empty_raw_transaction();
    let service = ServiceMockBuilder::new(EXECUTOR.clone())
        .convert_transaction_throwing(EXCEPTION_CLASS)
        .build();
    let err = service
        .tx_from_raw(raw)
        .expect_err("This transaction should be de-serialized with an error!");
    assert!(err
        .to_string()
        .starts_with("Java exception: java.lang.RuntimeException"));
}

#[test]
fn initialize_config() {
    let db = MemoryDB::new();
    let mut fork = db.fork();

    let service = ServiceMockBuilder::new(EXECUTOR.clone())
        .initial_global_config(TEST_CONFIG_JSON.to_string())
        .build();

    let config = service.initialize(&mut fork);
    assert_eq!(config, *TEST_CONFIG_VALUE);
}

#[test]
fn initialize_config_null() {
    let db = MemoryDB::new();
    let mut fork = db.fork();

    let service = ServiceMockBuilder::new(EXECUTOR.clone())
        .initial_global_config(None)
        .build();

    let config = service.initialize(&mut fork);
    assert_eq!(config, Value::Null);
}

#[test]
fn initialize_config_parse_error() {
    let db = MemoryDB::new();
    let mut fork = db.fork();

    let service = ServiceMockBuilder::new(EXECUTOR.clone())
        .initial_global_config(TEST_CONFIG_NOT_JSON.to_string())
        .build();

    match catch_unwind(AssertUnwindSafe(|| service.initialize(&mut fork))) {
        Ok(_config) => panic!("This test should panic"),
        Err(ref e) => {
            let error = any_to_string(e);
            assert!(error.starts_with("JSON deserialization error: "));
            assert!(error.ends_with(r#"; json string: "()""#));
        }
    };
}

#[test]
#[should_panic(expected = "Java exception: java.lang.RuntimeException")]
fn initialize_should_panic_if_java_exception_occurred() {
    let db = MemoryDB::new();
    let mut fork = db.fork();

    let service = ServiceMockBuilder::new(EXECUTOR.clone())
        .initial_global_config_throwing(EXCEPTION_CLASS)
        .build();

    service.initialize(&mut fork);
}

#[test]
fn service_can_modify_db_on_initialize() {
    let db = MemoryDB::new();
    let service = create_test_service(EXECUTOR.clone());
    {
        let mut fork = db.fork();
        service.initialize(&mut fork);
        db.merge(fork.into_patch())
            .expect("Failed to merge changes");
    }
    // Check that the Java service implementation has successfully written the initial value
    // into the storage.
    let snapshot = db.snapshot();
    let test_map = create_test_map(&*snapshot, service.service_name());
    let key = hash(INITIAL_ENTRY_KEY.as_ref());
    let value = test_map
        .get(&key)
        .expect("Failed to find the entry created in the test service");
    assert_eq!(INITIAL_ENTRY_VALUE, value);
}
