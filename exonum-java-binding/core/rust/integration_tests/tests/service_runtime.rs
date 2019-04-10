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
#[macro_use]
extern crate lazy_static;

use exonum_testkit::TestKitBuilder;
use integration_tests::{
    fake_service::*,
    vm::{create_vm_for_tests_with_fake_classes, get_fake_service_artifact_path},
};
use java_bindings::{jni::JavaVM, JavaServiceRuntime};
use std::{panic::catch_unwind, sync::Arc};

lazy_static! {
    static ref VM: Arc<JavaVM> = create_vm_for_tests_with_fake_classes();
}

#[test]
fn load_one_service() {
    let runtime = get_runtime();
    let artifact_path = create_service_artifact_valid(runtime.get_executor());
    let artifact_id = runtime.load_artifact(&artifact_path);
    let service = runtime.create_service(&artifact_id);

    let mut testkit = TestKitBuilder::validator().with_service(service).create();

    testkit.create_block();
}

#[test]
fn load_two_services() {
    let runtime = get_runtime();

    let fake_artifact_path = get_fake_service_artifact_path();
    let fake_artifact_id = runtime.load_artifact(&fake_artifact_path);
    let fake_service = runtime.create_service(&fake_artifact_id);

    let valid_artifact_path = create_service_artifact_valid(runtime.get_executor());
    let valid_artifact_id = runtime.load_artifact(&valid_artifact_path);
    let valid_service = runtime.create_service(&valid_artifact_id);

    let mut testkit = TestKitBuilder::validator()
        .with_service(fake_service)
        .with_service(valid_service)
        .create();

    testkit.create_block();
}

#[test]
#[should_panic(expected = "Unable to load artifact")]
fn load_nonexistent_artifact() {
    let runtime = get_runtime();
    let artifact_path = "nonexistent_artifact.jar";
    runtime.load_artifact(&artifact_path);
}

#[test]
#[should_panic(expected = "Unable to load artifact")]
fn load_artifact_twice() {
    let res = catch_unwind(|| {
        let runtime = get_runtime();
        let artifact_path = create_service_artifact_valid(runtime.get_executor());
        runtime.load_artifact(&artifact_path);
        (runtime, artifact_path)
    });
    assert!(res.is_ok());

    let (runtime, artifact_path) = res.unwrap();
    // The second loading attempt should fail.
    runtime.load_artifact(&artifact_path);
}

#[test]
#[should_panic(expected = "Unable to load artifact")]
fn load_failing_artifact() {
    let runtime = get_runtime();
    let artifact_path = create_service_artifact_non_loadable(runtime.get_executor());
    runtime.load_artifact(&artifact_path);
}

#[test]
#[should_panic(expected = "Unable to create service for artifact_id")]
fn non_instantiable_service() {
    let runtime = get_runtime();
    let artifact_path = create_service_artifact_non_instantiable_service(runtime.get_executor());
    let artifact_id = runtime.load_artifact(&artifact_path);
    runtime.create_service(&artifact_id);
}

#[test]
#[should_panic(expected = "Unable to create service for artifact_id")]
fn create_service_for_unknown_artifact() {
    let runtime = get_runtime();
    runtime.create_service("unknown:artifact:id");
}

// Creates a new instance of JavaServiceRuntime for same JVM.
fn get_runtime() -> JavaServiceRuntime {
    JavaServiceRuntime::create_with_jvm(VM.clone(), 0)
}
