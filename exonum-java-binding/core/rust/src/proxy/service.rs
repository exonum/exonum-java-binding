/*
 * Copyright 2019 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

use exonum::api::ServiceApiBuilder;
use exonum::blockchain::{Service, ServiceContext, Transaction};
use exonum::crypto::Hash;
use exonum::messages::RawTransaction;
use exonum::storage::{Fork, Snapshot};
use failure;
use jni::objects::{GlobalRef, JObject, JValue};
use jni::signature::JavaType;
use jni::Executor;
use serde_json;
use serde_json::value::Value;
use std::fmt;
use std::str::FromStr;

use handle::to_handle;
use proxy::node::NodeContext;
use storage::View;
use utils::{
    check_error_on_exception, convert_to_hash, convert_to_string, jni_cache::service_adapter,
    panic_on_exception, unwrap_jni,
};
use TransactionProxy;

/// A proxy for `Service`s.
#[derive(Clone)]
pub struct ServiceProxy {
    exec: Executor,
    service: GlobalRef,
    id: u16,
    name: String,
}

impl fmt::Debug for ServiceProxy {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "ServiceProxy(id={},name={})", self.id, self.name)
    }
}

impl ServiceProxy {
    /// Creates a `ServiceProxy` of the given Java service.
    pub fn from_global_ref(exec: Executor, service: GlobalRef) -> Self {
        let (id, name) = unwrap_jni(exec.with_attached(|env| {
            let id =
                panic_on_exception(env, env.call_method(service.as_obj(), "getId", "()S", &[]));
            let name = panic_on_exception(
                env,
                env.call_method(service.as_obj(), "getName", "()Ljava/lang/String;", &[]),
            );
            // Note: Exonum uses an unsigned `u16` int value for ids, while Java can only use
            // signed `short` ints.
            // Service negative values in Java (from -32768 to -1) will be translated
            // into the higher half of `u16` (respectively from 32768 to 65535).
            let id = id.s()? as u16;
            let name = convert_to_string(env, name.l()?)?;
            Ok((id, name))
        }));
        ServiceProxy {
            exec,
            service,
            id,
            name,
        }
    }
}

impl Service for ServiceProxy {
    fn service_id(&self) -> u16 {
        self.id
    }

    fn service_name(&self) -> &str {
        &self.name
    }

    fn state_hash(&self, snapshot: &Snapshot) -> Vec<Hash> {
        unwrap_jni(self.exec.with_attached(|env| {
            let view_handle = to_handle(View::from_ref_snapshot(snapshot));
            let java_service_hashes = panic_on_exception(
                env,
                env.call_method_unchecked(
                    self.service.as_obj(),
                    service_adapter::state_hashes_id(),
                    JavaType::from_str("[[B").unwrap(),
                    &[JValue::from(view_handle)],
                ),
            );
            let byte_array_array = java_service_hashes.l()?.into_inner();
            let len = env.get_array_length(byte_array_array)?;
            let mut hashes: Vec<Hash> = Vec::with_capacity(len as usize);
            for i in 0..len {
                let byte_array = env.get_object_array_element(byte_array_array, i)?;
                hashes.push(convert_to_hash(env, byte_array.into_inner())?);
            }
            Ok(hashes)
        }))
    }

    fn tx_from_raw(&self, raw: RawTransaction) -> Result<Box<dyn Transaction>, failure::Error> {
        unwrap_jni(self.exec.with_attached(|env| {
            let (tx_id, payload) = raw.service_transaction().into_raw_parts();
            let payload = JObject::from(env.byte_array_from_slice(&payload)?);

            let res = env.call_method_unchecked(
                self.service.as_obj(),
                service_adapter::convert_transaction_id(),
                JavaType::Object(
                    "com/exonum/binding/core/service/adapters/UserTransactionAdapter".into(),
                ),
                &[JValue::from(tx_id), JValue::from(payload)],
            );
            // TODO consider whether `NullPointerException` should raise a panic:
            // [https://jira.bf.local/browse/ECR-944]
            Ok(match check_error_on_exception(env, res) {
                Ok(java_transaction) => {
                    let java_transaction_proxy = TransactionProxy::from_global_ref(
                        self.exec.clone(),
                        env.new_global_ref(java_transaction.l()?)?,
                    );
                    Ok(Box::new(java_transaction_proxy) as Box<Transaction>)
                }
                Err(error_message) => Err(format_err!("{}", error_message)),
            })
        }))
    }

    fn initialize(&self, fork: &mut Fork) -> Value {
        let json_config = unwrap_jni(self.exec.with_attached(|env| {
            let view_handle = to_handle(View::from_ref_fork(fork));
            let json_config = panic_on_exception(
                env,
                env.call_method(
                    self.service.as_obj(),
                    "initialize",
                    "(J)Ljava/lang/String;",
                    &[JValue::from(view_handle)],
                ),
            )
            .l()?;
            if json_config.is_null() {
                Ok(None)
            } else {
                Ok(Some(convert_to_string(env, json_config)?))
            }
        }));
        match json_config {
            None => Value::Null,
            Some(json_config) => serde_json::from_str(&json_config)
                .map_err(|e| {
                    panic!(
                        "JSON deserialization error: {:?}; json string: {:?}",
                        e, json_config
                    )
                })
                .unwrap(),
        }
    }

    fn after_commit(&self, context: &ServiceContext) {
        unwrap_jni(self.exec.with_attached(|env| {
            let view_handle = to_handle(View::from_ref_snapshot(context.snapshot()));
            let validator_id = context.validator_id().map_or(-1, |id| i32::from(id.0));
            let height: u64 = context.height().into();
            panic_on_exception(
                env,
                env.call_method(
                    self.service.as_obj(),
                    "afterCommit",
                    "(JIJ)V",
                    &[
                        JValue::from(view_handle),
                        JValue::from(validator_id),
                        JValue::from(height as i64),
                    ],
                ),
            );
            Ok(())
        }));
    }

    fn wire_api(&self, builder: &mut ServiceApiBuilder) {
        assert!(builder.blockchain().is_some());

        let node = NodeContext::new(
            self.exec.clone(),
            builder.blockchain().unwrap().clone(),
            builder.public_key().unwrap(),
            builder.api_sender().unwrap().clone(),
        );

        unwrap_jni(self.exec.with_attached(|env| {
            let node_handle = to_handle(node);
            panic_on_exception(
                env,
                env.call_method(
                    self.service.as_obj(),
                    "mountPublicApiHandler",
                    "(J)V",
                    &[JValue::from(node_handle)],
                ),
            );
            Ok(())
        }));
    }
}
