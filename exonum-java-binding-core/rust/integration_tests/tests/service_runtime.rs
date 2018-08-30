extern crate exonum_testkit;
extern crate integration_tests;
extern crate java_bindings;

use exonum_testkit::TestKitBuilder;
use integration_tests::vm::get_fakes_classpath;
use java_bindings::{Config, InternalConfig, JavaServiceRuntime, PrivateConfig, PublicConfig};

const TEST_SERVICE_MODULE_NAME: &str =
    "com.exonum.binding.fakes.services.service.TestServiceModule";

#[test]
fn bootstrap() {
    let public_config = PublicConfig {
        module_name: TEST_SERVICE_MODULE_NAME.to_owned(),
    };

    let private_config = PrivateConfig {
        user_parameters: Vec::new(),
        service_class_path: "".to_string(),
        log_config_path: "".to_owned(),
        port: 6000,
    };

    let service_runtime = JavaServiceRuntime::get_or_create(
        Config {
            public_config,
            private_config,
        },
        InternalConfig {
            system_class_path: get_fakes_classpath(),
            system_lib_path: None,
        },
    );

    let mut testkit = TestKitBuilder::validator()
        .with_service(service_runtime.service_proxy())
        .create();

    testkit.create_block();
}
