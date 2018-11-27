use exonum::blockchain::{ExecutionError, ExecutionResult, Transaction, TransactionContext};
use exonum::encoding::serialize::json::ExonumJson;
use exonum::encoding::serialize::WriteBufferWrapper;
use exonum::encoding::Offset;
use exonum::messages::BinaryForm;
use exonum::messages::RawTransaction;
use jni::objects::{GlobalRef, JObject, JValue};
use jni::JNIEnv;
use serde;
use serde_json;
use serde_json::value::Value;

use std::error::Error;
use std::fmt;

use storage::View;
use utils::{
    check_error_on_exception, convert_to_string, describe_java_exception,
    get_and_clear_java_exception, get_exception_message, panic_on_exception, to_handle, unwrap_jni,
};
use {JniErrorKind, JniExecutor, JniResult, MainExecutor};

const CLASS_TRANSACTION_EXCEPTION: &str =
    "com/exonum/binding/transaction/TransactionExecutionException";

/// A proxy for `Transaction`s.
#[derive(Clone)]
pub struct TransactionProxy {
    exec: MainExecutor,
    transaction: GlobalRef,
    raw: RawTransaction,
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
    pub fn from_global_ref(
        exec: MainExecutor,
        transaction: GlobalRef,
        raw: RawTransaction,
    ) -> Self {
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

impl serde::Serialize for TransactionProxy {
    fn serialize<S>(
        &self,
        serializer: S,
    ) -> Result<<S as serde::Serializer>::Ok, <S as serde::Serializer>::Error>
    where
        S: serde::Serializer,
    {
        // FIXME: smth with error handling
        serializer.serialize_bytes(&self.raw.encode().unwrap())
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

    fn execute<'a>(&self, mut context: TransactionContext<'a>) -> ExecutionResult {
        let res = self.exec.with_attached(|env: &JNIEnv| {
            let view_handle = to_handle(View::from_ref_fork(context.fork()));
            let res = env
                .call_method(
                    self.transaction.as_obj(),
                    "execute",
                    "(J)V",
                    &[JValue::from(view_handle)],
                ).and_then(JValue::v);
            Ok(check_transaction_execution_result(env, res))
        });
        unwrap_jni(res)
    }
}

/// Handles exceptions after executing transactions
///
/// The TransactionExecutionException and its descendants are converted into `Error`s with their
/// descriptions. The rest (Java and JNI errors) are treated as unrecoverable and result in a panic.
///
/// Panics:
/// - Panics if there is some JNI error.
/// - If there is a pending Java throwable that IS NOT an instance of the
/// `TransactionExecutionException`.
fn check_transaction_execution_result<T>(
    env: &JNIEnv,
    result: JniResult<T>,
) -> Result<T, ExecutionError> {
    result.map_err(|jni_error| match jni_error.0 {
        JniErrorKind::JavaException => {
            let exception = get_and_clear_java_exception(env);
            let message = unwrap_jni(get_exception_message(env, exception));
            if !unwrap_jni(env.is_instance_of(exception, CLASS_TRANSACTION_EXCEPTION)) {
                let panic_msg = describe_java_exception(env, exception);
                panic!(panic_msg);
            }

            let err_code = unwrap_jni(get_tx_error_code(env, exception)) as u8;
            match message {
                Some(msg) => ExecutionError::with_description(err_code, msg),
                None => ExecutionError::new(err_code),
            }
        }
        _ => unwrap_jni(Err(jni_error)),
    })
}

/// Returns the error code of the `TransactionExecutionException` instance.
fn get_tx_error_code(env: &JNIEnv, exception: JObject) -> JniResult<i8> {
    let err_code = env.call_method(exception, "getErrorCode", "()B", &[])?;
    err_code.b()
}
