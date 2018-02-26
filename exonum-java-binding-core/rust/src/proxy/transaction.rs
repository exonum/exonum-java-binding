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
use utils;

static CLASS_JL_ERROR: &str = "java/lang/Error";

/// A proxy for `Transaction`s
#[derive(Clone)]
pub struct TransactionProxy<E>
where
    E: Executor + 'static,
{
    exec: E,
    adapter: GlobalRef,
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
    /// Creates an instance of `TransactionProxy` with already prepared global ref
    /// to `UserTransactionAdapter`.
    pub unsafe fn from_global_ref(exec: E, adapter: GlobalRef, raw: RawMessage) -> Self {
        TransactionProxy { exec, adapter, raw }
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
        unreachable!()
    }

    fn serialize_field(&self) -> Result<Value, Box<Error + Send + Sync>> {
        use exonum::api::ApiError;
        let res = self.exec.with_attached(|env| {
            let name = env.call_method(self.adapter.as_obj(), "info", "()Ljava/lang/String;", &[])?
                .l()?;
            Ok(String::from(env.get_string(name.into())?))
        });
        let json_string = match res {
            Ok(json_string) => json_string,
            Err(jni_error) => {
                let res = self.exec.with_attached(|env| {
                    if !env.exception_check()? {
                        Err("")?;
                    }
                    let exception = env.exception_occurred()?;
                    if env.is_instance_of(exception.into(), CLASS_JL_ERROR)? {
                        panic!("Fatal exception in Java: {:?}", jni_error);
                    } else {
                        env.exception_clear()?;
                        Ok(ApiError::Serialize(format!("{:?}", jni_error).into()))
                    }
                });
                match res {
                    Ok(tx_error) => Err(tx_error)?,
                    Err(jni_error) => panic!("Unknown error in JNI: {:?}", jni_error),
                }
            }
        };
        Ok(serde_json::from_str(&json_string)?)
    }
}

impl<E> Transaction for TransactionProxy<E>
where
    E: Executor + 'static,
{
    fn verify(&self) -> bool {
        let res = self.exec.with_attached(|env: &JNIEnv| {
            env.call_method(self.adapter.as_obj(), "isValid", "()Z", &[])?
                .z()
        });
        match res {
            Ok(is_valid) => is_valid,
            Err(err) => panic!(err),
        }
    }

    fn execute(&self, fork: &mut Fork) {
        let res = self.exec.with_attached(|env: &JNIEnv| {
            let view_handle = utils::to_handle(View::from_ref_fork(fork));
            env.call_method(
                self.adapter.as_obj(),
                "execute",
                "(J)V",
                &[JValue::from(view_handle)],
            )?;
            Ok(())
        });
        match res {
            Ok(execution_result) => execution_result,
            Err(err) => panic!(err),
        }
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
        unreachable!()
    }

    fn raw(&self) -> &RawMessage {
        &self.raw
    }
}
