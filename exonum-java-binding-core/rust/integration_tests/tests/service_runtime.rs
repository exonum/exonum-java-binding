extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;
extern crate exonum_testkit;

use java_bindings::jni::JavaVM;
use std::sync::Arc;
use java_bindings::{DumbExecutor, ServiceConfig, JvmConfig, Config, JavaServiceRuntime};
use java_bindings::exonum::helpers::fabric::NodeBuilder;
use integration_tests::vm::{create_vm_for_tests_with_fake_classes, get_classpath, get_libpath};
use exonum_testkit::{TestKit, TestKitBuilder};

const TEST_SERVICE_MODULE_NAME: &str = "com.exonum.binding.fakes.services.service.TestServiceModule";

#[test]
fn bootstrap() {
    let service_config = ServiceConfig {
        module_name: TEST_SERVICE_MODULE_NAME.to_owned(),
        port: 6300,
    };

    let jvm_config = JvmConfig {
        debug: true,
        class_path: get_classpath(),
        lib_path: get_libpath(),
        log_config_path: "".to_owned(),
    };

    let service_runtime = JavaServiceRuntime::new(Config {
        jvm_config,
        service_config,
    });

    let mut testkit = TestKitBuilder::validator().with_service(service_runtime.service_proxy()).create();

    testkit.create_block();
}

