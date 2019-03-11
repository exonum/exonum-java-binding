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
use integration_tests::vm::{get_fakes_classpath, get_libpath};
use java_bindings::{Config, EjbConfig, JavaServiceRuntime, JvmConfig, ServiceConfig};

const TEST_SERVICE_MODULE_NAME: &str =
    "com.exonum.binding.fakes.services.service.TestServiceModule";

#[test]
#[ignore] //TODO: use the configuration below after ECR-2979
//// TODO: reenable this test after ECR-2789
//#[cfg_attr(target_os = "linux", ignore)]
fn bootstrap() {
    let service_config = ServiceConfig {
        module_name: TEST_SERVICE_MODULE_NAME.to_owned(),
        port: 6300,
    };

    let jvm_config = JvmConfig {
        args_prepend: Vec::new(),
        args_append: Vec::new(),
        jvm_debug_socket: None,
    };

    let ejb_config = EjbConfig {
        class_path: get_fakes_classpath(),
        lib_path: get_libpath(),
        log_config_path: "".to_owned(),
    };

    let service_runtime = JavaServiceRuntime::get_or_create(Config {
        jvm_config,
        ejb_config,
        service_config,
    });

    let service = service_runtime.create_service("", TEST_SERVICE_MODULE_NAME);

    let mut testkit = TestKitBuilder::validator().with_service(service).create();

    testkit.create_block();
}
