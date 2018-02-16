extern crate java_bindings;
extern crate jni;
#[macro_use]
extern crate lazy_static;
extern crate exonum;

mod util;

use exonum::blockchain::Transaction;
use exonum::messages::{MessageBuffer, RawMessage};
use exonum::storage::{Database, Entry, MemoryDB};
use java_bindings::{DumbExecutor, Executor, TransactionProxy};
use jni::JavaVM;
use jni::objects::{AutoLocal, JObject, JValue};

use std::sync::Arc;

use util::create_vm;

static ENTRY_NAME: &str = "test_entry";
static ENTRY_VALUE: &str = "test_value";

lazy_static! {
    pub static ref VM: Arc<JavaVM> = Arc::new(create_vm(true));
}

fn create_transaction_mock<E: Executor>(executor: E, valid: bool) -> TransactionProxy<E> {
    let (adapter, raw) = executor.with_attached(|env| {
        let value = env.new_string(ENTRY_VALUE)?;
        let info = env.new_string("")?;
        let adapter = env.call_static_method(
            "com/exonum/binding/fakes/NativeFacade",
            "createTransaction",
            "(ZLjava/lang/String;Ljava/lang/String;)\
            Lcom/exonum/binding/service/adapters/UserTransactionAdapter;",
            &[
                JValue::from(valid),
                JValue::from(JObject::from(value)),
                JValue::from(JObject::from(info))
            ],
        )?;
        let adapter = env.new_global_ref(AutoLocal::new(env, adapter.l()?).as_obj())?;

//        let binary_message = env.call_method(
//            adapter.as_obj(),
//            "getMessage",
//            "()Lcom/exonum/binding/messages/BinaryMessage;",
//            &[],
//        )?;
//        let _binary_message = AutoLocal::new(env, binary_message.l()?);
//
//        let byte_buffer = env.call_method(
//            binary_message.as_obj(),
//            "getBytes",
//            "()[B",
//            &[],
//        )?;
//        let byte_buffer = AutoLocal::new(env, byte_buffer.l()?);

        // TODO remove this stub and get a real byte buffer as soon as it implemented
        let raw = RawMessage::new(MessageBuffer::from_vec(vec![]));
        Ok((adapter, raw))
    }).unwrap();

    unsafe { TransactionProxy::from_global_ref(executor, adapter, raw) }
}


fn create_entry<V>(view: V) -> Entry<V, String> {
    Entry::new(ENTRY_NAME, view)
}


#[test]
pub fn verify_valid() {
    let executor = DumbExecutor { vm: VM.clone() };
    let valid_tx = create_transaction_mock(executor, true);
    assert_eq!(true, valid_tx.verify());
}

#[test]
pub fn verify_invalid() {
    let executor = DumbExecutor { vm: VM.clone() };
    let invalid_tx = create_transaction_mock(executor, false);
    assert_eq!(false, invalid_tx.verify());
}

#[test]
pub fn execute_valid() {
    let executor = DumbExecutor { vm: VM.clone() };
    let db = MemoryDB::new();
    {
        let snapshot = db.snapshot();
        let entry = create_entry(&*snapshot);
        assert_eq!(None, entry.get());
    }
    {
        let mut fork = db.fork();
        let valid_tx = create_transaction_mock(executor, true);
        assert!(valid_tx.verify());
        let result = valid_tx.execute(&mut fork);
        // TODO here should be `ExecutionResult` in the next Exonum release
        // FIXME here should be `catch_panic` and it's `Result` now
        assert_eq!(result, ());
        db.merge(fork.into_patch()).expect("Failed to merge transaction");
    }
    let snapshot = db.snapshot();
    let entry = create_entry(&*snapshot);
    assert_eq!(Some(String::from(ENTRY_VALUE)), entry.get());
}

#[test]
#[should_panic]
pub fn execute_invalid() {
    let executor = DumbExecutor { vm: VM.clone() };
    let db = MemoryDB::new();
    {
        let snapshot = db.snapshot();
        let entry = create_entry(&*snapshot);
        assert_eq!(None, entry.get());
    }
    {
        let mut fork = db.fork();
        let invalid_tx = create_transaction_mock(executor, false);
        assert!(!invalid_tx.verify());
        let result = invalid_tx.execute(&mut fork);
        // TODO here should be `ExecutionResult` in the next Exonum release
        // FIXME here should be `catch_panic` and it's `Result` now
        assert_eq!(result, ());
        db.merge(fork.into_patch()).expect("Failed to merge transaction");
    }
    let snapshot = db.snapshot();
    let entry = create_entry(&*snapshot);
    assert_eq!(None, entry.get());
}
