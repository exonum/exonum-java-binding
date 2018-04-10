use exonum::blockchain::{Blockchain, Transaction};
use exonum::crypto::PublicKey;
use exonum::messages::RawMessage;
use exonum::node::{ApiSender, TransactionSend};
use exonum::storage::Snapshot;
use jni::JNIEnv;
use jni::objects::{GlobalRef, JClass};
use jni::sys::{jbyteArray, jint, jobject};

use std::error::Error;
use std::{io, panic, ptr};

use proxy::{MainExecutor, TransactionProxy};
use storage::View;
use utils::{cast_handle, drop_handle, Handle, to_handle, unwrap_exc_or_default, unwrap_exc_or,
            unwrap_jni_verbose};

const INTERNAL_SERVER_ERROR: &str = "com/exonum/binding/messages/InternalServerError";
const INVALID_TRANSACTION_EXCEPTION: &str = "com/exonum/binding/messages/InvalidTransactionException";
const VERIFY_ERROR: &str = "Unable to verify transaction";

///
/// An Exonum node context. Allows to add transactions to Exonum network
/// and get a snapshot of the database state.
///
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

///
/// Submits a transaction into the network.
///
/// Parameters:
/// - `node_handle` - a native handle to the native node object
/// - `transaction` - a transaction to submit
/// - `message` - an array containing the transaction message
/// - `offset` - an offset from which the message starts
/// - `size` - a size of the message in bytes
/*
 * Class:     com.exonum.binding.service.NodeProxy
 * Method:    nativeSubmit
 * Signature: (JLcom/exonum/binding/messages/Transaction;[BII)V
 */
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
        let node = cast_handle::<NodeContext>(node_handle);
        let message = unwrap_jni_verbose(&env, env.convert_byte_array(message));
        // TODO нужно ли?
        let message = message[offset as usize..(offset + size) as usize].to_vec();
        let message = RawMessage::from_vec(message);
        let vm = unwrap_jni_verbose(&env, env.get_java_vm());
        let transaction = unsafe { GlobalRef::from_raw(vm, transaction) };
        let exec = node.executor().clone();
        let transaction = TransactionProxy::from_global_ref(exec, transaction, message);
        if let Err(err) = node.submit(Box::new(transaction)) {
            let class =
                if err.kind() == io::ErrorKind::Other && err.description() == VERIFY_ERROR {
                    INVALID_TRANSACTION_EXCEPTION
                } else {
                    INTERNAL_SERVER_ERROR
                };
            unwrap_jni_verbose(&env, env.throw_new(class, err.description()));
        }
        Ok(())
    });
    unwrap_exc_or_default(&env, res);
}

///
/// Creates a new snapshot of the current database state.
///
/// The caller is responsible to **close** the snapshot
/// to destroy the corresponding native objects.
///
/// Parameters:
/// - `node_handle` - a native handle to the native node object
///
/// Returns a `Snapshot` of the database state
///
/*
 * Class:     com.exonum.binding.service.NodeProxy
 * Method:    nativeCreateSnapshot
 * Signature: (J)J
 */
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

///
/// Returns the public key of this node.
///
/// Throws `IllegalStateException` if the node proxy is closed
///
/*
 * Class:     com.exonum.binding.service.NodeProxy
 * Method:    nativeGetPublicKey
 * Signature: (J)[B
 */
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
