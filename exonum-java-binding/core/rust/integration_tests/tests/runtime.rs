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
extern crate futures;
extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;
extern crate tempfile;
extern crate tokio_core;

use exonum_testkit::TestKitBuilder;
use futures::sync::mpsc::Receiver;
use integration_tests::{
    fake_runtime::*,
    fake_service::*,
    vm::{create_vm_for_tests_with_fake_classes, fakes_classpath, java_library_path, log4j_path},
};
use java_bindings::{
    create_service_runtime,
    exonum::{
        blockchain::{Blockchain, ExecutionError, ExecutionErrorKind, InstanceConfig},
        crypto::{gen_keypair, PublicKey, SecretKey},
        node::ApiSender,
        runtime::{ArtifactId, InstanceId, InstanceSpec, Runtime},
    },
    exonum_merkledb::{BinaryValue, TemporaryDB},
    jni::JavaVM,
    utils::any_to_string,
    DeployArguments, Error, Executor, InternalConfig, JavaRuntimeProxy, RuntimeConfig,
};
use std::{panic, path::PathBuf, sync::Arc};
use tempfile::TempPath;
use tokio_core::reactor::Core;

lazy_static! {
    static ref VM: Arc<JavaVM> = create_vm_for_tests_with_fake_classes();
}

#[test]
fn runtime_exception_handling_checked_exception() {
    let artifact = ArtifactId::new(JavaRuntimeProxy::RUNTIME_ID, "artifact:test-name:1.0").unwrap();
    let mut runtime = get_fake_runtime("createRuntimeThrowingExceptions");
    let future = runtime.deploy_artifact(artifact, vec![]);

    let mut core = Core::new().unwrap();
    let result = core.run(future);

    assert!(result.is_err());

    let error = result.unwrap_err();
    let message = error.description;
    let kind = error.kind;

    match kind {
        ExecutionErrorKind::Runtime { code } => {
            assert_eq!(code, Error::JavaException as u8);
            assert!(message.contains("deployArtifact"));
            assert!(message.contains("ServiceLoadingException"))
        }
        _ => assert!(false, "error.kind is not 'ExecutionErrorKind::Runtime'"),
    }

    runtime.shutdown();
}

#[test]
fn runtime_exception_handling_unchecked_exception() {
    let artifact = ArtifactId::new(JavaRuntimeProxy::RUNTIME_ID, "artifact:test-name:1.0").unwrap();
    let mut runtime = get_fake_runtime("createRuntimeThrowingExceptions");
    assert_panics("isArtifactDeployed", || {
        runtime.is_artifact_deployed(&artifact)
    });
    runtime.shutdown();
}

#[test]
fn runtime_initialize_and_shutdown() {
    let mut runtime = get_runtime(6300);
    let keypair = gen_keypair();
    let blockchain = create_blockchain(keypair);

    runtime.initialize(&blockchain);
    runtime.shutdown();
}

#[test]
fn load_artifact() {
    let mut runtime = get_runtime(6301);
    let mut core = Core::new().unwrap();
    let blockchain = create_blockchain(gen_keypair());

    let artifact = create_artifact(
        runtime.get_executor(),
        "artifact:test-name",
        "1.0",
        create_service_artifact_valid,
    );

    runtime.initialize(&blockchain);

    let future = runtime.deploy_artifact(artifact.0.clone(), artifact.1.to_bytes());
    let result = core.run(future);

    assert!(result.is_ok(), "Deploy artifact result is not Ok()");
    assert!(runtime.is_artifact_deployed(&artifact.0));

    runtime.shutdown();
}

#[test]
fn load_non_loadable_artifact() {
    let mut runtime = get_runtime(6302);
    let mut core = Core::new().unwrap();
    let blockchain = create_blockchain(gen_keypair());

    let artifact = create_artifact(
        runtime.get_executor(),
        "artifact:test-name",
        "1.0",
        create_service_artifact_non_loadable,
    );

    runtime.initialize(&blockchain);

    let future = runtime.deploy_artifact(artifact.0.clone(), artifact.1.to_bytes());
    let result = core.run(future);

    assert!(
        result.is_err(),
        "Deploy non-loadable artifact result is not Err()",
    );
    assert!(
        !runtime.is_artifact_deployed(&artifact.0),
        "Non-loadable artifact is deployed",
    );

    runtime.shutdown();
}

#[test]
fn load_artifact_twice_same_version() {
    let mut runtime = get_runtime(6303);
    let mut core = Core::new().unwrap();
    let blockchain = create_blockchain(gen_keypair());

    let artifact_one = create_artifact(
        runtime.get_executor(),
        "artifact:test-name",
        "1.0",
        create_service_artifact_valid,
    );

    let artifact_two = create_artifact(
        runtime.get_executor(),
        "artifact:test-name",
        "1.0",
        create_service_artifact_valid,
    );

    runtime.initialize(&blockchain);

    let future = runtime.deploy_artifact(artifact_one.0.clone(), artifact_one.1.to_bytes());
    let result = core.run(future);

    assert!(result.is_ok(), "Deploy first artifact result is not Ok()");
    assert!(runtime.is_artifact_deployed(&artifact_one.0));

    let future = runtime.deploy_artifact(artifact_two.0.clone(), artifact_two.1.to_bytes());
    let result = core.run(future);

    assert!(
        result.is_err(),
        "Deploy second artifact result is not Err()",
    );

    runtime.shutdown();
}

#[test]
fn load_artifact_twice_other_version() {
    let mut runtime = get_runtime(6304);
    let mut core = Core::new().unwrap();
    let blockchain = create_blockchain(gen_keypair());

    let artifact_one = create_artifact(
        runtime.get_executor(),
        "artifact:test-name",
        "1.0",
        create_service_artifact_valid,
    );

    let artifact_two = create_artifact(
        runtime.get_executor(),
        "artifact:test-name",
        "1.1",
        create_service_artifact_valid,
    );

    runtime.initialize(&blockchain);

    let future = runtime.deploy_artifact(artifact_one.0.clone(), artifact_one.1.to_bytes());
    let result = core.run(future);

    assert!(result.is_ok(), "Deploy first artifact result is not Ok()");
    assert!(runtime.is_artifact_deployed(&artifact_one.0));

    let future = runtime.deploy_artifact(artifact_two.0.clone(), artifact_two.1.to_bytes());
    let result = core.run(future);

    assert!(result.is_ok(), "Deploy second artifact result is not Ok()");
    assert!(runtime.is_artifact_deployed(&artifact_two.0));

    runtime.shutdown();
}

#[test]
fn initialize_one_artifact() {
    let runtime = get_runtime(6305);

    let service_instance_config = get_service_instance_config(
        runtime.get_executor(),
        1,
        "artifact__test_name",
        "artifact:test-name",
        "1.0",
        create_service_artifact_valid,
    );
    let instances = vec![service_instance_config.0];

    let mut test_kit = TestKitBuilder::validator()
        .with_additional_runtime(runtime)
        .with_instances(instances)
        .create();
    test_kit.create_block();
    test_kit.stop();
}

#[test]
fn initialize_two_artifacts() {
    let runtime = get_runtime(6306);

    let service_instance_one_config = get_service_instance_config(
        runtime.get_executor(),
        1,
        "artifact__test_name_one",
        "artifact:test-name-one",
        "1.0",
        create_service_artifact_valid,
    );
    let service_instance_two_config = get_service_instance_config(
        runtime.get_executor(),
        2,
        "artifact__test_name_two",
        "artifact:test-name-two",
        "1.0",
        create_service_artifact_valid,
    );
    let instances = vec![service_instance_one_config.0, service_instance_two_config.0];

    let mut test_kit = TestKitBuilder::validator()
        .with_additional_runtime(runtime)
        .with_instances(instances)
        .create();
    test_kit.create_block();
    test_kit.stop();
}

#[test]
fn initialize_non_instantiable_artifact() {
    let runtime = get_runtime(6307);

    let service_instance_config = get_service_instance_config(
        runtime.get_executor(),
        1,
        "artifact__test_name",
        "artifact:test-name",
        "1.0",
        create_service_artifact_non_instantiable_service,
    );
    let instances = vec![service_instance_config.0];

    let test_kit_builder = TestKitBuilder::validator()
        .with_additional_runtime(runtime)
        .with_instances(instances);
    assert_panics(
        format!(
            "{} {}",
            "Could not find a suitable constructor in",
            "com.exonum.binding.fakes.services.invalidservice.NonInstantiableService",
        )
        .as_str(),
        || test_kit_builder.create(),
    );
}

fn create_artifact<C>(
    executor: &Executor,
    artifact_name: &str,
    artifact_version: &str,
    build_fn: C,
) -> (ArtifactId, DeployArguments, TempPath)
where
    C: FnOnce(&Executor, &str, &str) -> TempPath,
{
    let artifact = ArtifactId::new(
        JavaRuntimeProxy::RUNTIME_ID,
        format!("{}:{}", artifact_name, artifact_version),
    )
    .unwrap();
    let path = build_fn(executor, &artifact.to_string(), artifact_version);
    let deploy_args = DeployArguments {
        artifact_filename: path.to_str().unwrap().to_string(),
    };

    (artifact, deploy_args, path)
}

fn get_service_instance_config<C>(
    executor: &Executor,
    instance_id: InstanceId,
    instance_name: &str,
    artifact_name: &str,
    artifact_version: &str,
    build_artifact_fn: C,
) -> (InstanceConfig, TempPath)
where
    C: FnOnce(&Executor, &str, &str) -> TempPath,
{
    let (artifact, args, path) =
        create_artifact(executor, artifact_name, artifact_version, build_artifact_fn);

    let artifact_spec: Vec<u8> = args.to_bytes();
    let constructor: Vec<u8> = Vec::new();
    let instance_spec =
        InstanceSpec::new(instance_id, instance_name, &artifact.to_string()).unwrap();

    (
        InstanceConfig::new(
            instance_spec.clone(),
            Some(artifact_spec.clone()),
            constructor.clone(),
        ),
        path,
    )
}

// Creates a new instance of JavaRuntimeProxy and Executor for same JVM.
fn get_runtime(port: i32) -> JavaRuntimeProxy {
    let runtime_config = RuntimeConfig {
        artifacts_path: PathBuf::from("/tmp/"),
        // Pass log4j path to avoid error messages of mis-configuration
        log_config_path: log4j_path(),
        port,
        override_system_lib_path: None,
    };

    let executor = Executor::new(VM.to_owned());
    create_service_runtime(executor, &runtime_config)
}

// Creates a new fake instance of JavaRuntimeProxy and Executor for same JVM.
fn get_fake_runtime(facade_method: &str) -> JavaRuntimeProxy {
    let executor = Executor::new(VM.to_owned());
    create_fake_service_runtime_adapter(executor, facade_method)
}

fn create_blockchain(keypair: (PublicKey, SecretKey)) -> Blockchain {
    let api_channel = mpsc::channel(128);
    let (app_tx, app_rx) = (ApiSender::new(api_channel.0), api_channel.1);

    let storage = TemporaryDB::new();
    Blockchain::new(storage, keypair, app_tx.clone())
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

fn _assert_service_error_with_code(error: ExecutionError, service_code: u8) {
    match error.kind {
        ExecutionErrorKind::Service { code } => assert_eq!(code, service_code),
        _ => assert!(false, "error.kind is not 'ExecutionErrorKind::Service'"),
    }
}
