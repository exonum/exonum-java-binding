extern crate futures;
extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;

use futures::{Future, Stream};
use futures::sync::mpsc;
use integration_tests::mock::transaction::create_mock_transaction_proxy;
use integration_tests::vm::create_vm_for_tests_with_fake_classes;
use java_bindings::{MainExecutor, NodeContext};
use java_bindings::exonum::blockchain::Blockchain;
use java_bindings::exonum::crypto::gen_keypair;
use java_bindings::exonum::node::ApiSender;
use java_bindings::exonum::storage::MemoryDB;
use java_bindings::jni::JavaVM;

use std::cell::Cell;
use std::sync::{Arc, Barrier};
use std::thread::{sleep, spawn};
use std::time::Duration;

lazy_static! {
    static ref VM: JavaVM = create_vm_for_tests_with_fake_classes();
    pub static ref EXECUTOR: MainExecutor = MainExecutor::new(&VM);
}

#[test]
pub fn node_works_in_concurrent_threads() {
    const ITERS_PER_THREAD: usize = 1_000;
    const THREAD_NUM: usize = 8;

    let iter_delay = Duration::new(0, 100);

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
    let barrier = Arc::new(Barrier::new(THREAD_NUM));
    let mut threads = Vec::new();

    for _ in 0..THREAD_NUM {
        let barrier = Arc::clone(&barrier);
        let mut node = node.clone();

        let jh = spawn(move || {
            barrier.wait();
            for _ in 0..ITERS_PER_THREAD {
                let transaction = create_mock_transaction_proxy(node.executor().clone(), true);
                node.submit(Box::new(transaction)).expect(
                    "Can't submit the transaction",
                );
            }
        });
        threads.push(jh);
    }
    drop(node);

    let sum: Cell<u32> = Cell::new(0);
    let handler = app_rx.for_each(|_raw_message| {
        let value = sum.get();
        sleep(iter_delay);
        sum.set(value + 1);
        Ok(())
    });
    handler.wait().expect("An error in the handler occurred");

    for jh in threads {
        jh.join().unwrap();
    }
    let expected = (ITERS_PER_THREAD * THREAD_NUM) as u32;
    assert_eq!(expected, sum.get());
}
