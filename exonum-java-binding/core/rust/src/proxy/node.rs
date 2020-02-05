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
    blockchain::Blockchain,
    crypto::{Hash, PublicKey},
    messages::Verified,
    runtime::{AnyTx, CallInfo},
};
use exonum_merkledb::{ObjectHash, Snapshot};
use failure;
use futures::Future;
use jni::objects::JClass;
use jni::sys::{jbyteArray, jint};
use jni::JNIEnv;

use std::{panic, ptr};

use handle::{cast_handle, drop_handle, to_handle, Handle};
use storage::into_erased_access;
use utils::{unwrap_exc_or, unwrap_exc_or_default, unwrap_jni_verbose};
use JniResult;

const TX_SUBMISSION_EXCEPTION: &str =
    "com/exonum/binding/core/service/TransactionSubmissionException";

/// An Exonum node context. Allows to add transactions to Exonum network
/// and get a snapshot of the database state.
#[derive(Clone)]
pub struct Node {
    blockchain: Blockchain,
}

impl Node {
    /// Creates a node context for a service.
    pub fn new(blockchain: Blockchain) -> Self {
        Node { blockchain }
    }

    #[doc(hidden)]
    pub fn create_snapshot(&self) -> Box<dyn Snapshot> {
        self.blockchain.snapshot()
    }

    #[doc(hidden)]
    pub fn public_key(&self) -> PublicKey {
        self.blockchain.service_keypair().public_key()
    }

    #[doc(hidden)]
    pub fn submit(&self, tx: AnyTx) -> Result<Hash, failure::Error> {
        let keypair = self.blockchain.service_keypair();

        let verified = Verified::from_value(tx, keypair.public_key(), keypair.secret_key());
        let tx_hash = verified.object_hash();
        // TODO(ECR-3679): check Core behaviour/any errors on service inactivity
        self.blockchain
            .sender()
            .broadcast_transaction(verified)
            .wait()?;
        Ok(tx_hash)
    }
}

/// Submits a transaction into the network. Returns transaction hash as byte array.
///
/// Parameters:
/// - `node_handle` - a native handle to the native node object
/// - `method_id` - an identifier method within the service
/// - `arguments` - an array containing the transaction arguments
/// - `instance_id` - an identifier of the service
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_runtime_NodeProxy_nativeSubmit(
    env: JNIEnv,
    _: JClass,
    node_handle: Handle,
    arguments: jbyteArray,
    instance_id: jint,
    method_id: jint,
) -> jbyteArray {
    use utils::convert_hash;
    let res = panic::catch_unwind(|| {
        let node = cast_handle::<Node>(node_handle);
        let hash = unwrap_jni_verbose(
            &env,
            || -> JniResult<jbyteArray> {
                let call_info = CallInfo::new(instance_id as u32, method_id as u32);
                let args = env.convert_byte_array(arguments)?;
                let tx = AnyTx::new(call_info, args);

                match node.submit(tx) {
                    Ok(tx_hash) => convert_hash(&env, &tx_hash),
                    Err(err) => {
                        // node#submit can fail on an error in ApiSender#send
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
pub extern "system" fn Java_com_exonum_binding_core_runtime_NodeProxy_nativeCreateSnapshot(
    env: JNIEnv,
    _: JClass,
    node_handle: Handle,
) -> Handle {
    let res = panic::catch_unwind(|| {
        let node = cast_handle::<Node>(node_handle);
        let snapshot = node.create_snapshot();
        let access = unsafe { into_erased_access(snapshot) };
        Ok(to_handle(access))
    });
    unwrap_exc_or_default(&env, res)
}

/// Returns the public key of this node.
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_core_runtime_NodeProxy_nativeGetPublicKey(
    env: JNIEnv,
    _: JClass,
    node_handle: Handle,
) -> jbyteArray {
    let res = panic::catch_unwind(|| {
        let node = cast_handle::<Node>(node_handle);
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
pub extern "system" fn Java_com_exonum_binding_core_runtime_NodeProxy_nativeFree(
    env: JNIEnv,
    _: JClass,
    node_handle: Handle,
) {
    drop_handle::<Node>(&env, node_handle);
}
