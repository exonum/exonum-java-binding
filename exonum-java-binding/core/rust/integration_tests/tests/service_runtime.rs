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
use java_bindings::{Config, JavaServiceRuntime, JvmConfig, RuntimeConfig, ServiceConfig};

#[test]
// TODO: reenable this test after ECR-2789
#[cfg_attr(target_os = "linux", ignore)]
fn bootstrap() {
    let service_config = ServiceConfig {
        artifact_uri: "".to_owned(),
    };

    let jvm_config = JvmConfig {
        args_prepend: Vec::new(),
        args_append: Vec::new(),
        jvm_debug_socket: None,
    };

    let runtime_config = RuntimeConfig {
        log_config_path: "".to_owned(),
        port: 6300,
        system_class_path: get_fakes_classpath(),
        system_lib_path: None,
    };

    let config = Config {
        service_config,
        jvm_config,
        runtime_config,
    };

    let service_runtime = JavaServiceRuntime::get_or_create(config.clone());

    let artifact_id = service_runtime
        .load_artifact(&config.service_config.artifact_uri)
        .expect("Unable to load artifact");
    let service = service_runtime.create_service(&artifact_id);

    let mut testkit = TestKitBuilder::validator().with_service(service).create();

    testkit.create_block();
}
