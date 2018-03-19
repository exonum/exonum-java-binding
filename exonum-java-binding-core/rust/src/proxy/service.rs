use exonum::blockchain::{Service, Transaction};
use exonum::crypto::Hash;
use exonum::encoding::Error as MessageError;
use exonum::storage::{Fork, Snapshot};
use exonum::messages::RawTransaction;
use jni::objects::{GlobalRef, JObject, JValue};
use serde_json;
use serde_json::value::Value;

use std::fmt;

use Executor;
use TransactionProxy;
use storage::View;
use utils::{check_error_on_exception, convert_to_hash, convert_to_string, panic_on_exception,
            to_handle, unwrap_jni};

/// A proxy for `Service`s.
#[derive(Clone)]
pub struct ServiceProxy<E>
where
    E: Executor + 'static,
{
    exec: E,
    service: GlobalRef,
    id: u16,
    name: String,
}

// `ServiceProxy` is immutable, so it can be safely used in different threads.
unsafe impl<E> Sync for ServiceProxy<E>
where
    E: Executor + 'static,
{
}

impl<E> fmt::Debug for ServiceProxy<E>
where
    E: Executor + 'static,
{
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "ServiceProxy(id={},name={})", self.id, self.name)
    }
}

impl<E> ServiceProxy<E>
where
    E: Executor + 'static,
{
    /// Creates a `ServiceProxy` of the given Java service.
    pub fn from_global_ref(exec: E, service: GlobalRef) -> Self {
        let (id, name) = unwrap_jni(exec.with_attached(|env| {
            let id = panic_on_exception(env, env.call_method(
                service.as_obj(),
                "getId",
                "()S",
                &[],
            ))
                .s()?;
            let name = panic_on_exception(env, env.call_method(
                service.as_obj(),
                "getName",
                "()Ljava/lang/String;",
                &[],
            ))
                .l()?;
            // There is no u16 in Java.
            // FIXME document i16 <-> u16
            Ok((id as u16, convert_to_string(env, name)?))
        }));
        ServiceProxy {
            exec,
            service,
            id,
            name,
        }
    }
}

impl<E> Service for ServiceProxy<E>
where
    E: Executor + 'static,
{
    fn service_id(&self) -> u16 {
        self.id
    }

    fn service_name(&self) -> &'static str {
        // FIXME `'static` lifetime removed in Exonum 0.6 [https://jira.bf.local/browse/ECR-912].
        // dirty hack
        unsafe { &*(self.name.as_str() as *const str) }
        // &self.name
    }

    fn state_hash(&self, snapshot: &Snapshot) -> Vec<Hash> {
        unwrap_jni(self.exec.with_attached(|env| {
            let view_handle = to_handle(View::from_ref_snapshot(snapshot));
            let byte_array_array = panic_on_exception(env, env.call_method(
                self.service.as_obj(),
                "getStateHashes",
                "(J)[[B", // FIXME sig?
                &[JValue::from(view_handle)],
            ));
            let byte_array_array = byte_array_array.l()?.into_inner();
            let len = env.get_array_length(byte_array_array)?;
            let mut hashes: Vec<Hash> = Vec::with_capacity(len as usize);
            for i in 0..len {
                let byte_array = env.get_object_array_element(byte_array_array, i)?;
                hashes.push(convert_to_hash(env, byte_array.into_inner())?);
            }
            Ok(hashes)
        }))
    }

    fn tx_from_raw(&self, raw: RawTransaction) -> Result<Box<Transaction>, MessageError> {
        unwrap_jni(self.exec.with_attached(|env| {
            let transaction_message = JObject::from(env.byte_array_from_slice(raw.as_ref())?);
            let res = env.call_method(
                self.service.as_obj(),
                "convertTransaction",
                "([B)Lcom/exonum/binding/service/adapters/UserTransactionAdapter;",
                &[JValue::from(transaction_message)],
            );
            // TODO consider whether `NullPointerException` should raise a panic:
            // [https://jira.bf.local/browse/ECR-944]
            Ok(match check_error_on_exception(env, res) {
                Ok(java_transaction) => {
                    let local_ref = env.auto_local(java_transaction.l()?);
                    let global_ref = env.new_global_ref(local_ref.as_obj())?;
                    let java_transaction_proxy = TransactionProxy::from_global_ref(
                        self.exec.clone(), global_ref, raw);
                    Ok(Box::new(java_transaction_proxy) as Box<Transaction>)
                },
                Err(error_message) => {
                    Err(MessageError::Basic(error_message.into()))
                }
            })
        }))
    }

    fn initialize(&self, fork: &mut Fork) -> Value {
        let json_config = unwrap_jni(self.exec.with_attached(|env| {
            let view_handle = to_handle(View::from_ref_fork(fork));
            let json_config = panic_on_exception(env, env.call_method(
                self.service.as_obj(),
                "initialize",
                "(J)Ljava/lang/String;",
                &[JValue::from(view_handle)],
            ))
                .l()?;
            convert_to_string(env, json_config)
        }));
        serde_json::from_str(&json_config).expect("JSON deserialization error")
    }
}
