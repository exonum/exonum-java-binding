use exonum::blockchain::{Blockchain, Transaction};
use exonum::crypto::PublicKey;
use exonum::messages::RawMessage;
use exonum::node::{ApiSender, TransactionSend};
use exonum::storage::Snapshot;
use jni::objects::JClass;
use jni::sys::{jbyteArray, jint, jobject};
use jni::JNIEnv;

use std::error::Error;
use std::{io, panic, ptr};

use proxy::{MainExecutor, TransactionProxy};
use storage::View;
use utils::{
    cast_handle, drop_handle, to_handle, unwrap_exc_or, unwrap_exc_or_default, unwrap_jni_verbose,
    Handle,
};
use JniResult;

const INTERNAL_SERVER_ERROR: &str = "com/exonum/binding/messages/InternalServerError";
const INVALID_TRANSACTION_EXCEPTION: &str =
    "com/exonum/binding/messages/InvalidTransactionException";
const VERIFY_ERROR_MESSAGE: &str = "Unable to verify transaction";

/// An Exonum node context. Allows to add transactions to Exonum network
/// and get a snapshot of the database state.
#[derive(Clone)]
pub struct NodeContext {
    executor: MainExecutor,
    blockchain: Blockchain,
    public_key: PublicKey,
    channel: ApiSender,
}

impl NodeContext {
    /// Creates a node context for a service.
    pub fn new(
        executor: MainExecutor,
        blockchain: Blockchain,
        public_key: PublicKey,
        channel: ApiSender,
    ) -> Self {
        NodeContext {
            executor,
            blockchain,
            public_key,
            channel,
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
    pub fn submit(&self, transaction: Box<Transaction>) -> io::Result<()> {
        self.channel.send(transaction)
    }
}

/// Submits a transaction into the network.
///
/// Parameters:
/// - `node_handle` - a native handle to the native node object
/// - `transaction` - a transaction to submit
/// - `message` - an array containing the transaction message
/// - `offset` - an offset from which the message starts
/// - `size` - a size of the message in bytes
#[no_mangle]
pub extern "system" fn Java_com_exonum_binding_service_NodeProxy_nativeSubmit(
    env: JNIEnv,
    _: JClass,
    node_handle: Handle,
    transaction: jobject,
    message: jbyteArray,
    offset: jint,
    size: jint,
) {
    let res = panic::catch_unwind(|| {
        assert!(offset >= 0, "Offset can't be negative");
        assert!(size >= 0, "Size can't be negative");
        let (offset, size) = (offset as usize, size as usize);
        let node = cast_handle::<NodeContext>(node_handle);
        unwrap_jni_verbose(
            &env,
            || -> JniResult<()> {
                let message = env.convert_byte_array(message)?;
                let message = message[offset..offset + size].to_vec();
                let message = RawMessage::from_vec(message);
                let transaction = env.new_global_ref(transaction.into())?;
                let exec = node.executor().clone();
                let transaction = TransactionProxy::from_global_ref(exec, transaction, message);
                if let Err(err) = node.submit(Box::new(transaction)) {
                    let class;
                    if err.kind() == io::ErrorKind::Other
                        && err.description() == VERIFY_ERROR_MESSAGE
                    {
                        class = INVALID_TRANSACTION_EXCEPTION;
                    } else {
                        class = INTERNAL_SERVER_ERROR;
                    };
                    env.throw_new(class, err.description())?;
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
