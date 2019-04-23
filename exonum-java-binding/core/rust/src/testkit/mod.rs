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

use exonum::{
    blockchain::Block,
    crypto::{PublicKey, SecretKey},
    helpers::ValidatorId,
    messages::{BinaryForm, RawTransaction, Signed},
    storage::StorageValue,
};
use exonum_testkit::{TestKit, TestKitBuilder};
use exonum_time::{time_provider::TimeProvider, TimeService};
use jni::{
    objects::{JObject, JValue},
    sys::{jboolean, jbyteArray, jobjectArray, jshort},
    JNIEnv,
};
use proxy::{MainExecutor, ServiceProxy};
use std::{panic, sync::Arc};
use storage::View;
use testkit::time_provider::JavaTimeProvider;
use utils::{cast_handle, drop_handle, to_handle, unwrap_exc_or, unwrap_exc_or_default, Handle};

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
    time_provider: JObject,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let mut builder = if auditor == jni::sys::JNI_TRUE {
            TestKitBuilder::auditor()
        } else {
            TestKitBuilder::validator()
        };
        builder = builder.with_validators(validator_count as _);
        let builder = {
            let executor = MainExecutor::new(Arc::new(env.get_java_vm()?));
            let num_services = env.get_array_length(services)?;
            for i in 0..num_services {
                let service = env.get_object_array_element(services, i)?;
                let global_ref = env.new_global_ref(service)?;
                let service = ServiceProxy::from_global_ref(executor.clone(), global_ref);
                builder = builder.with_service(service);
            }

            // Handle Time Service
            if !time_provider.is_null() {
                let provider = JavaTimeProvider::new(executor.clone(), time_provider);
                builder = builder.with_service(TimeService::with_provider(
                    Box::new(provider) as Box<TimeProvider>
                ));
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
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_testkit_TestKit_nativeCreateSnapshot(
    env: JNIEnv,
    _: JObject,
    handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let testkit = cast_handle::<TestKit>(handle);
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
            let transaction: Signed<RawTransaction> =
                StorageValue::from_bytes(serialized_tx.into());
            raw_transactions.push(transaction);
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
    let serialized_block = block.encode().unwrap();
    env.byte_array_from_slice(&serialized_block)
}

fn create_java_keypair<'a>(
    env: &'a JNIEnv,
    keypair: (&PublicKey, &SecretKey),
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
