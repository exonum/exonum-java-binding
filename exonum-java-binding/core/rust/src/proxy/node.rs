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
    api::ApiContext,
    crypto::{Hash, PublicKey},
    messages::{AnyTx, Verified},
};
use exonum_merkledb::{Snapshot, ObjectHash};
use failure;
use jni::objects::JClass;
use jni::sys::{jbyteArray, jshort};
use jni::{Executor, JNIEnv};

use std::{panic, ptr};

use handle::{cast_handle, drop_handle, to_handle, Handle};
use storage::View;
use utils::{unwrap_exc_or, unwrap_exc_or_default, unwrap_jni_verbose};
use JniResult;
use exonum::runtime::CallInfo;

const TX_SUBMISSION_EXCEPTION: &str =
    "com/exonum/binding/core/service/TransactionSubmissionException";

/// An Exonum node context. Allows to add transactions to Exonum network
/// and get a snapshot of the database state.
#[derive(Clone)]
pub struct NodeContext {
    executor: Executor,
    api_context: ApiContext,
}

impl NodeContext {
    /// Creates a node context for a service.
    pub fn new(
        executor: Executor,
        api_context: ApiContext,
    ) -> Self {
        NodeContext {
            executor,
            api_context,
        }
    }

    #[doc(hidden)]
    pub fn executor(&self) -> &Executor {
        &self.executor
    }

    #[doc(hidden)]
    pub fn create_snapshot(&self) -> Box<Snapshot> {
        self.api_context.snapshot()
    }

    #[doc(hidden)]
    pub fn public_key(&self) -> PublicKey {
        self.api_context.service_keypair().0.clone()
    }

    #[doc(hidden)]
    pub fn submit(&self, transaction: AnyTx) -> Result<Hash, failure::Error> {
        // TODO: check service is active
        let _service_id = transaction.call_info.instance_id;
//        if !self.blockchain.service_map().contains_key(&service_id) {
//            return Err(format_err!(
//                "Unable to broadcast transaction: service(ID={}) not found",
//                service_id
//            ));
//        }

        let key_pair = self.api_context.service_keypair();
        let verified = Verified::from_value(
            transaction,
            key_pair.0.to_owned(),
            key_pair.1,
        );
        let tx_hash = verified.object_hash();

        self.api_context.sender().broadcast_transaction(verified)?;
        Ok(tx_hash)
    }
}

/// Submits a transaction into the network. Returns transaction hash as byte array.
///
/// Parameters:
/// - `node_handle` - a native handle to the native node object
/// - `transaction` - a transaction to submit
/// - `payload` - an array containing the transaction payload
/// - `service_id` - an identifier of the service
//  todo: [ECR-3438]
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_service_NodeProxy_nativeSubmit(
    env: JNIEnv,
    _: JClass,
    node_handle: Handle,
    payload: jbyteArray,
    service_id: jshort,
    transaction_id: jshort,
) -> jbyteArray {
    use utils::convert_hash;
    let res = panic::catch_unwind(|| {
        let node = cast_handle::<NodeContext>(node_handle);
        let hash = unwrap_jni_verbose(
            &env,
            || -> JniResult<jbyteArray> {
                let payload = env.convert_byte_array(payload)?;
                //  todo: [ECR-3438]
                let any_tx = AnyTx {
                    call_info: CallInfo {
                        instance_id: service_id as u32,
                        method_id: transaction_id as u32,
                    },
                    arguments: payload,
                };

                match node.submit(any_tx) {
                    Ok(tx_hash) => convert_hash(&env, &tx_hash),
                    Err(err) => {
                        // node#submit can fail for two reasons: unknown transaction id and
                        // an error in ApiSender#send. The former is the service implementation
                        // error; the latter is an internal, unrecoverable error.
                        let error_class = TX_SUBMISSION_EXCEPTION;
                        let error_description = err.to_string();
                        env.throw_new(error_class, error_description)?;
                        Ok(ptr::null_mut())
                    }
                }
            }(),
        );
        Ok(hash)
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Creates a new snapshot of the current database state.
///
/// The snapshot must be explicitly destroyed by the caller from Java.
///
/// Parameters:
/// - `node_handle` - a native handle to the native node object
///
/// Returns a `Snapshot` of the database state
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_service_NodeProxy_nativeCreateSnapshot(
    env: JNIEnv,
    _: JClass,
    node_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let node = cast_handle::<NodeContext>(node_handle);
        let snapshot = node.create_snapshot();
        let view = View::from_owned_snapshot(snapshot);
        Ok(to_handle(view))
    });
    unwrap_exc_or_default(&env, res)
}

/// Returns the public key of this node.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_service_NodeProxy_nativeGetPublicKey(
    env: JNIEnv,
    _: JClass,
    node_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let node = cast_handle::<NodeContext>(node_handle);
        let public_key = node.public_key();
        Ok(unwrap_jni_verbose(
            &env,
            env.byte_array_from_slice(public_key.as_ref()),
        ))
    });
    unwrap_exc_or(&env, res, ptr::null_mut())
}

/// Destroys node context.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_service_NodeProxy_nativeFree(
    env: JNIEnv,
    _: JClass,
    node_handle: Handle,
) {
    drop_handle::<NodeContext>(&env, node_handle);
}
