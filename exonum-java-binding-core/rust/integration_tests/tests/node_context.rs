extern crate futures;
extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;
extern crate failure;

use std::sync::Arc;

use futures::sync::mpsc::{self, Receiver};
use futures::Stream;
use integration_tests::vm::create_vm_for_tests_with_fake_classes;
use java_bindings::exonum::blockchain::{Blockchain, Service, Transaction};
use java_bindings::exonum::crypto::{gen_keypair, Hash};
use java_bindings::exonum::messages::{BinaryForm, RawTransaction, ServiceTransaction};
use java_bindings::exonum::node::{ApiSender, ExternalMessage};
use java_bindings::exonum::storage::{MemoryDB, Snapshot};
use java_bindings::jni::objects::JObject;
use java_bindings::jni::{JNIEnv, JavaVM};
use java_bindings::utils::{
    as_handle, get_and_clear_java_exception, get_class_name, unwrap_jni, unwrap_jni_verbose,
};
use java_bindings::{
    Java_com_exonum_binding_service_NodeProxy_nativeSubmit, JniExecutor, JniResult, MainExecutor,
    NodeContext,
};

lazy_static! {
    static ref VM: Arc<JavaVM> = create_vm_for_tests_with_fake_classes();
    pub static ref EXECUTOR: MainExecutor = MainExecutor::new(VM.clone());
}

#[test]
fn submit_transaction() {
    let (mut node, app_rx) = create_node();
    let raw_transaction =
        RawTransaction::new(0, ServiceTransaction::from_raw_unchecked(0, vec![1, 2, 3]));
    node.submit(raw_transaction.clone()).unwrap();
    let sent_message = app_rx.wait().next().unwrap().unwrap();
    match sent_message {
        ExternalMessage::Transaction(sent) => assert_eq!(&raw_transaction, sent.payload()),
        _ => panic!("Message is not Transaction"),
    }
}

fn create_node() -> (NodeContext, Receiver<ExternalMessage>) {
    let service_keypair = gen_keypair();
    let api_channel = mpsc::channel(128);
    let (app_tx, app_rx) = (ApiSender::new(api_channel.0), api_channel.1);

    struct EmptyService;

    impl Service for EmptyService {
        fn service_id(&self) -> u16 {
            0
        }

        fn service_name(&self) -> &str {
            "empty_service"
        }

        fn state_hash(&self, snapshot: &Snapshot) -> Vec<Hash> {
            vec![]
        }

        fn tx_from_raw(&self, raw: RawTransaction) -> Result<Box<dyn Transaction>, failure::Error> {
            unimplemented!()
        }
    }

    let storage = MemoryDB::new();
    let blockchain = Blockchain::new(
        storage,
        vec![Box::new(EmptyService)],
        service_keypair.0,
        service_keypair.1,
        app_tx.clone(),
    );
    let node = NodeContext::new(EXECUTOR.clone(), blockchain, service_keypair.0, app_tx);
    (node, app_rx)
}

fn message_from_raw<'e>(env: &'e JNIEnv<'e>, buffer: &[u8]) -> JniResult<JObject<'e>> {
    env.byte_array_from_slice(buffer).map(JObject::from)
}
