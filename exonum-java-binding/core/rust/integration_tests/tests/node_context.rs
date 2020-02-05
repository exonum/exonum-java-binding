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
use integration_tests::vm::create_vm_for_tests_with_classes;
use java_bindings::{
    exonum::{
        blockchain::{ApiSender, Blockchain},
        crypto::{gen_keypair, PublicKey, SecretKey},
        messages::Verified,
        runtime::{AnyTx, CallInfo},
    },
    exonum_merkledb::TemporaryDB,
    jni::JavaVM,
    Executor, Node,
};

lazy_static! {
    static ref VM: Arc<JavaVM> = create_vm_for_tests_with_classes();
    pub static ref EXECUTOR: Executor = Executor::new(VM.clone());
}

const TEST_TRANSACTION_ID: u32 = 0;
const TEST_TRANSACTION_PAYLOAD: &[u8] = &[1, 2, 3];

#[test]
fn submit_transaction() {
    let keypair = gen_keypair();
    let tx_author = keypair.0;
    let service_id = 0;
    let raw_transaction = create_transaction(service_id);

    let (node, app_rx) = create_node(keypair);
    node.submit(raw_transaction.clone()).unwrap();
    let sent_message = app_rx.wait().next().unwrap().unwrap();

    let message_payload = sent_message.payload();
    let message_author = sent_message.author();
    assert_eq!(raw_transaction, *message_payload);
    assert_eq!(message_author, tx_author);
}

#[test]
fn submit_transaction_to_missing_service() {
    let keypair = gen_keypair();
    let (node, _) = create_node(keypair);
    // invalid service_id
    let service_id = 1;
    let transaction = create_transaction(service_id);

    let res = node.submit(transaction);
    assert!(res.is_err());
}

fn create_transaction(instance_id: u32) -> AnyTx {
    AnyTx::new(
        CallInfo::new(instance_id, TEST_TRANSACTION_ID),
        TEST_TRANSACTION_PAYLOAD.to_vec(),
    )
}

fn create_node(keypair: (PublicKey, SecretKey)) -> (Node, Receiver<Verified<AnyTx>>) {
    let api_channel = mpsc::channel(128);
    let (app_tx, app_rx) = (ApiSender::new(api_channel.0), api_channel.1);

    let storage = TemporaryDB::new();
    let blockchain = Blockchain::new(storage, keypair, app_tx);
    let node = Node::new(blockchain);

    (node, app_rx)
}
