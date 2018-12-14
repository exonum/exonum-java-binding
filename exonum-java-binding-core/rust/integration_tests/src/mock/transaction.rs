use java_bindings::exonum::messages::{RawTransaction, ServiceTransaction};
use java_bindings::jni::objects::{GlobalRef, JObject, JValue};
use java_bindings::serde_json::Value;
use java_bindings::{JniExecutor, MainExecutor, TransactionProxy};

use super::NATIVE_FACADE_CLASS;

pub const TRANSACTION_ADAPTER_CLASS: &str =
    "com/exonum/binding/service/adapters/UserTransactionAdapter";

pub const TEST_ENTRY_NAME: &str = "test_entry";
pub const TX_HASH_ENTRY_NAME: &str = "tx_hash";
pub const AUTHOR_PK_ENTRY_NAME: &str = "author_pk";
pub const ENTRY_VALUE: &str = "test_value";

lazy_static! {
    pub static ref INFO_VALUE: Value = Value::String("test_info".to_string());
}

/// Creates `TransactionProxy` which throws an exception on any call.
pub fn create_throwing_mock_transaction_proxy(
    executor: MainExecutor,
    exception_class: &str,
) -> (TransactionProxy, RawTransaction) {
    let (java_tx_mock, raw) = executor
        .with_attached(|env| {
            let exception = env.find_class(exception_class)?;
            let java_tx_mock = env
                .call_static_method(
                    NATIVE_FACADE_CLASS,
                    "createThrowingTransaction",
                    format!("(Ljava/lang/Class;)L{};", TRANSACTION_ADAPTER_CLASS),
                    &[JValue::from(JObject::from(exception.into_inner()))],
                )?
                .l()?;
            let java_tx_mock = env.new_global_ref(java_tx_mock)?;
            let raw = create_empty_raw_transaction();
            Ok((java_tx_mock, raw))
        })
        .unwrap();

    let tx_proxy = TransactionProxy::from_global_ref(executor, java_tx_mock, raw.clone());
    (tx_proxy, raw)
}

/// Creates `TransactionProxy` which throws TransactionExecutionException on the `execute` call.
pub fn create_throwing_exec_exception_mock_transaction_proxy(
    executor: MainExecutor,
    is_subclass: bool,
    error_code: i8,
    error_message: Option<&str>,
) -> (TransactionProxy, RawTransaction) {
    let (java_tx_mock, raw) = executor
        .with_attached(|env| {
            let msg = match error_message {
                Some(err_msg) => {
                    let msg = env.new_string(err_msg)?;
                    JObject::from(msg)
                }
                None => JObject::null(),
            };
            let java_tx_mock = env
                .call_static_method(
                    NATIVE_FACADE_CLASS,
                    "createThrowingExecutionExceptionTransaction",
                    format!("(ZBLjava/lang/String;)L{};", TRANSACTION_ADAPTER_CLASS),
                    &[
                        JValue::from(is_subclass),
                        JValue::from(error_code),
                        JValue::from(msg),
                    ],
                )?
                .l()?;
            let java_tx_mock = env.new_global_ref(java_tx_mock)?;
            let raw = create_empty_raw_transaction();
            Ok((java_tx_mock, raw))
        })
        .unwrap();

    let tx_proxy = TransactionProxy::from_global_ref(executor, java_tx_mock, raw.clone());
    (tx_proxy, raw)
}

/// Creates `TransactionProxy` with a mock transaction and an empty `RawMessage`.
pub fn create_mock_transaction_proxy(executor: MainExecutor) -> (TransactionProxy, RawTransaction) {
    let (java_tx_mock, raw) = create_mock_transaction(&executor);
    let tx_proxy = TransactionProxy::from_global_ref(executor, java_tx_mock, raw.clone());
    (tx_proxy, raw)
}

/// Creates a mock transaction and an empty `RawMessage`.
pub fn create_mock_transaction(executor: &MainExecutor) -> (GlobalRef, RawTransaction) {
    executor
        .with_attached(|env| {
            let value = env.new_string(ENTRY_VALUE)?;
            let java_tx_mock = env
                .call_static_method(
                    NATIVE_FACADE_CLASS,
                    "createTransaction",
                    format!("(Ljava/lang/String;)L{};", TRANSACTION_ADAPTER_CLASS),
                    &[JValue::from(JObject::from(value))],
                )?
                .l()?;
            let java_tx_mock = env.new_global_ref(java_tx_mock)?;
            let raw = create_empty_raw_transaction();
            Ok((java_tx_mock, raw))
        })
        .unwrap()
}

pub fn create_empty_raw_transaction() -> RawTransaction {
    let transaction_id = 0;
    let service_id = 0;
    let payload = Vec::new();
    let service_transaction = ServiceTransaction::from_raw_unchecked(transaction_id, payload);
    RawTransaction::new(service_id, service_transaction)
}
