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

extern crate exonum_testkit;
extern crate integration_tests;
extern crate java_bindings;

use exonum_testkit::TestKitBuilder;
use integration_tests::vm::get_fakes_classpath;
use java_bindings::{Config, InternalConfig, JavaServiceRuntime, JvmConfig, PrivateConfig, PublicConfig};

const TEST_SERVICE_MODULE_NAME: &str =
    "com.exonum.binding.fakes.services.service.TestServiceModule";

#[test]
// TODO: reenable this test after ECR-2789
#[cfg_attr(target_os = "linux", ignore)]
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

    let jvm_config = JvmConfig {
        args_prepend: Vec::new(),
        args_append: Vec::new(),
        jvm_debug_socket: None,
    };

    let service_runtime = JavaServiceRuntime::get_or_create(
        Config {
            public_config,
            private_config,
            jvm_config,
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
