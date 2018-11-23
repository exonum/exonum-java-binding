extern crate exonum_testkit;
extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;
#[macro_use]
extern crate serde_derive;

use std::{
    panic::{catch_unwind, AssertUnwindSafe},
    sync::Arc,
};

use exonum_testkit::TestKitBuilder;

use integration_tests::{
    mock::{
        service::ServiceMockBuilder,
        transaction::{create_mock_transaction, INFO_VALUE},
    },
    test_service::{create_test_map, create_test_service, INITIAL_ENTRY_KEY, INITIAL_ENTRY_VALUE},
    vm::create_vm_for_tests_with_fake_classes,
};
use java_bindings::{
    exonum::{
        blockchain::Service,
        crypto::hash,
        encoding::Error as MessageError,
        messages::RawTransaction,
        storage::{Database, MemoryDB},
    },
    jni::{objects::JObject, JavaVM},
    serde_json,
    utils::{any_to_string, convert_to_string, unwrap_jni},
    JniExecutor, MainExecutor,
};

lazy_static! {
    static ref VM: Arc<JavaVM> = create_vm_for_tests_with_fake_classes();
    pub static ref EXECUTOR: MainExecutor = MainExecutor::new(VM.clone());
}

const EXCEPTION_CLASS: &str = "java/lang/RuntimeException";
const TEST_EXCEPTION_CLASS: &str = "com/exonum/binding/fakes/mocks/TestException";
const OOM_ERROR_CLASS: &str = "java/lang/OutOfMemoryError";

const TEST_CONFIG_JSON: &str = r#""test config""#;
const TEST_CONFIG_NOT_JSON: &str = r#"()"#;

lazy_static! {
    static ref TEST_CONFIG_VALUE: serde_json::Value =
        serde_json::Value::String("test config".to_string());
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

#[test]
#[should_panic(expected = "Java exception: java.lang.OutOfMemoryError")]
fn tx_from_raw_should_panic_if_java_error_occurred() {
    let raw = RawTransaction::from_vec(vec![]);
    let service = ServiceMockBuilder::new(EXECUTOR.clone())
        .convert_transaction_throwing(OOM_ERROR_CLASS)
        .build();
    service.tx_from_raw(raw).unwrap();
}

#[test]
fn tx_from_raw_should_return_err_if_java_exception_occurred() {
    let raw = RawTransaction::from_vec(vec![]);
    let service = ServiceMockBuilder::new(EXECUTOR.clone())
        .convert_transaction_throwing(EXCEPTION_CLASS)
        .build();
    let err = service
        .tx_from_raw(raw)
        .expect_err("This transaction should be de-serialized with an error!");
    if let MessageError::Basic(ref s) = err {
        assert!(s.starts_with("Java exception: java.lang.RuntimeException"));
    } else {
        panic!("Unexpected error message {:#?}", err);
    }
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
    assert_eq!(config, serde_json::Value::Null);
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

#[test]
#[should_panic(expected = "Java exception: com.exonum.binding.fakes.mocks.TestException")]
fn after_commit_throwing() {
    let service = ServiceMockBuilder::new(EXECUTOR.clone())
        .after_commit_throwing(TEST_EXCEPTION_CLASS)
        .build();

    // It turned out that it is MUCH easier to use testkit in order to trigger the after_commit()
    // callback than calling it by hands providing manually constructed ServiceContext entity.
    let mut testkit = TestKitBuilder::validator()
        .with_service(service.clone())
        .create();

    testkit.create_block();
}

#[test]
fn after_commit_validator() {
    let (builder, interactor) = ServiceMockBuilder::new(EXECUTOR.clone())
        .get_mock_interaction_after_commit();

    let service = builder.build();
    let mut testkit = TestKitBuilder::validator()
        .with_service(service.clone())
        .create();

    testkit.create_block();
    testkit.create_block();

    let result = get_mock_interaction_result(&EXECUTOR, interactor.as_obj());
    let after_commit_args: Vec<AfterCommitArgs> = serde_json::from_str(&result).unwrap();

    assert_eq!(after_commit_args.len(), 2);

    let item: &AfterCommitArgs = &after_commit_args[0];
    assert_ne!(item.handle, 0);
    assert_eq!(item.validator, 0);
    assert_eq!(item.height, 1);

    let item: &AfterCommitArgs = &after_commit_args[1];
    assert_ne!(item.handle, 0);
    assert_eq!(item.validator, 0);
    assert_eq!(item.height, 2);
}

#[test]
fn after_commit_auditor() {
    let (builder, interactor) = ServiceMockBuilder::new(EXECUTOR.clone())
        .get_mock_interaction_after_commit();

    let service = builder.build();
    let mut testkit = TestKitBuilder::auditor()
        .with_service(service.clone())
        .create();

    testkit.create_block();

    let result = get_mock_interaction_result(&EXECUTOR, interactor.as_obj());
    let after_commit_args: Vec<AfterCommitArgs> = serde_json::from_str(&result).unwrap();

    assert_eq!(after_commit_args.len(), 1);

    let item: &AfterCommitArgs = &after_commit_args[0];
    assert_ne!(item.handle, 0);
    assert_eq!(item.validator, -1);
    assert_eq!(item.height, 1);
}

// Helper methods. Gets the JSON representation of interaction with mock.
fn get_mock_interaction_result(exec: &MainExecutor, obj: JObject) -> String {
    unwrap_jni(exec.with_attached(|env| {
        env.call_method(obj, "getInteractions", "()Ljava/lang/String;", &[])?
            .l()
            .and_then(|obj| convert_to_string(env, obj))
    }))
}

#[derive(Serialize, Deserialize)]
struct AfterCommitArgs {
    handle: i64,
    validator: i32,
    height: i64,
}
