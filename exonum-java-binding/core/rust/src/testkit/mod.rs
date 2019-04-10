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

use exonum::{
    crypto::{PublicKey, SecretKey},
    messages::{BinaryForm, RawTransaction, Signed},
    storage::StorageValue,
};
use exonum_testkit::{TestKit, TestKitBuilder};
use jni::{
    objects::{JList, JObject, JValue},
    sys::{jboolean, jbyteArray, jshort},
    JNIEnv,
};
use proxy::{MainExecutor, ServiceProxy};
use std::{panic, sync::Arc};
use storage::View;
use utils::{cast_handle, to_handle, unwrap_exc_or, unwrap_exc_or_default, unwrap_jni, Handle};

const KEYPAIR_CLASS: &str = "com/exonum/binding/common/crypto/KeyPair";
const KEYPAIR_CTOR_SIGNATURE: &str = "([B[B)Lcom/exonum/binding/common/crypto/KeyPair;";
const EMULATED_NODE_CLASS: &str = "com/exonum/binding/testkit/EmulatedNode";
const EMULATED_NODE_CTOR_SIGNATURE: &str =
    "(ILcom/exonum/binding/common/crypto/KeyPair;)Lcom/exonum/binding/testkit/EmulatedNode;";

/// Creates TestKit instance with specified services and wires public API handlers.
/// Created instance is considered static and lives till the end of the program.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_testkit_TestKit_nativeCreateTestKit(
    env: JNIEnv,
    _: JObject,
    services: JList,
    auditor: jboolean,
    validator_count: jshort,
    _time_provider: JObject,
) -> Handle {
    let mut builder = if auditor == jni::sys::JNI_TRUE {
        TestKitBuilder::auditor()
    } else {
        TestKitBuilder::validator()
    };
    builder = builder.with_validators(validator_count as _);
    let builder = unwrap_jni((move || {
        let executor = MainExecutor::new(Arc::new(env.get_java_vm()?));
        for service in services.iter()? {
            let global_ref = env.new_global_ref(service)?;
            let service = ServiceProxy::from_global_ref(executor.clone(), global_ref);
            builder = builder.with_service(service);
        }
        Ok(builder)
    })());
    let testkit = Box::new(builder.create());
    // Mount API handlers
    testkit.api();
    // Make TestKit instance static
    let static_ref = Box::leak(testkit);
    to_handle(static_ref)
}

/// Creates Snapshot using provided TestKit instance.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_testkit_TestKit_nativeCreateSnapshot(
    env: JNIEnv,
    _: JObject,
    handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let testkit = cast_handle::<Box<TestKit>>(handle);
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
        let testkit = cast_handle::<Box<TestKit>>(handle);
        let block = testkit.create_block().header;
        let serialized_block = block.encode().unwrap();
        let byte_array = env.byte_array_from_slice(&serialized_block)?;
        Ok(byte_array)
    });
    unwrap_exc_or(&env, res, std::ptr::null_mut())
}

/// Creates Block with specified list of transactions and returns its header.
#[no_mangle]
#[rustfmt::skip]
pub extern "system" fn Java_com_exonum_binding_testkit_TestKit_nativeCreateBlockWithTransactions<'e>(
    env: JNIEnv<'e>,
    _: JObject,
    handle: Handle,
    transactions: JList,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let testkit = cast_handle::<Box<TestKit>>(handle);
        let mut raw_transactions = Vec::new();
        for object in transactions.iter()? {
            let java_byte_array: jbyteArray = object.into_inner().into();
            let byte_array = env.convert_byte_array(java_byte_array)?;
            let transaction: Signed<RawTransaction> = StorageValue::from_bytes(byte_array.into());
            raw_transactions.push(transaction);
        }
        let block = testkit
            .create_block_with_transactions(raw_transactions.into_iter())
            .header;
        let serialized_block = block.encode().unwrap();
        let byte_array = env.byte_array_from_slice(&serialized_block)?;
        Ok(byte_array)
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
        let testkit = cast_handle::<Box<TestKit>>(handle);
        let emulated_node = testkit.us();
        // Validator id == 0 in case of auditor node.
        let validator_id = emulated_node.validator_id().map(|id| id.0).unwrap_or(0);
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

fn create_java_keypair<'a>(
    env: &'a JNIEnv,
    keypair: (&PublicKey, &SecretKey),
) -> jni::errors::Result<JValue<'a>> {
    let public_key_byte_array: JObject = env.byte_array_from_slice(&keypair.0[..])?.into();
    let secret_key_byte_array: JObject = env.byte_array_from_slice(&keypair.1[..])?.into();
    env.call_static_method(
        KEYPAIR_CLASS,
        KEYPAIR_CTOR_SIGNATURE,
        "createKeyPair",
        &[public_key_byte_array.into(), secret_key_byte_array.into()],
    )
}
