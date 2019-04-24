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

extern crate integration_tests;
extern crate java_bindings;

use integration_tests::vm::{fakes_classpath, log4j_path};
use java_bindings::{
    Config, InternalConfig, JavaServiceRuntime, JniExecutor, JvmConfig, RuntimeConfig,
};

#[test]
fn bootstrap() {
    let jvm_config = JvmConfig {
        args_prepend: vec![],
        args_append: vec![],
        jvm_debug_socket: None,
    };

    let runtime_config = RuntimeConfig {
        // Pass log4j path to avoid error messages of mis-configuration
        log_config_path: log4j_path(),
        port: 6300,
    };

    let config = Config {
        jvm_config,
        runtime_config,
    };

    let internal_config = InternalConfig {
        system_class_path: fakes_classpath(),
        system_lib_path: None,
    };

    let runtime = JavaServiceRuntime::new(config, internal_config);

    let result = runtime
        .get_executor()
        .with_attached(|env| env.get_version());

    assert!(result.is_ok());
    assert!(i32::from(result.unwrap()) > 0);
}
