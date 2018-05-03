extern crate futures;
extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;

use futures::Stream;
use futures::sync::mpsc::{self, Receiver};
use integration_tests::mock::transaction::create_mock_transaction;
use integration_tests::vm::create_vm_for_tests_with_fake_classes;
use java_bindings::{JniExecutor, JniResult, MainExecutor, NodeContext,
                    Java_com_exonum_binding_service_NodeProxy_nativeSubmit};
use java_bindings::exonum::blockchain::Blockchain;
use java_bindings::exonum::crypto::gen_keypair;
use java_bindings::exonum::messages::RawMessage;
use java_bindings::exonum::node::{ApiSender, ExternalMessage};
use java_bindings::exonum::storage::MemoryDB;
use java_bindings::jni::{JavaVM, JNIEnv};
use java_bindings::jni::objects::{AutoLocal, JObject};
use java_bindings::utils::{as_handle, get_and_clear_java_exception, get_class_name, unwrap_jni,
                           unwrap_jni_verbose};

lazy_static! {
    static ref VM: JavaVM = create_vm_for_tests_with_fake_classes();
    pub static ref EXECUTOR: MainExecutor = MainExecutor::new(&VM);
}

#[test]
pub fn submit_valid_transaction() {
    let jclass = JObject::null().into();
    let (mut node, app_rx) = create_node();
    let node_handle_guard = as_handle(&mut node);
    let node_handle = node_handle_guard.get();
    let (java_transaction, _raw_message) = create_mock_transaction(EXECUTOR.clone(), true);
    let marker_raw = RawMessage::from_vec(vec![1, 2, 3]);
    let raw_message = marker_raw.clone();
    unwrap_jni(EXECUTOR.with_attached(move |env: &JNIEnv| {
        Ok(unwrap_jni_verbose(
            &env,
            (|| {
                let message = message_from_raw(env, &raw_message)?;
                Java_com_exonum_binding_service_NodeProxy_nativeSubmit(
                    env.clone(),
                    jclass,
                    node_handle,
                    *java_transaction.as_obj(),
                    *message.as_obj(),
                    0,
                    raw_message.len() as i32,
                );
                let exception: JObject = env.exception_occurred()?.into();
                assert!(exception.is_null());
                Ok(())
            })(),
        ))
    }));
    let sent_message = app_rx.wait().next().unwrap().unwrap();
    match sent_message {
        ExternalMessage::Transaction(sent) => assert_eq!(&marker_raw, sent.raw()),
        _ => panic!("Message is not Transaction"),
    }
}

#[test]
pub fn submit_not_valid_transaction() {
    const INVALID_TRANSACTION_EXCEPTION: &str = "com.exonum.binding.messages.InvalidTransactionException";

    let jclass = JObject::null().into();
    let (mut node, _app_rx) = create_node();
    let node_handle_guard = as_handle(&mut node);
    let node_handle = node_handle_guard.get();
    let (java_transaction, raw_message) = create_mock_transaction(EXECUTOR.clone(), false);
    unwrap_jni(EXECUTOR.with_attached(|env: &JNIEnv| {
        Ok(unwrap_jni_verbose(
            &env,
            (|| {
                let message = message_from_raw(env, &raw_message)?;
                Java_com_exonum_binding_service_NodeProxy_nativeSubmit(
                    env.clone(),
                    jclass,
                    node_handle,
                    *java_transaction.as_obj(),
                    *message.as_obj(),
                    0,
                    raw_message.len() as i32,
                );
                let exception = get_and_clear_java_exception(&env);
                let exception = env.auto_local(exception.into());
                assert_eq!(
                    get_class_name(&env, exception.as_obj())?,
                    INVALID_TRANSACTION_EXCEPTION
                );
                Ok(())
            })(),
        ))
    }));
}

fn create_node() -> (NodeContext, Receiver<ExternalMessage>) {
    let service_keypair = gen_keypair();
    let api_channel = mpsc::channel(128);
    let (app_tx, app_rx) = (ApiSender::new(api_channel.0), api_channel.1);

    let storage = MemoryDB::new();
    let blockchain = Blockchain::new(
        storage,
        vec![],
        service_keypair.0,
        service_keypair.1,
        app_tx.clone(),
    );
    let node = NodeContext::new(EXECUTOR.clone(), blockchain, service_keypair.0, app_tx);
    (node, app_rx)
}

fn message_from_raw<'e, R>(env: &'e JNIEnv<'e>, raw_message: R) -> JniResult<AutoLocal<'e>>
where
    R: AsRef<[u8]>,
{
    let message = env.byte_array_from_slice(raw_message.as_ref())?;
    Ok(env.auto_local(message.into()))
}
