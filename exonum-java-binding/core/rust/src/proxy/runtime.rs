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

use exonum::{
    api::ApiContext,
    blockchain::Schema as CoreSchema,
    crypto::{Hash, PublicKey, SecretKey},
    exonum_merkledb::{self, Fork, Snapshot},
    helpers::ValidatorId,
    messages::BinaryValue,
    node::ApiSender,
    runtime::{
        dispatcher::{DispatcherRef, DispatcherSender},
        ApiChange, ArtifactId, ArtifactProtobufSpec, CallInfo, ErrorKind, ExecutionContext,
        ExecutionError, InstanceId, InstanceSpec, Runtime, RuntimeIdentifier, StateHashAggregator,
    },
};
use futures::{Future, IntoFuture};
use jni::{
    objects::{GlobalRef, JObject, JValue},
    signature::{JavaType, Primitive},
    Executor, JNIEnv,
};
use proto;
use proxy::node::NodeContext;
use runtime::Error;
use std::fmt;
use storage::View;
use to_handle;
use utils::{
    describe_java_exception, get_and_clear_java_exception, get_exception_message,
    jni_cache::{classes_refs, runtime_adapter, tx_execution_exception},
    panic_on_exception, unwrap_jni,
};
use JniErrorKind;
use JniResult;

/// A proxy for `ServiceRuntimeAdapter`s.
#[derive(Clone)]
pub struct JavaRuntimeProxy {
    exec: Executor,
    runtime_adapter: GlobalRef,
}

impl JavaRuntimeProxy {
    /// Runtime Identifier
    pub const RUNTIME_ID: RuntimeIdentifier = RuntimeIdentifier::Java;

    /// Creates new `JavaRuntimeProxy` for given `ServiceRuntimeAdapter` object
    pub fn new(executor: Executor, adapter: GlobalRef) -> Self {
        JavaRuntimeProxy {
            exec: executor,
            runtime_adapter: adapter,
        }
    }

    fn parse_artifact(&self, artifact: &ArtifactId) -> Result<JavaArtifactId, ExecutionError> {
        if artifact.runtime_id != Self::RUNTIME_ID as u32 {
            Err(Error::IncorrectArtifactId.into())
        } else {
            Ok(JavaArtifactId(artifact.name.to_string()))
        }
    }

    /// If the current node is a validator, returns `Some(validator_id)`, for other nodes return `None`.
    fn validator_id(snapshot: &dyn Snapshot, pub_key: &PublicKey) -> Option<ValidatorId> {
        CoreSchema::new(snapshot)
            .consensus_config()
            .find_validator(|validator_keys| *pub_key == validator_keys.service_key)
    }

    /// If the current node is a validator, return its identifier, for other nodes return `default`.
    fn validator_id_or(snapshot: &dyn Snapshot, pub_key: &PublicKey, default: i32) -> i32 {
        Self::validator_id(snapshot, pub_key).map_or(default, |id| i32::from(id.0))
    }

    /// Executes closure `f` and handles any type of JNI errors from it.
    ///
    /// Does not perform handling of `TransactionExecutionException`.
    fn jni_call_default<F, R>(&self, f: F) -> Result<R, ExecutionError>
    where
        F: FnOnce(&JNIEnv) -> JniResult<R>,
    {
        self.jni_call_impl(f, false)
    }

    /// Executes closure `f` and handles any type of JNI errors from it.
    ///
    /// Performs handling of `TransactionExecutionException`.
    fn jni_call_transaction_execute<F, R>(&self, f: F) -> Result<R, ExecutionError>
    where
        F: FnOnce(&JNIEnv) -> JniResult<R>,
    {
        self.jni_call_impl(f, true)
    }

    /// Executes closure `f` and handles any type of JNI errors from it.
    ///
    /// Any JNI errors are converted into `ExecutionError` with their descriptions, for JNI errors
    /// like `JniErrorKind::JavaException` it gets (and clears) any exception that is currently
    /// being thrown, `handle_java_exception` is applied for such exceptions.
    fn jni_call_impl<F, R>(
        &self,
        f: F,
        check_for_transaction_execution_exception: bool,
    ) -> Result<R, ExecutionError>
    where
        F: FnOnce(&JNIEnv) -> JniResult<R>,
    {
        let result = self.exec.with_attached(|env| {
            // It looks like `f(env).map_err(|err| ...)` and it actually is, but we can't use
            // it here because of different types of returned results and usage of
            // question mark operator.
            Ok(match f(env) {
                Ok(value) => Ok(value),
                Err(err) => {
                    let execution_error = match err.kind() {
                        JniErrorKind::JavaException => {
                            handle_java_exception(env, check_for_transaction_execution_exception)?
                        }
                        _ => (Error::OtherJniError, err).into(),
                    };
                    Err(execution_error)
                }
            })
        });

        // If any error occurred in Java Bindings glue code, we return `OtherJniError`.
        result.unwrap_or_else(|e| {
            Err(ExecutionError::new(
                Error::OtherJniError.into(),
                format!("Unexpected JNI error: {:?}", e),
            ))
        })
    }
}

impl Runtime for JavaRuntimeProxy {
    fn deploy_artifact(
        &mut self,
        artifact: ArtifactId,
        deploy_spec: Vec<u8>,
    ) -> Box<dyn Future<Item = (), Error = ExecutionError>> {
        let id = match self.parse_artifact(&artifact) {
            Ok(id) => id.to_string(),
            Err(err) => return Box::new(Err(err).into_future()),
        };

        let result = self.jni_call_default(|env| {
            let artifact_id = JObject::from(env.new_string(id)?);
            let spec = JObject::from(env.byte_array_from_slice(&deploy_spec)?);

            env.call_method_unchecked(
                self.runtime_adapter.as_obj(),
                runtime_adapter::deploy_artifact_id(),
                JavaType::Primitive(Primitive::Void),
                &[JValue::from(artifact_id), JValue::from(spec)],
            )
            .and_then(JValue::v)
        });

        Box::new(result.into_future())
    }

    fn is_artifact_deployed(&self, id: &ArtifactId) -> bool {
        let artifact = match self.parse_artifact(id) {
            Ok(id) => id.to_string(),
            Err(_) => {
                return false;
            }
        };

        unwrap_jni(self.exec.with_attached(|env| {
            let artifact_id = JObject::from(env.new_string(artifact)?);

            panic_on_exception(
                env,
                env.call_method_unchecked(
                    self.runtime_adapter.as_obj(),
                    runtime_adapter::is_artifact_deployed_id(),
                    JavaType::Primitive(Primitive::Boolean),
                    &[JValue::from(artifact_id)],
                ),
            )
            .z()
        }))
    }

    fn artifact_protobuf_spec(&self, _id: &ArtifactId) -> Option<ArtifactProtobufSpec> {
        Some(ArtifactProtobufSpec::default())
    }

    fn restart_service(&mut self, spec: &InstanceSpec) -> Result<(), ExecutionError> {
        let serialized_instance_spec: Vec<u8> = spec.to_bytes();

        self.jni_call_default(|env| {
            let instance_spec =
                JObject::from(env.byte_array_from_slice(&serialized_instance_spec)?);
            env.call_method_unchecked(
                self.runtime_adapter.as_obj(),
                runtime_adapter::restart_service_id(),
                JavaType::Primitive(Primitive::Void),
                &[JValue::from(instance_spec)],
            )
            .and_then(JValue::v)
        })
    }

    fn add_service(
        &mut self,
        fork: &mut Fork,
        instance_spec: &InstanceSpec,
        parameters: Vec<u8>,
    ) -> Result<(), ExecutionError> {
        let serialized_instance_spec: Vec<u8> = instance_spec.to_bytes();
        self.jni_call_default(|env| {
            let instance_spec =
                JObject::from(env.byte_array_from_slice(&serialized_instance_spec)?);
            let view_handle = to_handle(View::from_ref_fork(fork));
            let params = JObject::from(env.byte_array_from_slice(&parameters)?);

            env.call_method_unchecked(
                self.runtime_adapter.as_obj(),
                runtime_adapter::add_service_id(),
                JavaType::Primitive(Primitive::Void),
                &[
                    JValue::from(view_handle),
                    JValue::from(instance_spec),
                    JValue::from(params),
                ],
            )
            .and_then(JValue::v)
        })
    }

    fn execute(
        &self,
        context: &ExecutionContext,
        call_info: &CallInfo,
        arguments: &[u8],
    ) -> Result<(), ExecutionError> {
        let tx = match context.caller.as_transaction() {
            Some((hash, pub_key)) => (hash.to_bytes(), pub_key.to_bytes()),
            None => {
                // TODO (ECR-3702): caller is Service (not Transaction) is not supported yet
                return Err(Error::NotSupportedOperation.into());
            }
        };

        self.jni_call_transaction_execute(|env| {
            let service_id = call_info.instance_id as i32;
            let tx_id = call_info.method_id as i32;
            let args = JObject::from(env.byte_array_from_slice(arguments)?);
            let view_handle = to_handle(View::from_ref_fork(context.fork));
            let hash = JObject::from(env.byte_array_from_slice(&tx.0)?);
            let pub_key = JObject::from(env.byte_array_from_slice(&tx.1)?);

            env.call_method_unchecked(
                self.runtime_adapter.as_obj(),
                runtime_adapter::execute_tx_id(),
                JavaType::Primitive(Primitive::Void),
                &[
                    JValue::from(service_id),
                    JValue::from(tx_id),
                    JValue::from(args),
                    JValue::from(view_handle),
                    JValue::from(hash),
                    JValue::from(pub_key),
                ],
            )
            .and_then(JValue::v)
        })
    }

    fn state_hashes(&self, snapshot: &dyn Snapshot) -> StateHashAggregator {
        let bytes = unwrap_jni(self.exec.with_attached(|env| {
            let view_handle = to_handle(View::from_ref_snapshot(snapshot));
            let java_runtime_hashes = panic_on_exception(
                env,
                env.call_method_unchecked(
                    self.runtime_adapter.as_obj(),
                    runtime_adapter::state_hashes_id(),
                    JavaType::Array(Box::new(JavaType::Primitive(Primitive::Byte))),
                    &[JValue::from(view_handle)],
                ),
            );
            let byte_array = java_runtime_hashes.l()?.into_inner();
            let data = env.convert_byte_array(byte_array)?;

            Ok(data)
        }));

        ServiceRuntimeStateHashes::from_bytes(bytes.into())
            .unwrap()
            .into()
    }

    fn before_commit(&self, _dispatcher: &DispatcherRef, fork: &mut Fork) {
        unwrap_jni(self.exec.with_attached(|env| {
            let view_handle = to_handle(View::from_ref_mut_fork(fork));

            panic_on_exception(
                env,
                env.call_method_unchecked(
                    self.runtime_adapter.as_obj(),
                    runtime_adapter::before_commit_id(),
                    JavaType::Primitive(Primitive::Void),
                    &[JValue::from(view_handle)],
                ),
            );
            Ok(())
        }));
    }

    fn after_commit(
        &self,
        _dispatcher: &DispatcherSender,
        snapshot: &Snapshot,
        service_keypair: &(PublicKey, SecretKey),
        _tx_sender: &ApiSender,
    ) {
        unwrap_jni(self.exec.with_attached(|env| {
            let view_handle = to_handle(View::from_ref_snapshot(snapshot));
            let validator_id = Self::validator_id_or(snapshot, &service_keypair.0, -1);
            let height: u64 = CoreSchema::new(snapshot).height().into();

            panic_on_exception(
                env,
                env.call_method_unchecked(
                    self.runtime_adapter.as_obj(),
                    runtime_adapter::after_commit_id(),
                    JavaType::Primitive(Primitive::Void),
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

    fn notify_api_changes(&self, context: &ApiContext, changes: &[ApiChange]) {
        let added_instances_ids: Vec<i32> = changes
            .iter()
            .filter_map(|change| {
                if let ApiChange::InstanceAdded(instance_id) = change {
                    Some(*instance_id as i32)
                } else {
                    None
                }
            })
            .collect();

        if added_instances_ids.is_empty() {
            return;
        }

        let node = NodeContext::new(self.exec.clone(), context.clone());

        unwrap_jni(self.exec.with_attached(|env| {
            let node_handle = to_handle(node);
            let ids_array = env.new_int_array(added_instances_ids.len() as i32)?;
            env.set_int_array_region(ids_array, 0, &added_instances_ids)?;
            let service_ids = JObject::from(ids_array);

            panic_on_exception(
                env,
                env.call_method_unchecked(
                    self.runtime_adapter.as_obj(),
                    runtime_adapter::connect_apis_id(),
                    JavaType::Primitive(Primitive::Void),
                    &[JValue::from(service_ids), JValue::from(node_handle)],
                ),
            );
            Ok(())
        }));
    }

    fn shutdown(&self) {
        unwrap_jni(self.exec.with_attached(|env| {
            panic_on_exception(
                env,
                env.call_method_unchecked(
                    self.runtime_adapter.as_obj(),
                    runtime_adapter::shutdown_id(),
                    JavaType::Primitive(Primitive::Void),
                    &[],
                ),
            );
            Ok(())
        }));
    }
}

impl fmt::Debug for JavaRuntimeProxy {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "JavaRuntimeProxy()")
    }
}

impl From<JavaRuntimeProxy> for (u32, Box<dyn Runtime>) {
    fn from(r: JavaRuntimeProxy) -> Self {
        (JavaRuntimeProxy::RUNTIME_ID as u32, Box::new(r))
    }
}

/// Artifact identification properties within `JavaRuntimeProxy`
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct JavaArtifactId(String);

impl fmt::Display for JavaArtifactId {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "{}", self.0)
    }
}

#[derive(Serialize, Deserialize, Clone, ProtobufConvert, PartialEq)]
#[protobuf_convert(source = "proto::ServiceStateHashes")]
struct ServiceStateHashes {
    instance_id: u32,
    state_hashes: Vec<Vec<u8>>,
}

impl From<&ServiceStateHashes> for (InstanceId, Vec<Hash>) {
    fn from(value: &ServiceStateHashes) -> Self {
        let hashes: Vec<Hash> = value
            .state_hashes
            .iter()
            .map(|bytes| to_hash(bytes))
            .collect();
        (value.instance_id, hashes)
    }
}

#[derive(Serialize, Deserialize, Clone, ProtobufConvert, BinaryValue, PartialEq)]
#[protobuf_convert(source = "proto::ServiceRuntimeStateHashes")]
struct ServiceRuntimeStateHashes {
    runtime_state_hashes: Vec<Vec<u8>>,
    service_state_hashes: Vec<ServiceStateHashes>,
}

impl ServiceRuntimeStateHashes {
    fn runtime(&self) -> Vec<Hash> {
        self.runtime_state_hashes
            .iter()
            .map(|bytes| to_hash(bytes))
            .collect()
    }

    fn instances(&self) -> Vec<(InstanceId, Vec<Hash>)> {
        self.service_state_hashes
            .iter()
            .map(|service| service.into())
            .collect()
    }
}

impl From<ServiceRuntimeStateHashes> for StateHashAggregator {
    fn from(value: ServiceRuntimeStateHashes) -> Self {
        StateHashAggregator {
            runtime: value.runtime(),
            instances: value.instances(),
        }
    }
}

fn to_hash(bytes: &[u8]) -> Hash {
    Hash::from_bytes(bytes.into()).unwrap()
}

/// Converts Java exception to `ExecutionError`.
///
/// If `check_for_transaction_execution_exception` is true, returns `ErrorKind::Service` with
/// user-provided error code and error message, else returns `ErrorKind::Runtime` with
/// `Error::JavaException` code.
fn handle_java_exception(
    env: &JNIEnv,
    check_for_transaction_execution_exception: bool,
) -> JniResult<ExecutionError> {
    let exception = get_and_clear_java_exception(env);

    // If the exception is instance of `TransactionExecutionException` and we should check for it,
    // use `ErrorKind::Service`.
    if check_for_transaction_execution_exception
        && is_transaction_execution_exception(env, exception)?
    {
        let code = get_tx_error_code(env, exception)? as u8;
        let message = get_exception_message(env, exception)?.unwrap_or_default();
        Ok(ExecutionError::new(ErrorKind::service(code), message))
    } else {
        let message = describe_java_exception(env, exception);
        Ok(ExecutionError::new(Error::JavaException.into(), message))
    }
}

/// Returns `true` if exception is instance of `TransactionExecutionException` class.
fn is_transaction_execution_exception(env: &JNIEnv, exception: JObject) -> JniResult<bool> {
    env.is_instance_of(
        exception,
        classes_refs::transaction_execution_exception().as_obj(),
    )
}

/// Returns user-provided error code of the `TransactionExecutionException`.
fn get_tx_error_code(env: &JNIEnv, exception: JObject) -> JniResult<i8> {
    let err_code = env.call_method_unchecked(
        exception,
        tx_execution_exception::get_error_code_id(),
        JavaType::Primitive(Primitive::Byte),
        &[],
    )?;
    err_code.b()
}
