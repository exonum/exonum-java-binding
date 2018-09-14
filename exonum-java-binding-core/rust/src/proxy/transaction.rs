use exonum::blockchain::{ExecutionError, ExecutionResult, Transaction};
use exonum::encoding::serialize::json::ExonumJson;
use exonum::encoding::serialize::WriteBufferWrapper;
use exonum::encoding::Offset;
use exonum::messages::{Message, RawMessage};
use exonum::storage::Fork;
use jni::objects::{GlobalRef, JValue};
use jni::JNIEnv;
use serde_json;
use serde_json::value::Value;

use std::error::Error;
use std::fmt;

use storage::View;
use utils::{
    check_error_on_exception, convert_to_string, panic_on_exception, to_handle, unwrap_jni,
};
use {JniExecutor, MainExecutor};

/// A proxy for `Transaction`s.
#[derive(Clone)]
pub struct TransactionProxy {
    exec: MainExecutor,
    transaction: GlobalRef,
    raw: RawMessage,
}

// `TransactionProxy` is immutable, so it can be safely used in different threads.
unsafe impl Sync for TransactionProxy {}

impl fmt::Debug for TransactionProxy {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "TransactionProxy")
    }
}

impl TransactionProxy {
    /// Creates a `TransactionProxy` of the given Java transaction.
    pub fn from_global_ref(exec: MainExecutor, transaction: GlobalRef, raw: RawMessage) -> Self {
        TransactionProxy {
            exec,
            transaction,
            raw,
        }
    }
}

impl ExonumJson for TransactionProxy {
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
                let obj = unwrap_jni(json_string.l());
                unwrap_jni(convert_to_string(env, obj))
            }))
        }));
        Ok(serde_json::from_str(&res?)?)
    }
}

impl Transaction for TransactionProxy {
    fn verify(&self) -> bool {
        let res = self.exec.with_attached(|env: &JNIEnv| {
            let res = env.call_method(self.transaction.as_obj(), "isValid", "()Z", &[]);
            panic_on_exception(env, res).z()
        });
        unwrap_jni(res)
    }

    fn execute(&self, fork: &mut Fork) -> ExecutionResult {
        // A required code for a TransactionProxy#execute error result.
        // This code has no special meaning.
        const ERROR_CODE: u8 = 0;

        let res = self.exec.with_attached(|env: &JNIEnv| {
            let view_handle = to_handle(View::from_ref_fork(fork));
            let res = env
                .call_method(
                    self.transaction.as_obj(),
                    "execute",
                    "(J)V",
                    &[JValue::from(view_handle)],
                ).and_then(JValue::v);
            Ok(check_error_on_exception(env, res))
        });
        unwrap_jni(res).map_err(|err: String| ExecutionError::with_description(ERROR_CODE, err))
    }
}

impl Message for TransactionProxy {
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
