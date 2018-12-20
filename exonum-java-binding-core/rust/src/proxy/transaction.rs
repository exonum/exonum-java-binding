use exonum::blockchain::{ExecutionError, ExecutionResult, Transaction, TransactionContext};
use exonum::messages::BinaryForm;
use exonum::messages::RawTransaction;
use jni::objects::{GlobalRef, JObject, JValue};
use jni::signature::{JavaType, Primitive};
use jni::JNIEnv;
use serde;

use std::fmt;

use storage::View;
use utils::{
    jni_cache::{transaction_adapter::execute_id, classes_refs::transaction_execution_exception},
    describe_java_exception, get_and_clear_java_exception, get_exception_message, to_handle,
    unwrap_jni,
};
use {JniErrorKind, JniExecutor, JniResult, MainExecutor};

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

impl serde::Serialize for TransactionProxy {
    fn serialize<S>(
        &self,
        serializer: S,
    ) -> Result<<S as serde::Serializer>::Ok, <S as serde::Serializer>::Error>
    where
        S: serde::Serializer,
    {
        serializer.serialize_bytes(
            &self
                .raw
                .encode()
                .expect("Could not serialize TransactionProxy"),
        )
    }
}

impl Transaction for TransactionProxy {
    fn execute(&self, mut context: TransactionContext) -> ExecutionResult {
        let res = self.exec.with_attached(|env: &JNIEnv| {
            let tx_hash = context.tx_hash();
            let author_pk = context.author();

            let view_handle = to_handle(View::from_ref_fork(context.fork()));
            let tx_hash = JObject::from(env.byte_array_from_slice(tx_hash.as_ref())?);
            let author_pk = JObject::from(env.byte_array_from_slice(author_pk.as_ref())?);

            let res = unsafe {
                env.call_method_unsafe(
                    self.transaction.as_obj(),
                    execute_id(),
                    JavaType::Primitive(Primitive::Void),
                    &[
                        JValue::from(view_handle),
                        JValue::from(tx_hash),
                        JValue::from(author_pk),
                    ]
                ).and_then(JValue::v)
            };

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
            if !unwrap_jni(env.is_instance_of(exception, &transaction_execution_exception())) {
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
