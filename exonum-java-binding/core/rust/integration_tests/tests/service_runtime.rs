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
use integration_tests::vm::{get_fake_service_artifact_path, get_fakes_classpath};
use java_bindings::{Config, InternalConfig, JavaServiceRuntime, JvmConfig, RuntimeConfig};

#[test]
fn bootstrap() {
    let artifact_path = get_fake_service_artifact_path();

    let jvm_config = JvmConfig {
        args_prepend: vec![],
        args_append: vec![],
        jvm_debug_socket: None,
    };

    let runtime_config = RuntimeConfig {
        log_config_path: "".to_owned(),
        port: 6300,
    };

    let config = Config {
        jvm_config,
        runtime_config,
    };

    let internal_config = InternalConfig {
        system_class_path: get_fakes_classpath(),
        system_lib_path: None,
    };

    let service_runtime = JavaServiceRuntime::new(config, internal_config);

    let artifact_id = service_runtime.load_artifact(&artifact_path);
    let service = service_runtime.create_service(&artifact_id);

    let mut testkit = TestKitBuilder::validator().with_service(service).create();

    testkit.create_block();
}
