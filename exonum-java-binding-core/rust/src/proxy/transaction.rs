use exonum::blockchain::Transaction;
use exonum::encoding::Offset;
use exonum::encoding::serialize::json::ExonumJson;
use exonum::encoding::serialize::WriteBufferWrapper;
use exonum::storage::Fork;
use exonum::messages::{Message, RawMessage};
use jni::*;
use jni::errors::Result;
use jni::objects::GlobalRef;
use jni::objects::JValue;
use serde_json;
use serde_json::value::Value;

use std::fmt;

use Executor;
use storage::View;
use utils;

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

// FIXME docs
// `TransactionProxy` is immutable, so it can be safely used in different threads.
unsafe impl <E> Sync for TransactionProxy<E>
where
    E: Executor + 'static,
{}

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
    fn deserialize_field<B>(_value: &Value, _buffer: &mut B, _from: Offset, _to: Offset)
        -> ::std::result::Result<(), Box<::std::error::Error>>
    where
        Self: Sized,
        B: WriteBufferWrapper,
    {
        unimplemented!()
    }

    fn serialize_field(&self)
        -> ::std::result::Result<Value, Box<::std::error::Error + Send + Sync>>
    {
        use exonum::api::ApiError;
        let json_string = self.exec.with_attached(|env| {
            let name = env.call_method(
                self.adapter.as_obj(),
                "info",
                "()Ljava/lang/String;",
                &[],
            )?
                .l()?;
            Ok(String::from(env.get_string(name.into())?))
        })
            // FIXME here should be a panic
            .map_err(|e| ApiError::Serialize(format!("{:?}", e).into()))?;
        Ok(serde_json::from_str(&json_string)?)
    }
}

impl<E> Transaction for TransactionProxy<E>
where
    E: Executor + 'static,
{
    fn verify(&self) -> bool {
        let res = self.exec.with_attached(|env: &JNIEnv| {
            env.call_method(
                self.adapter.as_obj(),
                "isValid",
                "()Z",
                &[],
            )?
                .z()
        });
        // FIXME maybe here should be a panic?
        res.unwrap_or(false)
    }

    fn execute(&self, fork: &mut Fork) {
        let res = (|| -> Result<()> {
            self.exec.with_attached(|env: &JNIEnv| {
                let view_handle = utils::to_handle(View::from_ref_fork(fork));
                env.call_method(
                    self.adapter.as_obj(),
                    "execute",
                    "(J)V",
                    &[JValue::from(view_handle)],
                )?;
                Ok(())
            })
        })();
        // FIXME here should be a nice panic
        res.unwrap()
    }
}

impl<E> Message for TransactionProxy<E>
where
    E: Executor,
{
    fn from_raw(_raw: RawMessage) -> ::std::result::Result<Self, ::exonum::encoding::Error> where
        Self: Sized {
        unreachable!()
    }

    fn raw(&self) -> &RawMessage {
        &self.raw
    }
}
