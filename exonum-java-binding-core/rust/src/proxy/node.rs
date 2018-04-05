use exonum::blockchain::Transaction;
use exonum::crypto::PublicKey;
use exonum::messages::RawMessage;
use exonum::node::{ApiSender, TransactionSend};
use exonum::storage::{Database, Snapshot};
use jni::JNIEnv;
use jni::objects::{GlobalRef, JClass};
use jni::sys::{jbyteArray, jint, jobject};

use std::io;
use std::sync::Arc;

use proxy::{DumbExecutor, TransactionProxy};
use storage::View;
use utils::{cast_handle, drop_handle, Handle, to_handle, unwrap_jni_verbose};

///
/// An Exonum node context. Allows to add transactions to Exonum network
/// and get a snapshot of the database state.
///
#[derive(Clone)]
pub struct NodeContext {
    executor: DumbExecutor,
    db: Arc<Database>,
    public_key: PublicKey,
    channel: ApiSender,
}

impl NodeContext {
    /// Creates a node context for a service.
    pub fn new(
        executor: DumbExecutor,
        db: Arc<Database>,
        public_key: PublicKey,
        channel: ApiSender,
    ) -> Self {
        NodeContext {
            executor,
            db,
            public_key,
            channel,
        }
    }

    #[doc(hidden)]
    pub fn executor(&self) -> &DumbExecutor {
        &self.executor
    }

    #[doc(hidden)]
    pub fn create_snapshot(&self) -> Box<Snapshot> {
        self.db.snapshot()
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
    if let Err(_err) = node.submit(Box::new(transaction)) {
        // FIXME throw an exception on error
        unimplemented!()
    }
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
    _env: JNIEnv,
    _: JClass,
    node_handle: Handle,
) -> Handle {
    let node = cast_handle::<NodeContext>(node_handle);
    let snapshot = node.create_snapshot();
    let view = View::from_owned_snapshot(snapshot);
    to_handle(view)
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
    let node = cast_handle::<NodeContext>(node_handle);
    let public_key = node.public_key();
    unwrap_jni_verbose(&env, env.byte_array_from_slice(public_key.as_ref()))
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
