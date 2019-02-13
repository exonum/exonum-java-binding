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

extern crate futures;
extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;
extern crate failure;

use std::sync::Arc;

use futures::{
    sync::mpsc::{self, Receiver},
    Stream,
};
use integration_tests::vm::create_vm_for_tests_with_fake_classes;
use java_bindings::{
    exonum::{
        blockchain::{Blockchain, Service, Transaction},
        crypto::{gen_keypair, Hash, PublicKey, SecretKey},
        messages::{RawTransaction, ServiceTransaction},
        node::{ApiSender, ExternalMessage},
        storage::{MemoryDB, Snapshot},
    },
    jni::JavaVM,
    MainExecutor, NodeContext,
};

lazy_static! {
    static ref VM: Arc<JavaVM> = create_vm_for_tests_with_fake_classes();
    pub static ref EXECUTOR: MainExecutor = MainExecutor::new(VM.clone());
}

const TEST_TRANSACTION_ID: u16 = 0;
const TEST_TRANSACTION_PAYLOAD: &[u8] = &[1, 2, 3];

#[test]
fn submit_transaction() {
    let keypair = gen_keypair();
    let tx_author = keypair.0;
    let service_id = 0;
    let raw_transaction = create_raw_transaction(service_id);

    let (node, app_rx) = create_node(keypair);
    node.submit(raw_transaction.clone()).unwrap();
    let sent_message = app_rx.wait().next().unwrap().unwrap();

    match sent_message {
        ExternalMessage::Transaction(sent) => {
            let message_payload = sent.payload();
            let message_author = sent.author();
            assert_eq!(&raw_transaction, message_payload);
            assert_eq!(message_author, tx_author);
        }
        _ => panic!("Message is not Transaction"),
    }
}

#[test]
fn submit_transaction_to_missing_service() {
    let keypair = gen_keypair();
    let (node, _) = create_node(keypair);
    // invalid service_id
    let service_id = 1;
    let raw_transaction = create_raw_transaction(service_id);

    let res = node.submit(raw_transaction.clone());
    assert!(res.is_err());
}

fn create_raw_transaction(service_id: u16) -> RawTransaction {
    let service_transaction = ServiceTransaction::from_raw_unchecked(
        TEST_TRANSACTION_ID,
        TEST_TRANSACTION_PAYLOAD.to_vec(),
    );
    RawTransaction::new(service_id, service_transaction)
}

fn create_node(keypair: (PublicKey, SecretKey)) -> (NodeContext, Receiver<ExternalMessage>) {
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

        fn state_hash(&self, _: &Snapshot) -> Vec<Hash> {
            vec![]
        }

        fn tx_from_raw(&self, _: RawTransaction) -> Result<Box<dyn Transaction>, failure::Error> {
            unimplemented!()
        }
    }

    let storage = MemoryDB::new();
    let blockchain = Blockchain::new(
        storage,
        vec![Box::new(EmptyService)],
        keypair.0,
        keypair.1,
        app_tx.clone(),
    );
    let node = NodeContext::new(EXECUTOR.clone(), blockchain, keypair.0, app_tx);
    (node, app_rx)
}
