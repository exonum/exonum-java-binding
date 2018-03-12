use exonum::blockchain::Transaction;
use exonum::encoding::Offset;
use exonum::encoding::serialize::json::ExonumJson;
use exonum::encoding::serialize::WriteBufferWrapper;
use exonum::storage::Fork;
use exonum::messages::{Message, RawMessage};
use jni::JNIEnv;
use jni::objects::{GlobalRef, JValue};
use serde_json;
use serde_json::value::Value;

use std::error::Error;
use std::fmt;

use Executor;
use storage::View;
use utils::{check_error_on_exception, panic_on_exception, to_handle, unwrap_jni};

/// A proxy for `Transaction`s.
#[derive(Clone)]
pub struct TransactionProxy<E>
where
    E: Executor + 'static,
{
    exec: E,
    transaction: GlobalRef,
    raw: RawMessage,
}

// `TransactionProxy` is immutable, so it can be safely used in different threads.
unsafe impl<E> Sync for TransactionProxy<E>
where
    E: Executor + 'static,
{
}

impl<E> fmt::Debug for TransactionProxy<E>
where
    E: Executor + 'static,
{
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "TransactionProxy")
    }
}

impl<E> TransactionProxy<E>
where
    E: Executor + 'static,
{
    /// Creates a `TransactionProxy` of the given Java transaction.
    pub fn from_global_ref(exec: E, transaction: GlobalRef, raw: RawMessage) -> Self {
        TransactionProxy {
            exec,
            transaction,
            raw,
        }
    }
}

impl<E> ExonumJson for TransactionProxy<E>
where
    E: Executor + 'static,
{
    fn deserialize_field<B>(
        _value: &Value,
        _buffer: &mut B,
        _from: Offset,
        _to: Offset,
    ) -> Result<(), Box<Error>>
    where
        Self: Sized,
        B: WriteBufferWrapper,
    {
        unimplemented!("Is not used in Java bindings")
    }

    fn serialize_field(&self) -> Result<Value, Box<Error + Send + Sync>> {
        let res: Result<String, String> = unwrap_jni(self.exec.with_attached(|env| {
            let res = env.call_method(
                self.transaction.as_obj(),
                "info",
                "()Ljava/lang/String;",
                &[],
            );
            Ok(check_error_on_exception(env, res).map(|json_string| {
                let obj = unwrap_jni(json_string.l()).into();
                String::from(unwrap_jni(env.get_string(obj)))
            }))
        }));
        Ok(serde_json::from_str(&res?)?)
    }
}

impl<E> Transaction for TransactionProxy<E>
where
    E: Executor + 'static,
{
    fn verify(&self) -> bool {
        let res = self.exec.with_attached(|env: &JNIEnv| {
            let res = env.call_method(self.transaction.as_obj(), "isValid", "()Z", &[]);
            panic_on_exception(env, res).z()
        });
        unwrap_jni(res)
    }

    fn execute(&self, fork: &mut Fork) {
        let res = self.exec.with_attached(|env: &JNIEnv| {
            let view_handle = to_handle(View::from_ref_fork(fork));
            let res = env.call_method(
                self.transaction.as_obj(),
                "execute",
                "(J)V",
                &[JValue::from(view_handle)],
            );
            panic_on_exception(env, res).v()
        });
        unwrap_jni(res)
    }
}

impl<E> Message for TransactionProxy<E>
where
    E: Executor,
{
    fn from_raw(_raw: RawMessage) -> Result<Self, ::exonum::encoding::Error>
    where
        Self: Sized,
    {
        unimplemented!("Is not used in Java bindings")
    }

    fn raw(&self) -> &RawMessage {
        &self.raw
    }
}
