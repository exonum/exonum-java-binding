extern crate java_bindings;
extern crate jni;
#[macro_use]
extern crate lazy_static;
extern crate exonum;

mod util;

use exonum::blockchain::Transaction;
use exonum::storage::Database;
use exonum::storage::MemoryDB;
use java_bindings::{DumbExecutor, Executor, TransactionProxy};
use jni::JavaVM;
use jni::objects::{AutoLocal, JValue};

use std::sync::Arc;

use util::create_vm;

lazy_static! {
    pub static ref VM: Arc<JavaVM> = Arc::new(create_vm(true));
}

fn create_tx_proxy<E: Executor>(executor: E, valid: bool) -> TransactionProxy<E> {
    let adapter = executor.with_attached(|env| {
        let value = env.call_static_method(
            "com/exonum/binding/fakes/services/NativeAdapterFakes",
            "createTransactionMock",
            "(Z)Lcom/exonum/binding/service/adapters/UserTransactionAdapter;",
            &[JValue::from(valid)],
        )?;
        let local_ref = AutoLocal::new(env, value.l().unwrap());
        env.new_global_ref(local_ref.as_obj())
    }).unwrap();
    unsafe { TransactionProxy::from_global_ref(executor, adapter) }
}

#[test]
pub fn valid_verify() {
    let executor = DumbExecutor { vm: VM.clone() };
    let valid_tx = create_tx_proxy(executor, true);
    assert_eq!(true, valid_tx.verify());
}

#[test]
pub fn invalid_verify() {
    let executor = DumbExecutor { vm: VM.clone() };
    let invalid_tx = create_tx_proxy(executor, false);
    assert_eq!(false, invalid_tx.verify());
}

#[test]
pub fn valid_execute() {
    let executor = DumbExecutor { vm: VM.clone() };
    let valid_tx = create_tx_proxy(executor, true);
    let db = MemoryDB::new();
    let _res = valid_tx.execute(&mut db.fork());
    // TODO implement check for changes in DB. But `execute` do nothing in the mock now.
    // assert_eq!(result, expected);
}

#[test]
pub fn invalid_execute() {
    let executor = DumbExecutor { vm: VM.clone() };
    let invalid_tx = create_tx_proxy(executor, false);
    let db = MemoryDB::new();
    let _res = invalid_tx.execute(&mut db.fork());
    // TODO implement check for changes in DB. But `execute` do nothing in the mock now.
    // assert_eq!(result, expected);
}
