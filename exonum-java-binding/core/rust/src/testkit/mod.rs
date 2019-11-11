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

//! Provides native methods for Java TestKit support.

use std::{panic, sync::Arc};

use exonum::{
    blockchain::{Block, InstanceCollection, InstanceConfig},
    crypto::{PublicKey, SecretKey},
    helpers::ValidatorId,
    merkledb::BinaryValue,
    runtime::InstanceSpec,
};
use exonum_testkit::{TestKit, TestKitBuilder};
use exonum_time::{time_provider::TimeProvider, TimeServiceFactory};
use jni::{
    objects::{JObject, JValue},
    sys::{jboolean, jbyteArray, jobjectArray, jshort},
    Executor, JNIEnv,
};

use handle::{cast_handle, drop_handle, to_handle, Handle};
use storage::View;
use utils::{convert_to_string, unwrap_exc_or, unwrap_exc_or_default};
use {JavaRuntimeProxy, JniError, JniResult};

use self::time_provider::JavaTimeProvider;

mod time_provider;

const KEYPAIR_CLASS: &str = "com/exonum/binding/common/crypto/KeyPair";
const KEYPAIR_CTOR_SIGNATURE: &str = "([B[B)Lcom/exonum/binding/common/crypto/KeyPair;";
const EMULATED_NODE_CLASS: &str = "com/exonum/binding/testkit/EmulatedNode";
const EMULATED_NODE_CTOR_SIGNATURE: &str = "(ILcom/exonum/binding/common/crypto/KeyPair;)V";
const SERVICE_SPECS_FIELD_TYPE: &str = "[Lcom/exonum/binding/testkit/ServiceSpec;";
const TIME_PROVIDER_FIELD_TYPE: &str = "Lcom/exonum/binding/testkit/TimeProviderAdapter;";

/// Creates TestKit instance with specified services and wires public API handlers.
/// The caller is responsible for properly destroying TestKit instance and freeing
/// the memory by calling `nativeFreeTestKit` function.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_testkit_TestKit_nativeCreateTestKit(
    env: JNIEnv,
    _: JObject,
    services: jobjectArray,
    auditor: jboolean,
    validator_count: jshort,
    time_service_spec: JObject,
    runtime_adapter: JObject,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let mut builder = if auditor == jni::sys::JNI_TRUE {
            TestKitBuilder::auditor()
        } else {
            TestKitBuilder::validator()
        };
        builder = builder.with_validators(validator_count as _);
        let builder = {
            let executor = Executor::new(Arc::new(env.get_java_vm()?));

            let runtime =
                JavaRuntimeProxy::new(executor.clone(), env.new_global_ref(runtime_adapter)?);
            builder = builder
                .with_additional_runtime(runtime)
                // TODO: Rewrite with protobuf: ECR-3689
                .with_instances(instance_configs_from_java_array(&env, services)?);

            if let Some(instance) =
                time_service_instance_from_java(&env, executor.clone(), time_service_spec)?
            {
                builder = builder.with_rust_service(instance);
            }

            builder
        };
        let mut testkit = builder.create();
        // Mount API handlers
        testkit.api();
        Ok(to_handle(testkit))
    });
    unwrap_exc_or_default(&env, res)
}

/// Destroys TestKit instance behind the provided handler and frees occupied memory.
/// Must be called by Java side.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_testkit_TestKit_nativeFreeTestKit(
    env: JNIEnv,
    _: JObject,
    handle: Handle,
) {
    drop_handle::<TestKit>(&env, handle)
}

/// Creates Snapshot using provided TestKit instance.
///
/// Calls `TestKit::poll_events`, so all transactions received prior to the call of this method
/// are handled and added to the pool.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_testkit_TestKit_nativeCreateSnapshot(
    env: JNIEnv,
    _: JObject,
    handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let testkit = cast_handle::<TestKit>(handle);
        testkit.poll_events();
        let snapshot = testkit.snapshot();
        let view = View::from_owned_snapshot(snapshot);
        Ok(to_handle(view))
    });
    unwrap_exc_or_default(&env, res)
}

/// Creates new block and returns its header.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_testkit_TestKit_nativeCreateBlock(
    env: JNIEnv,
    _: JObject,
    handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let testkit = cast_handle::<TestKit>(handle);
        let block = testkit.create_block().header;
        serialize_block(&env, block)
    });
    unwrap_exc_or(&env, res, std::ptr::null_mut())
}

/// Creates Block with specified list of transactions and returns its header.
/// The transactions are the byte[][] array which contains the set of serialized transaction
/// messages in Protobuf format.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_testkit_TestKit_nativeCreateBlockWithTransactions(
    env: JNIEnv,
    _: JObject,
    handle: Handle,
    transactions: jobjectArray,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let testkit = cast_handle::<TestKit>(handle);
        let mut raw_transactions = Vec::new();
        let transactions_count = env.get_array_length(transactions)?;
        for i in 0..transactions_count {
            let serialized_tx_object =
                env.auto_local(env.get_object_array_element(transactions, i as _)?);
            let serialized_tx: jbyteArray = serialized_tx_object.as_obj().into_inner();
            let serialized_tx = env.convert_byte_array(serialized_tx)?;
            raw_transactions.push(BinaryValue::from_bytes(serialized_tx.into()).unwrap());
        }
        let block = testkit
            .create_block_with_transactions(raw_transactions.into_iter())
            .header;
        serialize_block(&env, block)
    });
    unwrap_exc_or(&env, res, std::ptr::null_mut())
}

/// Returns the EmulatedNode of the provided TestKit instance.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_testkit_TestKit_nativeGetEmulatedNode<'e>(
    env: JNIEnv<'e>,
    _: JObject,
    handle: Handle,
) -> JObject<'e> {
    let res = panic::catch_unwind(|| {
        let testkit = cast_handle::<TestKit>(handle);
        let emulated_node = testkit.us();
        // Validator id == -1 in case of auditor node.
        let validator_id = match emulated_node.validator_id() {
            Some(ValidatorId(id)) => i32::from(id),
            None => -1,
        };
        let service_keypair = emulated_node.service_keypair();
        let java_key_pair = create_java_keypair(&env, service_keypair)?;
        let java_emulated_node = env.new_object(
            EMULATED_NODE_CLASS,
            EMULATED_NODE_CTOR_SIGNATURE,
            &[validator_id.into(), java_key_pair],
        )?;
        Ok(java_emulated_node)
    });
    unwrap_exc_or(&env, res, JObject::null())
}

fn serialize_block(env: &JNIEnv, block: Block) -> jni::errors::Result<jbyteArray> {
    let serialized_block = block.into_bytes();
    env.byte_array_from_slice(&serialized_block)
}

fn create_java_keypair<'a>(
    env: &'a JNIEnv,
    keypair: (PublicKey, SecretKey),
) -> jni::errors::Result<JValue<'a>> {
    let public_key_byte_array: JObject = env.byte_array_from_slice(&keypair.0[..])?.into();
    let secret_key_byte_array: JObject = env.byte_array_from_slice(&keypair.1[..])?.into();
    env.call_static_method(
        KEYPAIR_CLASS,
        "createKeyPair",
        KEYPAIR_CTOR_SIGNATURE,
        &[secret_key_byte_array.into(), public_key_byte_array.into()],
    )
}

// Converts Java array of `TestKitServiceInstances` to vector of `InstanceConfig`.
//
// `TestKitServiceInstances` representation:
//      String artifactId;
//      byte[] deployArguments;
//      ServiceSpec[] serviceSpecs;
fn instance_configs_from_java_array(
    env: &JNIEnv,
    service_artifact_specs: jobjectArray,
) -> JniResult<Vec<InstanceConfig>> {
    let mut instance_configs = vec![];
    let num_artifacts = env.get_array_length(service_artifact_specs)?;
    for i in 0..num_artifacts {
        env.with_local_frame(8, || {
            let artifact_spec_obj = env.get_object_array_element(service_artifact_specs, i)?;

            let artifact_id = get_field_as_string(env, artifact_spec_obj, "artifactId")?;
            let deploy_args: jbyteArray = env
                .get_field(artifact_spec_obj, "deployArguments", "[B")?
                .l()?
                .into_inner();
            let deploy_args = env.convert_byte_array(deploy_args)?;
            let service_specs_obj: jobjectArray = env
                .get_field(artifact_spec_obj, "serviceSpecs", SERVICE_SPECS_FIELD_TYPE)?
                .l()?
                .into_inner();
            // TODO: Avoid deploy arguments duplication after ECR-3690
            let configs = parse_service_specs(env, service_specs_obj, artifact_id, deploy_args)?;
            instance_configs.extend(configs);

            Ok(JObject::null())
        })?;
    }
    Ok(instance_configs)
}

// Converts Java array of `ServiceSpec` instances into vector of `InstanceConfig` for specific artifact.
fn parse_service_specs(
    env: &JNIEnv,
    specs_array: jobjectArray,
    artifact_id: String,
    deploy_args: Vec<u8>,
) -> JniResult<Vec<InstanceConfig>> {
    let num_specs = env.get_array_length(specs_array)?;

    let mut instance_configs = vec![];
    for i in 0..num_specs {
        env.with_local_frame(8, || {
            let service_spec = env.get_object_array_element(specs_array, i)?;
            let (spec, config) = parse_instance_spec(&env, service_spec, &artifact_id)?;
            let cfg = InstanceConfig::new(spec, Some(deploy_args.to_bytes()), config);
            instance_configs.push(cfg);

            Ok(JObject::null())
        })?;
    }

    Ok(instance_configs)
}

// Parses the `ServiceSpec` instance.
//
// `ServiceSpec` representation:
//      String serviceName;
//      int serviceId;
//      byte[] configuration;
fn parse_instance_spec(
    env: &JNIEnv,
    service_spec_obj: JObject,
    artifact_id: impl AsRef<str>,
) -> JniResult<(InstanceSpec, Vec<u8>)> {
    let (service_id, service_name) = get_service_id_and_name(env, service_spec_obj)?;
    let config_params: jbyteArray = env
        .get_field(service_spec_obj, "configuration", "[B")?
        .l()?
        .into_inner();
    let config = env.convert_byte_array(config_params)?;
    let spec = InstanceSpec::new(service_id, service_name, artifact_id).map_err(|err| {
        JniError::from(format!(
            "Unable to create instance specification for the service with id {}: {}",
            service_id, err
        ))
    })?;

    Ok((spec, config))
}

// Creates `InstanceCollection` from `TimeServiceSpec` object.
//
// `TimeServiceSpec`
//      TimeProviderAdapter timeProvider;
//      String serviceName;
//      int serviceId;
fn time_service_instance_from_java(
    env: &JNIEnv,
    executor: Executor,
    time_service_spec: JObject,
) -> JniResult<Option<InstanceCollection>> {
    if time_service_spec.is_null() {
        return Ok(None);
    }

    let (service_id, service_name) = get_service_id_and_name(env, time_service_spec)?;
    let time_provider = env
        .get_field(time_service_spec, "timeProvider", TIME_PROVIDER_FIELD_TYPE)?
        .l()?;

    let provider = JavaTimeProvider::new(executor.clone(), time_provider);
    let factory = TimeServiceFactory::with_provider(Arc::new(provider) as Arc<dyn TimeProvider>);
    let instance = InstanceCollection::new(factory).with_instance(service_id, service_name, ());

    Ok(Some(instance))
}

// Returns id and name value from corresponding instances of Java objects.
fn get_service_id_and_name(env: &JNIEnv, service_obj: JObject) -> JniResult<(u32, String)> {
    let service_id = env.get_field(service_obj, "serviceId", "I")?.i()? as u32;
    let service_name = get_field_as_string(env, service_obj, "serviceName")?;
    Ok((service_id, service_name))
}

// Returns String value of instance's field.
fn get_field_as_string(env: &JNIEnv, obj: JObject, field_name: &str) -> JniResult<String> {
    convert_to_string(
        env,
        env.get_field(obj, field_name, "Ljava/lang/String;")?.l()?,
    )
}
