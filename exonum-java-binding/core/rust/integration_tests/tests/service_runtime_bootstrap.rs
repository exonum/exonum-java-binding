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

use integration_tests::vm::{fakes_classpath, java_library_path, log4j_path};
use java_bindings::exonum::runtime::Runtime;
use java_bindings::{
    create_java_vm, create_service_runtime, Executor, InternalConfig, JvmConfig, RuntimeConfig,
};

use exonum_testkit::TestKitBuilder;
use std::{path::PathBuf, sync::Arc};

#[test]
// Fails on Java 12. Ignored until [ECR-3133] is fixed because the cause of the issue also prevents
// the execution of system tests.
#[ignore]
fn bootstrap() {
    let jvm_config = JvmConfig {
        args_prepend: vec![],
        args_append: vec![],
        jvm_debug_socket: None,
    };

    let runtime_config = RuntimeConfig {
        artifacts_path: PathBuf::from("/tmp/"),
        // Pass log4j path to avoid error messages of mis-configuration
        log_config_path: log4j_path(),
        port: 6300,
        override_system_lib_path: None,
    };

    let internal_config = InternalConfig {
        system_class_path: fakes_classpath(),
        system_lib_path: java_library_path(),
    };

    let java_vm = create_java_vm(&jvm_config, &runtime_config, internal_config);
    let executor = Executor::new(Arc::new(java_vm));

    let runtime = create_service_runtime(executor, &runtime_config);

    let mut testkit = TestKitBuilder::validator()
        .with_additional_runtime(runtime)
        .create();
    testkit.create_block();
}
