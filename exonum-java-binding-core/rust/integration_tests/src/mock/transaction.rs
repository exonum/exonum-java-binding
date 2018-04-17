use java_bindings::{JniExecutor, MainExecutor, TransactionProxy};
use java_bindings::exonum::messages::{MessageBuffer, RawMessage};
use java_bindings::jni::objects::{AutoLocal, GlobalRef, JObject, JValue};
use java_bindings::serde_json::Value;

use super::NATIVE_FACADE_CLASS;

pub const TRANSACTION_ADAPTER_CLASS: &str = "com/exonum/binding/service/adapters/UserTransactionAdapter";

pub const ENTRY_NAME: &str = "test_entry";
pub const ENTRY_VALUE: &str = "test_value";
pub const INFO_JSON: &str = r#""test_info""#;

lazy_static! {
    pub static ref INFO_VALUE: Value = Value::String("test_info".to_string());
}

/// Creates `TransactionProxy` which throws an exception on any call.
pub fn create_throwing_mock_transaction_proxy(
    executor: MainExecutor,
    exception_class: &str,
) -> TransactionProxy {
    let (java_tx_mock, raw) = executor
        .with_attached(|env| {
            let exception = env.find_class(exception_class)?;
            let java_tx_mock = env.call_static_method(
                NATIVE_FACADE_CLASS,
                "createThrowingTransaction",
                format!("(Ljava/lang/Class;)L{};", TRANSACTION_ADAPTER_CLASS),
                &[JValue::from(JObject::from(exception.into_inner()))],
            )?
                .l()?;
            let java_tx_mock = env.new_global_ref(
                AutoLocal::new(env, java_tx_mock).as_obj(),
            )?;
            let raw = RawMessage::new(MessageBuffer::from_vec(vec![]));
            Ok((java_tx_mock, raw))
        })
        .unwrap();

    TransactionProxy::from_global_ref(executor, java_tx_mock, raw)
}

/// Creates `TransactionProxy` with a mock transaction and an empty `RawMessage`.
pub fn create_mock_transaction_proxy(executor: MainExecutor, valid: bool) -> TransactionProxy {
    let (java_tx_mock, raw) = create_mock_transaction(executor.clone(), valid);
    TransactionProxy::from_global_ref(executor, java_tx_mock, raw)
}

/// Creates a mock transaction and an empty `RawMessage`.
pub fn create_mock_transaction(executor: MainExecutor, valid: bool) -> (GlobalRef, RawMessage) {
    executor
        .with_attached(|env| {
            let value = env.new_string(ENTRY_VALUE)?;
            let info = env.new_string(INFO_JSON)?;
            let java_tx_mock = env.call_static_method(
                NATIVE_FACADE_CLASS,
                "createTransaction",
                format!(
                    "(ZLjava/lang/String;Ljava/lang/String;)L{};",
                    TRANSACTION_ADAPTER_CLASS
                ),
                &[
                    JValue::from(valid),
                    JValue::from(JObject::from(value)),
                    JValue::from(JObject::from(info)),
                ],
            )?
                .l()?;
            let java_tx_mock = env.new_global_ref(
                AutoLocal::new(env, java_tx_mock).as_obj(),
            )?;
            let raw = RawMessage::new(MessageBuffer::from_vec(vec![]));
            Ok((java_tx_mock, raw))
        })
        .unwrap()
}
