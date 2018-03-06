use exonum::blockchain::Transaction;
use exonum::encoding::Offset;
use exonum::encoding::serialize::json::ExonumJson;
use exonum::encoding::serialize::WriteBufferWrapper;
use exonum::storage::Fork;
use exonum::messages::{Message, RawMessage};
use jni::JNIEnv;
use jni::errors::ErrorKind;
use jni::objects::{GlobalRef, JValue, JObject};
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
    pub unsafe fn from_global_ref(exec: E, adapter: GlobalRef, raw: RawMessage) -> Self {
        TransactionProxy { exec, transaction: adapter, raw }
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
        unimplemented!("Does not used in Java bindings")
    }

    fn serialize_field(&self) -> Result<Value, Box<Error + Send + Sync>> {
        enum Res {
            Json(String),
            Error(String),
        }
        let res = self.exec.with_attached(|env| {
            match env.call_method(self.transaction.as_obj(), "info", "()Ljava/lang/String;", &[]) {
                Ok(json_string) => {
                    Ok(Res::Json(String::from(env.get_string(json_string.l()?.into())?)))
                }
                Err(jni_error) => match jni_error.0 {
                    ErrorKind::JavaException => {
                        let exception: JObject = env.exception_occurred()?.into();
                        assert!(!exception.is_null());
                        env.exception_clear()?;
                        if env.is_instance_of(exception, CLASS_JL_ERROR)? {
                            let message = format!(
                                "Java exception occurred: {}\n{:#?}",
                                utils::get_class_name(env, exception)?, jni_error.backtrace());
                            panic!(message)
                        } else {
                            Ok(Res::Error(format!("Serialization error: {:?}", &jni_error).into()))
                        }
                    },
                    _ => Err(jni_error),
                }
            }
        })
            .unwrap_or_else(|err| {
                panic!("JNI error occurred: {:?}", err);
            });

        match res {
            Res::Json(json_string) => Ok(serde_json::from_str(&json_string)?),
            Res::Error(err) => Err(err)?,
        }
    }
}

impl<E> Transaction for TransactionProxy<E>
where
    E: Executor + 'static,
{
    fn verify(&self) -> bool {
        self.exec.with_attached(|env: &JNIEnv| {
            let res = env.call_method(self.transaction.as_obj(), "isValid", "()Z", &[]);
            if let Err(ref jni_error) = res {
                match jni_error.0 {
                    ErrorKind::JavaException => {
                        let exception: JObject = env.exception_occurred()?.into();
                        assert!(!exception.is_null());
                        env.exception_clear()?;
                        let message = format!(
                            "Java exception occurred: {}\n{:#?}",
                            utils::get_class_name(env, exception)?, jni_error.backtrace());
                        panic!(message)
                    },
                    _ => {},
                }
            }
            res?.z()
        })
            .unwrap_or_else(|err| {
                panic!("JNI error occurred: {:?}", err);
            })
    }

    fn execute(&self, fork: &mut Fork) {
        self.exec.with_attached(|env: &JNIEnv| {
            let view_handle = utils::to_handle(View::from_ref_fork(fork));
            let res = env.call_method(
                self.transaction.as_obj(),
                "execute",
                "(J)V",
                &[JValue::from(view_handle)],
            );
            if let Err(ref jni_error) = res {
                match jni_error.0 {
                    ErrorKind::JavaException => {
                        let exception: JObject = env.exception_occurred()?.into();
                        assert!(!exception.is_null());
                        env.exception_clear()?;
                        let message = format!(
                            "Java exception occurred: {}\n{:#?}",
                            utils::get_class_name(env, exception)?, jni_error.backtrace());
                        panic!(message)
                    },
                    _ => {},
                }
            }
            res?.v()
        })
            .unwrap_or_else(|err| {
                panic!("JNI error occurred: {:?}", err);
            })
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
        unimplemented!("Does not used in Java bindings")
    }

    fn raw(&self) -> &RawMessage {
        &self.raw
    }
}
