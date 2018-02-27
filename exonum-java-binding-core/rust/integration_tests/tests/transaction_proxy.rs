extern crate java_bindings;
#[macro_use]
extern crate lazy_static;

mod util;

use std::any::Any;
use std::error::Error;
use std::panic;


use java_bindings::{DumbExecutor, Executor, TransactionProxy};
use java_bindings::exonum::blockchain::Transaction;
use java_bindings::exonum::encoding::serialize::json::ExonumJson;
use java_bindings::exonum::messages::{MessageBuffer, RawMessage};
use java_bindings::exonum::storage::{Database, Entry, MemoryDB};
use java_bindings::jni::{JavaVM, JNIEnv};
use java_bindings::jni::descriptors::Desc;
use java_bindings::jni::errors::{Error as JNIError};
use java_bindings::jni::errors::ErrorKind;
use java_bindings::jni::objects::{AutoLocal, JClass, JObject, JValue};
use java_bindings::serde_json::Value;

use std::sync::Arc;

use util::create_vm;

static ENTRY_NAME: &str = "test_entry";
static ENTRY_VALUE: &str = "test_value";
static INFO_JSON: &str = r#""test_info""#;
static INFO_VALUE: &str = r"test_info";

static TRANSACTION_ADAPTER_CLASS: &str =
    "com/exonum/binding/service/adapters/UserTransactionAdapter";
static NATIVE_FACADE_CLASS: &str = "com/exonum/binding/fakes/NativeFacade";

static OUT_OF_MEMORY_ERROR_CLASS: &str = "java/lang/OutOfMemoryError";
static EXCEPTION_CLASS: &str = "java/lang/Exception";
static ERROR_CLASS: &str = "java/lang/Error";
static THROWABLE_CLASS: &str = "java/lang/Throwable";
static ARITHMETIC_EXCEPTION_CLASS: &str = "java/lang/ArithmeticException";


lazy_static! {
    pub static ref VM: Arc<JavaVM> = Arc::new(create_vm(true));
}

#[test]
pub fn verify_should_work_with_valid_transaction() {
    let executor = DumbExecutor { vm: VM.clone() };
    let valid_tx = create_transaction_mock(executor, true);
    assert_eq!(true, valid_tx.verify());
}

#[test]
pub fn verify_should_work_with_invalid_transaction() {
    let executor = DumbExecutor { vm: VM.clone() };
    let invalid_tx = create_transaction_mock(executor, false);
    assert_eq!(false, invalid_tx.verify());
}

#[test]
pub fn verify_should_panic_with_any_exception() {
    let exception_class = ARITHMETIC_EXCEPTION_CLASS;

    let executor = DumbExecutor { vm: VM.clone() };
    let panic_tx = create_transaction_panic_mock(executor.clone(), exception_class);

    let any_err = panic::catch_unwind(panic::AssertUnwindSafe(|| panic_tx.verify()))
        .expect_err("This transaction should panic, but it doesn't!");
    executor.with_attached(|env| Ok(check_exception(env, &*any_err, exception_class)))
        .unwrap();
}

#[test]
pub fn execute_should_work_with_valid_transaction() {
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
        assert_eq!(result, ());
        db.merge(fork.into_patch()).expect("Failed to merge transaction");
    }
    let snapshot = db.snapshot();
    let entry = create_entry(&*snapshot);
    assert_eq!(Some(String::from(ENTRY_VALUE)), entry.get());
}

#[test]
// TODO this behaviour should change to "work" in the next release of Exonum.
pub fn execute_should_panic_with_invalid_transaction() {
    let interception_class = THROWABLE_CLASS;

    let executor = DumbExecutor { vm: VM.clone() };
    let db = MemoryDB::new();
    {
        let snapshot = db.snapshot();
        let entry = create_entry(&*snapshot);
        assert_eq!(None, entry.get());
    }
    {
        let mut fork = db.fork();
        let invalid_tx = create_transaction_mock(executor.clone(), false);
        assert!(!invalid_tx.verify());
        // TODO here should be `ExecutionResult` in the next Exonum release
        let any_err = panic::catch_unwind(panic::AssertUnwindSafe(|| invalid_tx.execute(&mut fork)))
            .expect_err("This transaction should panic, but it doesn't!");
        executor.with_attached(|env| Ok(check_exception(env, &*any_err, interception_class)))
            .unwrap();

        db.merge(fork.into_patch()).expect("Failed to merge transaction");
    }
    let snapshot = db.snapshot();
    let entry = create_entry(&*snapshot);
    assert_eq!(None, entry.get());
}

#[test]
pub fn execute_should_panic_with_fatal_exception() {
    let exception_class = ERROR_CLASS;
    let interception_class = ERROR_CLASS;

    let executor = DumbExecutor { vm: VM.clone() };
    let panic_tx = create_transaction_panic_mock(executor.clone(), exception_class);
    let db = MemoryDB::new();
    let mut fork = db.fork();

    let any_err = panic::catch_unwind(panic::AssertUnwindSafe(|| panic_tx.execute(&mut fork)))
        .expect_err("This transaction should panic, but it doesn't!");
    executor.with_attached(|env| Ok(check_exception(env, &*any_err, interception_class)))
        .unwrap();
}

#[test]
pub fn execute_should_panic_with_fatal_exception_heir() {
    let exception_class = OUT_OF_MEMORY_ERROR_CLASS;
    let interception_class = ERROR_CLASS;

    let executor = DumbExecutor { vm: VM.clone() };
    let panic_tx = create_transaction_panic_mock(executor.clone(), exception_class);
    let db = MemoryDB::new();
    let mut fork = db.fork();

    let any_err = panic::catch_unwind(panic::AssertUnwindSafe(|| panic_tx.execute(&mut fork)))
        .expect_err("This transaction should panic, but it doesn't!");
    executor.with_attached(|env| Ok(check_exception(env, &*any_err, interception_class)))
        .unwrap();
}

#[test]
// TODO this behaviour should change to "work" in the next release of Exonum.
pub fn execute_should_panic_with_nonfatal_exception() {
    let exception_class = ARITHMETIC_EXCEPTION_CLASS;
    let interception_class = ERROR_CLASS;

    let executor = DumbExecutor { vm: VM.clone() };
    let panic_tx = create_transaction_panic_mock(executor.clone(), exception_class);
    let db = MemoryDB::new();
    let mut fork = db.fork();

    let any_err = panic::catch_unwind(panic::AssertUnwindSafe(|| panic_tx.execute(&mut fork)))
        .expect_err("This transaction should panic, but it doesn't!");
    executor.with_attached(|env| Ok(check_exception(env, &*any_err, interception_class)))
        .unwrap();
}

#[test]
pub fn json_serialize_should_work() {
    let executor = DumbExecutor { vm: VM.clone() };
    let valid_tx = create_transaction_mock(executor, true);
    assert_eq!(valid_tx.serialize_field().unwrap(), Value::String(INFO_VALUE.into()));
}

#[test]
#[ignore]
pub fn json_serialize_should_panic_from_fatal_exception() {
    let exception_class = ERROR_CLASS;
    let interception_class = ERROR_CLASS;

    let executor = DumbExecutor { vm: VM.clone() };
    let panic_tx = create_transaction_panic_mock(executor.clone(), exception_class);
    let any_err = panic::catch_unwind(panic::AssertUnwindSafe(|| panic_tx.serialize_field()))
        .expect_err("This transaction should panic, but it doesn't!");
    executor.with_attached(|env| Ok(check_exception(env, &*any_err, interception_class)))
        .unwrap();
}

#[test]
#[ignore]
pub fn json_serialize_should_panic_from_fatal_exception_heir() {
    let exception_class = OUT_OF_MEMORY_ERROR_CLASS;
    let interception_class = ERROR_CLASS;

    let executor = DumbExecutor { vm: VM.clone() };
    let panic_tx = create_transaction_panic_mock(executor.clone(), exception_class);

    let any_err = panic::catch_unwind(panic::AssertUnwindSafe(|| panic_tx.serialize_field()))
        .expect_err("This transaction should panic, but it doesn't!");
    executor.with_attached(|env| Ok(check_exception(env, &*any_err, interception_class)))
        .unwrap();
}

#[test]
#[ignore]
pub fn json_serialize_should_not_panic_from_nonfatal_exception() {
    let exception_class = ARITHMETIC_EXCEPTION_CLASS;

    let executor = DumbExecutor { vm: VM.clone() };
    let panic_tx = create_transaction_panic_mock(executor.clone(), exception_class);

    let err = panic::catch_unwind(panic::AssertUnwindSafe(|| panic_tx.serialize_field()))
        .expect("This transaction should not panic, but it does!")
        .expect_err("This transaction should be serialized with error!");
    // FIXME string representation of an error
    assert_eq!(err.description(), "");
}

pub fn check_exception<'e, C>(env: &'e JNIEnv<'e>, any_err: &Any, class: C)
    where
        C: Desc<'e, JClass<'e>>,
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

fn create_transaction_mock<E: Executor>(executor: E, valid: bool) -> TransactionProxy<E> {
    let (adapter, raw) = executor.with_attached(|env| {
        let value = env.new_string(ENTRY_VALUE)?;
        let info = env.new_string(INFO_JSON)?;
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
        // TODO remove this stub and get a real byte buffer
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
