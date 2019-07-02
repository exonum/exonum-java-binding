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
    vm::{create_vm_for_tests_with_fake_classes, fake_service_artifact_path},
};
use java_bindings::{jni::JavaVM, utils::any_to_string, JavaServiceRuntime};
use std::{panic, sync::Arc};

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

    let fake_artifact_path = fake_service_artifact_path();
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
fn load_nonexistent_artifact() {
    let runtime = get_runtime();
    let artifact_path = "nonexistent_artifact.jar";

    assert_panics(
        &format!("Failed to load the service from {}", artifact_path),
        || runtime.load_artifact(&artifact_path),
    );
}

#[test]
fn load_artifact_twice() {
    let runtime = get_runtime();
    let artifact_path = create_service_artifact_valid(runtime.get_executor());
    runtime.load_artifact(&artifact_path);

    // The second loading attempt should fail.
    assert_panics(
        &format!(
            "Failed to load the service from {}",
            artifact_path.to_string_lossy()
        ),
        || runtime.load_artifact(&artifact_path),
    );
}

#[test]
fn load_failing_artifact() {
    let runtime = get_runtime();
    let artifact_path = create_service_artifact_non_loadable(runtime.get_executor());

    assert_panics(
        "Java exception: com.exonum.binding.core.runtime.ServiceLoadingException;",
        || runtime.load_artifact(&artifact_path),
    );
}

#[test]
fn non_instantiable_service() {
    let runtime = get_runtime();
    let artifact_path = create_service_artifact_non_instantiable_service(runtime.get_executor());
    let artifact_id = runtime.load_artifact(&artifact_path);

    assert_panics(
        "com.exonum.binding.fakes.services.invalidservice.NonInstantiableService",
        || runtime.create_service(&artifact_id),
    );
}

#[test]
fn create_service_for_unknown_artifact() {
    let runtime = get_runtime();

    assert_panics("Unknown artifactId: unknown:artifact:id", || {
        runtime.create_service("unknown:artifact:id")
    });
}

// Creates a new instance of JavaServiceRuntime for same JVM.
fn get_runtime() -> JavaServiceRuntime {
    JavaServiceRuntime::create_with_jvm(VM.clone(), 0)
}

// Asserts that given closure panics while executed and error message contains given substring.
fn assert_panics<F, R>(err_substring: &str, f: F)
where
    F: FnOnce() -> R,
{
    let result = panic::catch_unwind(panic::AssertUnwindSafe(f));
    match result {
        Ok(_) => panic!("Panic expected"),
        Err(err) => {
            let err_msg = any_to_string(&err);
            assert!(err_msg.contains(err_substring));
        }
    }
}
