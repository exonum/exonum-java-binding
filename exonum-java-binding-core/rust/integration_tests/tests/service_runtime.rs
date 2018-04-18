extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;

use java_bindings::jni::JavaVM;
use std::sync::Arc;
use java_bindings::{DumbExecutor, ServiceConfig, JavaServiceRuntime};
use java_bindings::exonum::helpers::fabric::NodeBuilder;
use integration_tests::vm::create_vm_for_tests_with_fake_classes;

lazy_static! {
    pub static ref VM: Arc<JavaVM> = Arc::new(create_vm_for_tests_with_fake_classes());
    pub static ref EXECUTOR: DumbExecutor = DumbExecutor { vm: VM.clone() };
}
const TEST_SERVICE_MODULE_NAME: &str = "com.exonum.binding.fakes.services.service.TestServiceModule";

#[test]
fn bootstrap() {
    let service_config = ServiceConfig {
        module_name: TEST_SERVICE_MODULE_NAME.to_owned(),
        port: 6300,
    };
    let service_runtime = JavaServiceRuntime::with_executor(EXECUTOR.clone(), service_config);

    let node_builder = NodeBuilder::new().with_service(Box::new(service_runtime));
}

