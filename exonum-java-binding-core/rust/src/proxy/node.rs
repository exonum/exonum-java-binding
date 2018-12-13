use exonum::blockchain::Blockchain;
use exonum::crypto::PublicKey;
use exonum::messages::RawTransaction;
use exonum::messages::ServiceTransaction;
use exonum::node::ApiSender;
use exonum::storage::Snapshot;
use failure;
use jni::objects::JClass;
use jni::sys::{jbyteArray, jshort};
use jni::JNIEnv;

use std::{panic, ptr};

use proxy::MainExecutor;
use storage::View;
use utils::{
    cast_handle, drop_handle, to_handle, unwrap_exc_or, unwrap_exc_or_default, unwrap_jni_verbose,
    Handle,
};
use JniResult;

const INTERNAL_SERVER_ERROR: &str = "com/exonum/binding/service/InternalServerError";

/// An Exonum node context. Allows to add transactions to Exonum network
/// and get a snapshot of the database state.
#[derive(Clone)]
pub struct NodeContext {
    executor: MainExecutor,
    blockchain: Blockchain,
    public_key: PublicKey,
    transaction_sender: ApiSender,
}

impl NodeContext {
    /// Creates a node context for a service.
    pub fn new(
        executor: MainExecutor,
        blockchain: Blockchain,
        public_key: PublicKey,
        transaction_sender: ApiSender,
    ) -> Self {
        NodeContext {
            executor,
            blockchain,
            public_key,
            transaction_sender,
        }
    }

    #[doc(hidden)]
    pub fn executor(&self) -> &MainExecutor {
        &self.executor
    }

    #[doc(hidden)]
    pub fn create_snapshot(&self) -> Box<Snapshot> {
        self.blockchain.snapshot()
    }

    #[doc(hidden)]
    pub fn public_key(&self) -> PublicKey {
        self.public_key
    }

    #[doc(hidden)]
    pub fn submit(&self, transaction: RawTransaction) -> Result<(), failure::Error> {
        self.blockchain.broadcast_raw_transaction(transaction)
    }
}

/// Submits a transaction into the network.
///
/// Parameters:
/// - `node_handle` - a native handle to the native node object
/// - `transaction` - a transaction to submit
/// - `payload` - an array containing the transaction payload
/// - `service_id` - an identifier of the service
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_service_NodeProxy_nativeSubmit(
    env: JNIEnv,
    _: JClass,
    node_handle: Handle,
    payload: jbyteArray,
    service_id: jshort,
    transaction_id: jshort,
) {
    let res = panic::catch_unwind(|| {
        let node = cast_handle::<NodeContext>(node_handle);
        unwrap_jni_verbose(
            &env,
            || -> JniResult<()> {
                let payload = env.convert_byte_array(payload)?;
                let service_transaction =
                    ServiceTransaction::from_raw_unchecked(transaction_id as u16, payload);
                let raw_transaction = RawTransaction::new(service_id as u16, service_transaction);
                if let Err(err) = node.submit(raw_transaction) {
                    let error_class = INTERNAL_SERVER_ERROR;
                    let error_description = err.to_string();
                    env.throw_new(error_class, error_description)?;
                }
                Ok(())
            }(),
        );
        Ok(())
    });
    unwrap_exc_or_default(&env, res);
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
pub extern "system" fn Java_com_exonum_binding_service_NodeProxy_nativeCreateSnapshot(
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
pub extern "system" fn Java_com_exonum_binding_service_NodeProxy_nativeGetPublicKey(
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
pub extern "system" fn Java_com_exonum_binding_service_NodeProxy_nativeFree(
    env: JNIEnv,
    _: JClass,
    node_handle: Handle,
) {
    drop_handle::<NodeContext>(&env, node_handle);
}
