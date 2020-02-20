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
    blockchain::{config::InstanceInitParams, Block},
    crypto::KeyPair,
    helpers::ValidatorId,
    merkledb::{self as exonum_merkledb, BinaryValue},
    runtime::ArtifactSpec,
};
use exonum_proto::ProtobufConvert;
use exonum_rust_runtime::ServiceFactory;
use exonum_testkit::{TestKit, TestKitBuilder};
use exonum_time::{TimeProvider, TimeServiceFactory};
use jni::{
    objects::{JClass, JObject, JValue},
    sys::{jboolean, jbyteArray, jobjectArray, jshort},
    Executor, JNIEnv,
};

use crate::{
    handle::{cast_handle, drop_handle, to_handle, Handle},
    proto,
    storage::into_erased_access,
    utils::{convert_to_string, unwrap_exc_or, unwrap_exc_or_default},
    JavaRuntimeProxy, JniResult,
};

use self::time_provider::JavaTimeProvider;

mod time_provider;

const KEYPAIR_CLASS: &str = "com/exonum/binding/common/crypto/KeyPair";
const KEYPAIR_CTOR_SIGNATURE: &str = "([B[B)Lcom/exonum/binding/common/crypto/KeyPair;";
const EMULATED_NODE_CLASS: &str = "com/exonum/binding/testkit/EmulatedNode";
const EMULATED_NODE_CTOR_SIGNATURE: &str = "(ILcom/exonum/binding/common/crypto/KeyPair;)V";
const TIME_PROVIDER_FIELD_TYPE: &str = "Lcom/exonum/binding/testkit/TimeProviderAdapter;";

/// Protobuf based container for Testkit initialization.
#[derive(BinaryValue, ProtobufConvert)]
#[protobuf_convert(source = "proto::TestKitServiceInstances")]
struct TestKitServiceInstances {
    artifact_specs: Vec<ArtifactSpec>,
    service_specs: Vec<InstanceInitParams>,
}

/// Creates TestKit instance with specified services and wires public API handlers.
/// The caller is responsible for properly destroying TestKit instance and freeing
/// the memory by calling `nativeFreeTestKit` function.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_testkit_TestKit_nativeCreateTestKit(
    env: JNIEnv,
    _: JObject,
    services: jbyteArray,
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
            builder = builder.with_additional_runtime(runtime);

            let testkit_services = testkit_initialization_data_from_proto(&env, services)?;

            for spec in testkit_services.artifact_specs {
                builder = builder.with_parametric_artifact(spec.artifact, spec.payload);
            }

            for instance in testkit_services.service_specs {
                builder = builder.with_instance(instance);
            }

            if let Some(time_service) =
                time_service_instance_from_java(&env, executor, time_service_spec)?
            {
                let artifact_id = time_service.factory.artifact_id();
                builder = builder
                    .with_artifact(artifact_id)
                    .with_instance(&time_service)
                    .with_rust_service(time_service.factory);
            }

            builder
        };
        let mut testkit = builder.build();
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
    _: JClass,
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
        let access = unsafe { into_erased_access(snapshot) };
        Ok(to_handle(access))
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
        let transactions_count = env.get_array_length(transactions)?;
        let mut raw_transactions = Vec::with_capacity(transactions_count as usize);
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

// Deserializes TestKitServiceInstances from its protobuf representation in format ot Java bytes array.
fn testkit_initialization_data_from_proto(
    env: &JNIEnv,
    services: jbyteArray,
) -> JniResult<TestKitServiceInstances> {
    let services = env.convert_byte_array(services)?;
    Ok(TestKitServiceInstances::from_bytes(services.into()).unwrap())
}

fn serialize_block(env: &JNIEnv, block: Block) -> jni::errors::Result<jbyteArray> {
    let serialized_block = block.into_bytes();
    env.byte_array_from_slice(&serialized_block)
}

fn create_java_keypair<'a>(env: &'a JNIEnv, keypair: KeyPair) -> jni::errors::Result<JValue<'a>> {
    let public_key_byte_array: JObject =
        env.byte_array_from_slice(&keypair.public_key()[..])?.into();
    let secret_key_byte_array: JObject =
        env.byte_array_from_slice(&keypair.secret_key()[..])?.into();
    env.call_static_method(
        KEYPAIR_CLASS,
        "createKeyPair",
        KEYPAIR_CTOR_SIGNATURE,
        &[secret_key_byte_array.into(), public_key_byte_array.into()],
    )
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
) -> JniResult<Option<TimeServiceInstanceParams>> {
    if time_service_spec.is_null() {
        return Ok(None);
    }

    let (service_id, service_name) = get_service_id_and_name(env, time_service_spec)?;
    let time_provider = env
        .get_field(time_service_spec, "timeProvider", TIME_PROVIDER_FIELD_TYPE)?
        .l()?;

    let provider = JavaTimeProvider::new(executor, time_provider);
    let factory = TimeServiceFactory::with_provider(Arc::new(provider) as Arc<dyn TimeProvider>);
    let params = TimeServiceInstanceParams {
        factory,
        service_id,
        service_name,
    };

    Ok(Some(params))
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

/// DTO for time oracle service instantiation parameters.
struct TimeServiceInstanceParams {
    pub factory: TimeServiceFactory,
    pub service_id: u32,
    pub service_name: String,
}

impl<'a> Into<InstanceInitParams> for &'a TimeServiceInstanceParams {
    fn into(self) -> InstanceInitParams {
        InstanceInitParams::new(
            self.service_id,
            self.service_name.clone(),
            self.factory.artifact_id(),
            (),
        )
    }
}
