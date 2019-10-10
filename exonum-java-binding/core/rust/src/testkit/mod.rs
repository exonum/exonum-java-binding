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

use self::time_provider::JavaTimeProvider;
use exonum::{
    blockchain::{Block, InstanceCollection, InstanceConfig},
    crypto::{PublicKey, SecretKey},
    helpers::ValidatorId,
    merkledb::BinaryValue,
    runtime::{InstanceId, InstanceSpec, Runtime},
};
use exonum_testkit::{TestKit, TestKitBuilder};
use exonum_time::{time_provider::TimeProvider, TimeServiceFactory};
use handle::{cast_handle, drop_handle, to_handle, Handle};
use jni::{
    objects::{JObject, JValue},
    sys::{jboolean, jbyteArray, jobjectArray, jshort},
    Executor, JNIEnv,
};
use std::{panic, sync::Arc};
use storage::View;
use utils::{convert_to_string, unwrap_exc_or, unwrap_exc_or_default};
use {JavaRuntimeProxy, JniResult};

mod time_provider;

const KEYPAIR_CLASS: &str = "com/exonum/binding/common/crypto/KeyPair";
const KEYPAIR_CTOR_SIGNATURE: &str = "([B[B)Lcom/exonum/binding/common/crypto/KeyPair;";
const EMULATED_NODE_CLASS: &str = "com/exonum/binding/testkit/EmulatedNode";
const EMULATED_NODE_CTOR_SIGNATURE: &str = "(ILcom/exonum/binding/common/crypto/KeyPair;)V";

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
    time_service_specs: jobjectArray,
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
            // TODO: remove this line after corresponding From<JavaRuntimeProxy> implementation
            let runtime = (
                JavaRuntimeProxy::RUNTIME_ID as u32,
                Box::new(runtime) as Box<dyn Runtime>,
            );
            builder = builder
                .with_runtime(runtime)
                .with_instances(instance_configs_from_java_array(&env, services)?);

            for instance_spec in
                time_service_specs_from_java_array(&env, executor.clone(), time_service_specs)?
            {
                builder = builder.with_service(instance_spec);
            }

            builder
        };
        let testkit = builder.create();
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
fn instance_configs_from_java_array(
    env: &JNIEnv,
    services: jobjectArray,
) -> JniResult<Vec<InstanceConfig>> {
    let mut instance_configs = vec![];
    let num_services = env.get_array_length(services)?;
    for i in 0..num_services {
        let service_obj = env.get_object_array_element(services, i)?;

        let artifact_id = convert_to_string(
            &env,
            env.get_field(service_obj, "artifactId", "Ljava/lang/String;")?
                .l()?,
        )?;
        let artifact_filename = convert_to_string(
            &env,
            env.get_field(service_obj, "artifactFilename", "Ljava/lang/String;")?
                .l()?,
        )?;
        let service_specs_obj: jobjectArray = env
            .get_field(
                service_obj,
                "serviceSpecs",
                "[Lcom/exonum/binding/testkit/ServiceSpec;",
            )?
            .l()?
            .into_inner();

        let num_specs = env.get_array_length(service_specs_obj)?;

        for k in 0..num_specs {
            let service_spec = env.get_object_array_element(service_specs_obj, k)?;

            let service_id = env.get_field(service_spec, "serviceId", "I")?.i()? as u32;
            let service_name = convert_to_string(
                &env,
                env.get_field(service_spec, "serviceName", "Ljava/lang/String;")?
                    .l()?,
            )?;
            let config_params: jbyteArray = env
                .get_field(service_spec, "configuration", "[B")?
                .l()?
                .into_inner();
            let config = env.convert_byte_array(config_params)?;

            let spec = InstanceSpec::new(service_id, service_name, &artifact_id)
                .expect("Unable to create instance specification for service");
            let cfg = InstanceConfig::new(spec, Some(artifact_filename.to_bytes()), config);
            instance_configs.push(cfg);
        }
    }
    Ok(instance_configs)
}

// Converts Java array of `TimeServiceSpec` to vector of `InstanceCollection`.
fn time_service_specs_from_java_array(
    env: &JNIEnv,
    executor: Executor,
    service_specs: jobjectArray,
) -> JniResult<Vec<InstanceCollection>> {
    let mut instance_collection = vec![];
    let num_configs = env.get_array_length(service_specs)?;
    for i in 0..num_configs {
        let service_spec_obj = env.get_object_array_element(service_specs, i)?;

        let service_id = env.get_field(service_spec_obj, "serviceId", "I")?.i()?;
        let service_name = convert_to_string(
            &env,
            env.get_field(service_spec_obj, "serviceName", "Ljava/lang/String;")?
                .l()?,
        )?;
        let time_provider = env
            .get_field(
                service_spec_obj,
                "timeProvider",
                "Lcom/exonum/binding/testkit/TimeProviderAdapter;",
            )?
            .l()?;

        let provider = JavaTimeProvider::new(executor.clone(), time_provider);
        let factory =
            TimeServiceFactory::with_provider(Arc::new(provider) as Arc<dyn TimeProvider>);
        instance_collection.push(InstanceCollection::new(factory).with_instance(
            service_id as InstanceId,
            service_name,
            (),
        ));
    }
    Ok(instance_collection)
}
