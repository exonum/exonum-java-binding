use java_bindings::{Executor, TransactionProxy};
use java_bindings::exonum::messages::{MessageBuffer, RawMessage};
use java_bindings::jni::objects::{AutoLocal, JObject, JValue};

const TRANSACTION_ADAPTER_CLASS: &str =
    "com/exonum/binding/service/adapters/UserTransactionAdapter";
const NATIVE_FACADE_CLASS: &str = "com/exonum/binding/fakes/NativeFacade";

pub const ENTRY_VALUE: &str = "test_value";
pub const INFO_JSON: &str = r#""test_info""#;

/// Creates `TransactionProxy` with a mock transaction and an empty `RawMessage`.
pub fn create_transaction_mock<E: Executor>(executor: E, valid: bool) -> TransactionProxy<E> {
    let (java_tx_mock, raw) = executor.with_attached(|env| {
        let value = env.new_string(ENTRY_VALUE)?;
        let info = env.new_string(INFO_JSON)?;
        let java_tx_mock = env.call_static_method(
            NATIVE_FACADE_CLASS,
            "createTransaction",
            format!("(ZLjava/lang/String;Ljava/lang/String;)L{};", TRANSACTION_ADAPTER_CLASS),
            &[
                JValue::from(valid),
                JValue::from(JObject::from(value)),
                JValue::from(JObject::from(info))
            ],
        )?
            .l()?;
        let java_tx_mock = env.new_global_ref(AutoLocal::new(env, java_tx_mock).as_obj())?;
        let raw = RawMessage::new(MessageBuffer::from_vec(vec![]));
        Ok((java_tx_mock, raw))
    }).unwrap();

    TransactionProxy::from_global_ref(executor, java_tx_mock, raw)
}

/// Creates `TransactionProxy` which throws an exception on any call.
pub fn create_throwing_mock<E: Executor>(executor: E, exception_class: &str) -> TransactionProxy<E> {
    let (java_tx_mock, raw) = executor.with_attached(|env| {
        let exception = env.find_class(exception_class)?;
        let java_tx_mock = env.call_static_method(
            NATIVE_FACADE_CLASS,
            "createThrowingTransaction",
            format!("(Ljava/lang/Class;)L{};", TRANSACTION_ADAPTER_CLASS),
            &[JValue::from(JObject::from(exception.into_inner()))],
        )?
            .l()?;
        let java_tx_mock = env.new_global_ref(AutoLocal::new(env, java_tx_mock).as_obj())?;
        let raw = RawMessage::new(MessageBuffer::from_vec(vec![]));
        Ok((java_tx_mock, raw))
    }).unwrap();

    TransactionProxy::from_global_ref(executor, java_tx_mock, raw)
}
