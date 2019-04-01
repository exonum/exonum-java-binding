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
use integration_tests::vm::{get_fake_service_artifact_path, get_fakes_classpath, get_libpath};
use java_bindings::{Config, JavaServiceRuntime, JvmConfig, RuntimeConfig};

#[test]
fn bootstrap() {
    let artifact_path = get_fake_service_artifact_path();
    let system_class_path = get_fakes_classpath();
    let system_lib_path = get_libpath();
    let log_config_path = "".to_owned();

    let jvm_config = JvmConfig {
        args_prepend: Vec::new(),
        args_append: Vec::new(),
        jvm_debug_socket: None,
    };

    let runtime_config = RuntimeConfig {
        log_config_path,
        port: 6300,
        system_class_path,
        system_lib_path,
    };

    let config = Config {
        jvm_config,
        runtime_config,
    };

    let service_runtime = JavaServiceRuntime::new(config);

    let artifact_id = service_runtime.load_artifact(&artifact_path);
    let service = service_runtime.create_service(&artifact_id);

    let mut testkit = TestKitBuilder::validator().with_service(service).create();

    testkit.create_block();
}
