extern crate java_bindings;
#[macro_use]
extern crate lazy_static;

mod util;

use std::panic;

use java_bindings::{DumbExecutor, Executor, TransactionProxy};
use java_bindings::exonum::blockchain::Transaction;
use java_bindings::exonum::messages::{MessageBuffer, RawMessage};
use java_bindings::exonum::storage::{Database, Entry, MemoryDB};
use java_bindings::jni::{JavaVM, JNIEnv};
use java_bindings::jni::descriptors::Desc;
use java_bindings::jni::errors::{Error as JNIError, Result as JNIResult};
use java_bindings::jni::errors::ErrorKind;
use java_bindings::jni::objects::{AutoLocal, JClass, JObject, JValue};

use std::sync::Arc;

use util::create_vm;

static ENTRY_NAME: &str = "test_entry";
static ENTRY_VALUE: &str = "test_value";

static TRANSACTION_ADAPTER_CLASS: &str =
    "com/exonum/binding/service/adapters/UserTransactionAdapter";
static NATIVE_FACADE_CLASS: &str = "com/exonum/binding/fakes/NativeFacade";


lazy_static! {
    pub static ref VM: Arc<JavaVM> = Arc::new(create_vm(true));
}

fn create_transaction_mock<E: Executor>(executor: E, valid: bool) -> TransactionProxy<E> {
    let (adapter, raw) = executor.with_attached(|env| {
        let value = env.new_string(ENTRY_VALUE)?;
        let info = env.new_string("")?;
        let adapter = env.call_static_method(
            NATIVE_FACADE_CLASS,
            "createTransaction",
            format!("(ZLjava/lang/String;Ljava/lang/String;)L{};", TRANSACTION_ADAPTER_CLASS),
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

fn create_transaction_panic_mock<E: Executor>(executor: E, exception_class: &str)
    -> TransactionProxy<E>
{
    let (adapter, raw) = executor.with_attached(|env| {
        let exception = env.find_class(exception_class)?;
//        let exception = env.new_object(exception_class, "()V", &[])?;
        let adapter = env.call_static_method(
            NATIVE_FACADE_CLASS,
            "createThrowingTransaction",
            format!("(Ljava/lang/Class;)L{};", TRANSACTION_ADAPTER_CLASS),
            &[JValue::from(JObject::from(exception.into_inner()))],
        )?;
        let adapter = env.new_global_ref(AutoLocal::new(env, adapter.l()?).as_obj())?;
        let raw = RawMessage::new(MessageBuffer::from_vec(vec![]));
        Ok((adapter, raw))
    }).unwrap();

    unsafe { TransactionProxy::from_global_ref(executor, adapter, raw) }
}

fn create_entry<V>(view: V) -> Entry<V, String> {
    Entry::new(ENTRY_NAME, view)
}

use std::any::Any;
use std::error::Error;

pub fn any_to_string(any: &Any) -> String {
    if let Some(s) = any.downcast_ref::<&str>() {
        s.to_string()
    } else if let Some(s) = any.downcast_ref::<String>() {
        s.clone()
    } else if let Some(error) = any.downcast_ref::<Box<Error + Send>>() {
        error.description().to_string()
    } else {
        "Unknown error occurred".to_string()
    }
}

#[test]
pub fn catch_panic_example() {
    let catch_result = panic::catch_unwind(panic::AssertUnwindSafe(
        || panic!("Civil Disorder in Las Vegas! Mayor flees in panic.")));

    match catch_result {
        Ok(()) => unreachable!(),
        Err(err) => println!("Catched the panic: {:?}", any_to_string(&err)),
    }
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
pub fn verify_panic() {
    let executor = DumbExecutor { vm: VM.clone() };
    let exception_class = "java/lang/Exception";
    let panic_tx = create_transaction_panic_mock(executor.clone(), exception_class);

    let any_err = panic::catch_unwind(panic::AssertUnwindSafe(|| panic_tx.verify()))
        .expect_err("This transaction should panic, but it doesn't!");
    executor.with_attached(|env| Ok(check_exception(env, &*any_err, exception_class)));
}

pub fn check_exception<'c, C>(env: &'c JNIEnv<'c>, any_err: &Any, class: C)
where
    C: Desc<'c, JClass<'c>>,
{
    let err = any_err.downcast_ref::<JNIError>()
        .unwrap_or_else(||
            panic!("This error should be JNIError, found: {:?}", any_to_string(any_err)));
    match err.0 {
        ErrorKind::JavaException => {}
        _ => panic!("Expected JavaException, found: {:?}", err.0),
    }
    let exception = env.exception_occurred().expect("Exception not found");
    assert!(env.is_instance_of(exception.into(), class).unwrap());
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

#[test]
pub fn execute_panic() {
    let executor = DumbExecutor { vm: VM.clone() };
    let exception_class = "java/lang/Exception";
    let panic_tx = create_transaction_panic_mock(executor.clone(), exception_class);
    let db = MemoryDB::new();
    let mut fork = db.fork();

    let any_err = panic::catch_unwind(panic::AssertUnwindSafe(|| panic_tx.execute(&mut fork)))
        .expect_err("This transaction should panic, but it doesn't!");
    executor.with_attached(|env| Ok(check_exception(env, &*any_err, exception_class)));
}

