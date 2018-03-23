extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;

use integration_tests::mock::service::{ServiceMockBuilder, SERVICE_ID, SERVICE_NAME};
use integration_tests::mock::transaction::{create_mock_transaction, INFO_VALUE};
use integration_tests::test_service::{create_test_map, create_test_service, INITIAL_ENTRY_KEY,
                                      INITIAL_ENTRY_VALUE};
use integration_tests::vm::create_vm_for_tests_with_fake_classes;
use java_bindings::DumbExecutor;
use java_bindings::exonum::blockchain::Service;
use java_bindings::exonum::crypto::hash;
use java_bindings::exonum::encoding::Error as MessageError;
use java_bindings::exonum::messages::RawTransaction;
use java_bindings::exonum::storage::{Database, MemoryDB};
use java_bindings::jni::JavaVM;
use java_bindings::serde_json::Value;

use std::sync::Arc;

lazy_static! {
    pub static ref VM: Arc<JavaVM> = Arc::new(create_vm_for_tests_with_fake_classes());
    pub static ref EXECUTOR: DumbExecutor = DumbExecutor { vm: VM.clone() };
}

const EXCEPTION_CLASS: &str = "java/lang/Exception";
const OOM_ERROR_CLASS: &str = "java/lang/OutOfMemoryError";

const TEST_CONFIG_JSON: &str = r#""test config""#;
lazy_static! {
    static ref TEST_CONFIG_VALUE: Value = Value::String("test config".to_string());
}


#[test]
pub fn service_id() {
    let executor = DumbExecutor { vm: VM.clone() };
    let service = ServiceMockBuilder::new(executor).build();
    assert_eq!(SERVICE_ID, service.service_id());
}

#[test]
pub fn service_name() {
    let executor = DumbExecutor { vm: VM.clone() };
    let service = ServiceMockBuilder::new(executor).build();
    assert_eq!(SERVICE_NAME, service.service_name());
}

#[test]
pub fn state_hash() {
    let db = MemoryDB::new();
    let snapshot = db.snapshot();
    let hashes = [hash(&[1]), hash(&[2]), hash(&[3])];
    let service = ServiceMockBuilder::new(EXECUTOR.clone())
        .state_hashes(&hashes)
        .build();
    assert_eq!(&hashes, service.state_hash(&*snapshot).as_slice());
}

#[test]
pub fn tx_from_raw() {
    let (java_transaction, raw_message) = create_mock_transaction(EXECUTOR.clone(), true);
    let service = ServiceMockBuilder::new(EXECUTOR.clone())
        .convert_transaction(java_transaction)
        .build();
    let decoded_transaction = service.tx_from_raw(raw_message).expect("Failed to get transaction");
    assert_eq!(decoded_transaction.serialize_field().unwrap(), Value::String(INFO_VALUE.into()));
}

#[test]
#[should_panic(expected="Java exception: java.lang.OutOfMemoryError")]
pub fn tx_from_raw_should_panic_if_java_error_occurred() {
    let raw = RawTransaction::from_vec(vec![]);
    let service = ServiceMockBuilder::new(EXECUTOR.clone())
        .convert_transaction_throwing(OOM_ERROR_CLASS)
        .build();
    service.tx_from_raw(raw).unwrap();
}

#[test]
pub fn tx_from_raw_should_return_err_if_java_exception_occurred() {
    let raw = RawTransaction::from_vec(vec![]);
    let service = ServiceMockBuilder::new(EXECUTOR.clone())
        .convert_transaction_throwing(EXCEPTION_CLASS)
        .build();
    let err = service.tx_from_raw(raw)
        .expect_err("This transaction should be de-serialized with an error!");
    if let MessageError::Basic(ref s) = err {
        assert!(s.starts_with("Java exception: java.lang.Exception"));
    } else {
        panic!("Unexpected error message {:#?}", err);
    }
}

#[test]
pub fn initialize_config() {
    let db = MemoryDB::new();
    let mut fork = db.fork();

    let service = ServiceMockBuilder::new(EXECUTOR.clone())
        .initial_global_config(TEST_CONFIG_JSON.to_string())
        .build();

    let config = service.initialize(&mut fork);
    assert_eq!(config, *TEST_CONFIG_VALUE);
}

#[test]
#[should_panic(expected="Java exception: java.lang.Exception")]
pub fn initialize_should_panic_if_java_exception_occurred() {
    let db = MemoryDB::new();
    let mut fork = db.fork();

    let service = ServiceMockBuilder::new(EXECUTOR.clone())
        .initial_global_config_throwing(EXCEPTION_CLASS)
        .build();

    service.initialize(&mut fork);
}

#[test]
pub fn test_service_initialize() {
    let db = MemoryDB::new();
    let service = create_test_service(EXECUTOR.clone());
    {
        let mut fork = db.fork();
        service.initialize(&mut fork);
        db.merge(fork.into_patch()).expect("Failed to merge changes");
    }
    let snapshot = db.snapshot();
    let test_map = create_test_map(&*snapshot, service.service_name());
    let key = hash(INITIAL_ENTRY_KEY.as_ref());
    assert_eq!(Some(INITIAL_ENTRY_VALUE.to_string()), test_map.get(&key));
}
