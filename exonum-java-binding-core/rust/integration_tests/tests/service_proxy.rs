extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;

use integration_tests::mock::service::ServiceMockBuilder;
use integration_tests::vm::create_vm_for_tests_with_fake_classes;
use java_bindings::exonum::blockchain::{Service, Transaction};
use java_bindings::exonum::crypto::Hash;
use java_bindings::exonum::storage::{Database, MemoryDB};
use java_bindings::{DumbExecutor, Executor, ServiceProxy, TransactionProxy};
use java_bindings::jni::{JavaVM, JNIEnv};
use java_bindings::jni::objects::{AutoLocal, JObject, JValue, GlobalRef};
use java_bindings::utils::unwrap_jni;

use std::sync::Arc;

lazy_static! {
    pub static ref VM: Arc<JavaVM> = Arc::new(create_vm_for_tests_with_fake_classes());
    pub static ref EXECUTOR: DumbExecutor = DumbExecutor { vm: VM.clone() };
}

#[test]
pub fn service_id() {
    let executor = DumbExecutor { vm: VM.clone() };
    let service = ServiceMockBuilder::new(executor)
        .id(42)
        .build();
    assert_eq!(42, service.service_id());
}
