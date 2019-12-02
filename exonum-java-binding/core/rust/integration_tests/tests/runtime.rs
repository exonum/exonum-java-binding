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
extern crate hex;
extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;
extern crate tempfile;
extern crate tokio_core;

use exonum_testkit::TestKitBuilder;
use futures::sync::mpsc;
use hex::FromHex;
use integration_tests::{
    fake_runtime::create_fake_service_runtime_adapter,
    fake_service::{
        self, create_before_commit_test_map, create_init_service_test_map,
        create_service_artifact_non_instantiable_service, create_service_artifact_non_loadable,
        create_service_artifact_valid, create_tx_test_entry,
    },
    vm::{create_vm_for_tests_with_fake_classes, log4j_path},
};
use java_bindings::{
    create_service_runtime,
    exonum::{
        blockchain::{Blockchain, ExecutionError, ExecutionErrorKind, InstanceConfig},
        crypto::{gen_keypair, hash, Hash, PublicKey, SecretKey},
        messages::{AnyTx, Verified},
        node::ApiSender,
        runtime::{ArtifactId, CallInfo, InstanceId, InstanceSpec, MethodId, Runtime},
    },
    exonum_merkledb::{BinaryValue, TemporaryDB},
    jni::JavaVM,
    utils::any_to_string,
    DeployArguments, Error, Executor, JavaRuntimeProxy, RuntimeConfig,
};
use std::{panic, path::PathBuf, sync::Arc};
use tempfile::TempPath;
use tokio_core::reactor::Core;

lazy_static! {
    static ref VM: Arc<JavaVM> = create_vm_for_tests_with_fake_classes();
}

const VALID_INSTANCE_ID: u32 = 127;
const VALID_INSTANCE_NAME: &str = "artifact__test_valid";
const VALID_ARTIFACT_NAME: &str = "artifact:test-valid";
const VALID_ARTIFACT_VERSION: &str = "1.0";

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
        _ => panic!("error.kind is not 'ExecutionErrorKind::Runtime'"),
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
    let mut runtime = create_runtime();
    let blockchain = create_blockchain();

    runtime.initialize(&blockchain);
    runtime.shutdown();
}

#[test]
fn load_artifact() {
    let mut runtime = create_runtime();
    let mut core = Core::new().unwrap();
    let blockchain = create_blockchain();

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
    let mut runtime = create_runtime();
    let mut core = Core::new().unwrap();
    let blockchain = create_blockchain();

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
    let mut runtime = create_runtime();
    let mut core = Core::new().unwrap();
    let blockchain = create_blockchain();

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
    let mut runtime = create_runtime();
    let mut core = Core::new().unwrap();
    let blockchain = create_blockchain();

    let artifact_one = create_artifact(
        runtime.get_executor(),
        "artifact:test-name",
        "1.0",
        create_service_artifact_valid,
    );

    let artifact_two = create_artifact(
        runtime.get_executor(),
        "artifact:test-name",
        "0.9",
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
    let runtime = create_runtime();

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
    let runtime = create_runtime();

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
    let runtime = create_runtime();

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

#[test]
fn service_can_modify_db_on_initialize() {
    let (runtime, _instance_id, config, _path) = create_runtime_with_valid_test_config();

    let mut test_kit = TestKitBuilder::validator()
        .with_additional_runtime(runtime)
        .with_instances(vec![config])
        .create();
    test_kit.create_block();

    let snapshot = test_kit.snapshot();
    let test_map = create_init_service_test_map(&*snapshot, fake_service::TEST_SERVICE_NAME);
    let key = hash(fake_service::INITIAL_ENTRY_KEY.as_ref());
    let value = test_map
        .get(&key)
        .expect("Failed to find the entry created in the test service");
    assert_eq!(fake_service::INITIAL_ENTRY_VALUE, value);

    test_kit.stop();
}

#[test]
fn execute_valid_transaction() {
    let tx_args = &[1_u8, 2_u8, 3_u8, 4_u8];
    let tx_args_hex = hex::encode(tx_args);

    let (runtime, instance_id, config, _path) = create_runtime_with_valid_test_config();

    let mut test_kit = TestKitBuilder::validator()
        .with_additional_runtime(runtime)
        .with_instances(vec![config])
        .create();

    test_kit.create_block();

    let tx = create_transaction(instance_id, fake_service::SET_ENTRY_TX, tx_args);
    let block = test_kit.create_block_with_transaction(tx);

    assert_eq!(block.transactions.len(), 1);
    assert!(block.transactions.get(0).unwrap().status().is_ok());

    test_kit.create_block();

    let snapshot = test_kit.snapshot();
    let test_entry = create_tx_test_entry(&*snapshot, fake_service::TEST_SERVICE_NAME);
    let value = test_entry
        .get()
        .expect("Failed to find the entry created in the test service");

    assert_eq!(tx_args_hex, value);

    test_kit.stop();
}

#[test]
fn submit_non_convertible_tx() {
    let tx_args = &[1_u8, 2_u8, 3_u8, 4_u8];

    let (runtime, instance_id, config, _path) = create_runtime_with_valid_test_config();

    let mut test_kit = TestKitBuilder::validator()
        .with_additional_runtime(runtime)
        .with_instances(vec![config])
        .create();
    test_kit.create_block();

    let tx = create_transaction(instance_id, fake_service::THROW_SOE_TX, tx_args);
    let block = test_kit.create_block_with_transaction(tx);

    assert_eq!(block.transactions.len(), 1);
    let status = block.transactions.get(0).unwrap().status();
    assert!(status.is_err());
    assert!(status
        .unwrap_err()
        .to_string()
        .contains("java.lang.StackOverflowError;"));

    test_kit.stop();
}

#[test]
fn submit_service_error_on_exec_tx() {
    let tx_args = &[255_u8, 2_u8, 3_u8, 4_u8];
    let tx_error_code = tx_args[0];

    let (runtime, instance_id, config, _path) = create_runtime_with_valid_test_config();

    let mut test_kit = TestKitBuilder::validator()
        .with_additional_runtime(runtime)
        .with_instances(vec![config])
        .create();

    test_kit.create_block();

    let tx = create_transaction(instance_id, fake_service::SRVC_ERR_ON_EXEC_TX, tx_args);
    let block = test_kit.create_block_with_transaction(tx);

    assert_eq!(block.transactions.len(), 1);
    let status = block.transactions.get(0).unwrap().status();
    assert!(status.is_err());
    assert_service_error_with_code(status.unwrap_err(), tx_error_code);

    test_kit.stop();
}

#[test]
fn submit_failing_on_exec_tx() {
    let tx_args = &[1_u8, 2_u8, 3_u8, 4_u8];

    let (runtime, instance_id, config, _path) = create_runtime_with_valid_test_config();

    let mut test_kit = TestKitBuilder::validator()
        .with_additional_runtime(runtime)
        .with_instances(vec![config])
        .create();

    test_kit.create_block();

    let tx = create_transaction(instance_id, fake_service::FAIL_ON_EXEC_TX, tx_args);
    let block = test_kit.create_block_with_transaction(tx);

    assert_eq!(block.transactions.len(), 1);
    let status = block.transactions.get(0).unwrap().status();
    assert!(status.is_err());
    assert!(status
        .unwrap_err()
        .to_string()
        .contains("java.lang.ArithmeticException;"));

    test_kit.stop();
}

#[test]
fn before_commit() {
    let (runtime, _instance_id, config, _path) = create_runtime_with_valid_test_config();

    let mut test_kit = TestKitBuilder::validator()
        .with_additional_runtime(runtime)
        .with_instances(vec![config])
        .create();

    let mut value: Option<i32> = None;

    for _ in 0..10 {
        test_kit.create_block();

        let snapshot = test_kit.snapshot();
        let map = create_before_commit_test_map(&*snapshot, fake_service::TEST_SERVICE_NAME);
        let key = hash(fake_service::BEFORE_COMMIT_ENTRY_KEY.as_ref());
        let cur_value = map
            .get(&key)
            .expect("Failed to find the entry created in beforeCommit")
            .parse::<i32>()
            .expect("Failed to parse the entry value created in beforeCommit");

        if let Some(prev_value) = value {
            assert_eq!(prev_value + 1, cur_value);
        }

        value = Some(cur_value);
    }

    test_kit.stop();
}

#[test]
fn state_hashes() {
    let service_hash = &[
        Hash::from_hex("8c1ea14c7893acabde2aa95031fae57abb91516ddb78b0f6622afa0d8cb1b5c2").unwrap(),
        Hash::from_hex("7324b5c72b51bb5d4c180f1109cfd347b60473882145841c39f3e584576296f9").unwrap(),
    ];

    let (runtime, _instance_id, config, _path) = create_runtime_with_valid_test_config();
    let runtime_copy = runtime.clone();

    let mut test_kit = TestKitBuilder::validator()
        .with_additional_runtime(runtime)
        .with_instances(vec![config])
        .create();

    for _ in 0..5 {
        test_kit.create_block();

        let snapshot = test_kit.snapshot();
        let aggregator = runtime_copy.state_hashes(&snapshot);

        assert_eq!(aggregator.instances.len(), 1);
        let (instance, hashes) = aggregator
            .instances
            .get(0)
            .expect("Failed to find state hash pair for test service");
        assert_eq!(*instance, VALID_INSTANCE_ID);

        assert_eq!(hashes.len(), 2);
        let hash = hashes
            .get(0)
            .expect("Failed to find state_hash[0] for test-service");
        assert_eq!(*hash, service_hash[0]);
        let hash = hashes
            .get(1)
            .expect("Failed to find state_hash[1] for test-service");
        assert_eq!(*hash, service_hash[1]);
    }

    test_kit.stop();
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

fn create_runtime_with_valid_test_config(
) -> (JavaRuntimeProxy, InstanceId, InstanceConfig, TempPath) {
    let runtime = create_runtime();

    let service_instance_config = get_service_instance_config(
        runtime.get_executor(),
        VALID_INSTANCE_ID,
        VALID_INSTANCE_NAME,
        VALID_ARTIFACT_NAME,
        VALID_ARTIFACT_VERSION,
        create_service_artifact_valid,
    );

    (
        runtime,
        service_instance_config.0.instance_spec.id,
        service_instance_config.0,
        service_instance_config.1,
    )
}

// Creates a new instance of JavaRuntimeProxy and Executor for same JVM.
fn create_runtime() -> JavaRuntimeProxy {
    let runtime_config = RuntimeConfig {
        artifacts_path: PathBuf::from("/tmp/"),
        // Pass log4j path to avoid error messages of mis-configuration
        log_config_path: log4j_path(),
        port: 0,
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

// Creates a new Blockchain instance.
fn create_blockchain() -> Blockchain {
    let keypair: (PublicKey, SecretKey) = gen_keypair();
    let api_channel = mpsc::channel(128);
    let (app_tx, _app_rx) = (ApiSender::new(api_channel.0), api_channel.1);

    let storage = TemporaryDB::new();
    Blockchain::new(storage, keypair, app_tx.clone())
}

// Creates a new signed transaction containing `args` for given service instance.
fn create_transaction(instance: InstanceId, method: MethodId, args: &[u8]) -> Verified<AnyTx> {
    let tx = AnyTx {
        call_info: CallInfo {
            instance_id: instance,
            method_id: method,
        },
        arguments: args.to_vec(),
    };

    let (pub_key, sec_key) = gen_keypair();
    Verified::from_value(tx, pub_key, &sec_key)
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

// Asserts that given `ExecutionError`
// has kind `ExecutionErrorKind::Service` and given `service_code`
fn assert_service_error_with_code(error: &ExecutionError, service_code: u8) {
    match error.kind {
        ExecutionErrorKind::Service { code } => assert_eq!(code, service_code),
        _ => panic!("error.kind is not 'ExecutionErrorKind::Service'"),
    }
}
