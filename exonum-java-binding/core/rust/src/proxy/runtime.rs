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
    helpers::ValidatorId,
    messages::BinaryValue,
    exonum_merkledb::{self, Fork, Snapshot},
    node::ApiSender,
    runtime::{
        api::ServiceApiBuilder,
        dispatcher::{Dispatcher, DispatcherRef, DispatcherSender},
        ApiChange, ArtifactId, ArtifactProtobufSpec, CallInfo, ErrorKind, ExecutionContext,
        ExecutionError, InstanceDescriptor, InstanceId, InstanceSpec, Runtime, RuntimeIdentifier,
        StateHashAggregator,
    },
};
use futures::{Future, IntoFuture};
use jni::{
    objects::{GlobalRef, JObject, JValue},
    signature::{JavaType, Primitive},
    Executor,
};
use proto;
use proxy::node::NodeContext;
use runtime::Error;
use std::fmt;
use storage::View;
use to_handle;
use utils::{jni_cache::runtime_adapter, log_jni_error_or_exception, panic_on_exception, unwrap_jni};
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

    fn parse_jni<T>(res: JniResult<T>) -> Result<T, ExecutionError> {
        res.map_err(|err| {
            let kind: ErrorKind = match err.kind() {
                JniErrorKind::JavaException => Error::JavaException.into(),
                _ => Error::UnspecifiedError.into(),
            };
            (kind, err).into()
        })
    }

    /// If the current node is a validator, returns `Some(validator_id)`, for other nodes return `None`.
    fn validator_id(snapshot: &dyn Snapshot, pub_key: &PublicKey) -> Option<ValidatorId> {
        CoreSchema::new(snapshot)
            .consensus_config()
            .find_validator(|validator_keys| *pub_key == validator_keys.service_key)
    }

    /// If the current node is a validator, return its identifier, for other nodes return `default`.
    fn validator_id_or(
        snapshot: &dyn Snapshot,
        pub_key: &PublicKey,
        default: i32
    ) -> i32 {
        Self::validator_id(snapshot, pub_key)
            .map_or(default, |id| i32::from(id.0))
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

        Box::new(Self::parse_jni(self.exec.with_attached(|env| {
            let artifact_id = JObject::from(env.new_string(id)?);
            let spec = JObject::from(env.byte_array_from_slice(&deploy_spec)?);

            log_jni_error_or_exception(
                env,
                env.call_method_unchecked(
                    self.runtime_adapter.as_obj(),
                    runtime_adapter::deploy_artifact_id(),
                    JavaType::Primitive(Primitive::Void),
                    &[
                        JValue::from(artifact_id),
                        JValue::from(spec),
                    ],
                )
            ).map(|_| ())
        }))
        .into_future())
    }

    fn is_artifact_deployed(&self, id: &ArtifactId) -> bool {
        let artifact = match self.parse_artifact(id) {
            Ok(id) => id.to_string(),
            Err(err) => {
                return false;
            },
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
            ).z()
        }))
    }

    fn artifact_protobuf_spec(&self, _id: &ArtifactId) -> Option<ArtifactProtobufSpec> {
        Some(ArtifactProtobufSpec::default())
    }

    fn start_service(&mut self, spec: &InstanceSpec) -> Result<(), ExecutionError> {
        let adapter = self.runtime_adapter.as_obj().clone();
        let service_name = spec.name.clone();
        let id = spec.id;
        let artifact = self.parse_artifact(&spec.artifact)?;

        Self::parse_jni(self.exec.with_attached(|env| {
            let name = JObject::from(env.new_string(service_name)?);
            let artifact_id = JObject::from(env.new_string(artifact.to_string())?);

            log_jni_error_or_exception(
                env,
                env.call_method_unchecked(
                    adapter,
                    runtime_adapter::create_service_id(),
                    JavaType::Primitive(Primitive::Void),
                    &[
                        JValue::from(name),
                        JValue::from(id as i32),
                        JValue::from(artifact_id),
                    ],
                )
            ).map(|_| ())
        }))
    }

    fn initialize_service(
        &self,
        fork: &Fork,
        descriptor: InstanceDescriptor,
        parameters: Vec<u8>,
    ) -> Result<(), ExecutionError> {

        Self::parse_jni(self.exec.with_attached(|env| {
            let id = descriptor.id as i32;
            let view_handle = to_handle(View::from_ref_fork(fork));
            let params = JObject::from(env.byte_array_from_slice(&parameters)?);

            log_jni_error_or_exception(
                env,
                env.call_method_unchecked(
                    self.runtime_adapter.as_obj(),
                    runtime_adapter::initialize_service_id(),
                    JavaType::Primitive(Primitive::Void),
                    &[
                        JValue::from(id),
                        JValue::from(view_handle),
                        JValue::from(params),
                    ],
                )
            )
                .map(|_| ())
        }))
    }

    fn stop_service(&mut self, descriptor: InstanceDescriptor) -> Result<(), ExecutionError> {
        let adapter = self.runtime_adapter.as_obj();

        Self::parse_jni(self.exec.with_attached(|env| {
            let id = descriptor.id as i32;

            log_jni_error_or_exception(
                env,
                env.call_method_unchecked(
                    adapter,
                    runtime_adapter::stop_service_id(),
                    JavaType::Primitive(Primitive::Void),
                    &[
                        JValue::from(id),
                    ],
                )
            ).map(|_| ())
        }))
    }

    fn execute(
        &self,
        context: &ExecutionContext,
        call_info: &CallInfo,
        arguments: &[u8],
    ) -> Result<(), ExecutionError> {
        let tx = if let Some((hash, pub_key)) = context.caller.as_transaction()
        {
            (hash.to_bytes(), pub_key.to_bytes())
        } else {
            // TODO: caller is Blockchain (not Transaction) is not supported  yet
            return Err(Error::NotSupportedOperation.into());
        };

        Self::parse_jni(self.exec.with_attached(|env| {
            let service_id = call_info.instance_id as i32;
            let tx_id = call_info.method_id as i32;
            let args = JObject::from(env.byte_array_from_slice(arguments)?);
            let view_handle = to_handle(View::from_ref_fork(context.fork));
            let pub_key = JObject::from(env.byte_array_from_slice(&tx.0)?);
            let hash = JObject::from(env.byte_array_from_slice(&tx.1)?);

            log_jni_error_or_exception(
                env,
                env.call_method_unchecked(
                    self.runtime_adapter.as_obj(),
                    runtime_adapter::execute_tx_id(),
                    JavaType::Primitive(Primitive::Void),
                    &[
                        JValue::from(service_id),
                        JValue::from(tx_id),
                        JValue::from(args),
                        JValue::from(view_handle),
                        JValue::from(pub_key),
                        JValue::from(hash),
                    ],
                )
            ).map(|_| ())
        }))
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
            let view_handle = to_handle(View::from_ref_fork(fork));

            panic_on_exception(
                env,
                env.call_method_unchecked(
                    self.runtime_adapter.as_obj(),
                    runtime_adapter::before_commit_id(),
                    JavaType::Primitive(Primitive::Void),
                    &[
                        JValue::from(view_handle),
                    ],
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
            let height:u64 = CoreSchema::new(snapshot).height().into();

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

    fn api_endpoints(&self, context: &ApiContext) -> Vec<(String, ServiceApiBuilder)> {
        Vec::<(String, ServiceApiBuilder)>::new()
    }

    fn notify_api_changes(&self, _context: &ApiContext, _changes: &[ApiChange]) {

    }
}

impl fmt::Debug for JavaRuntimeProxy {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "JavaRuntimeProxy()")
    }
}

/// Artifact identification properties within `JavaRuntimeProxy`
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct JavaArtifactId(String);

impl ToString for JavaArtifactId {
    fn to_string(&self) -> String {
        self.0.to_owned()
    }
}

#[derive(Serialize, Deserialize, Clone, ProtobufConvert, PartialEq)]
#[exonum(pb = "proto::ServiceStateHashes")]
struct ServiceStateHashes {
    instance_id: u32,
    state_hashes: Vec<Vec<u8>>,
}

impl ServiceStateHashes {
    fn to_hashes(&self) -> (InstanceId, Vec<Hash>) {
        let hashes = self.state_hashes
            .iter()
            .map(|bytes| Hash::from_bytes(bytes.into()).unwrap())
            .collect();
        (self.instance_id, hashes)
    }
}

#[derive(Serialize, Deserialize, Clone, ProtobufConvert, PartialEq)]
#[exonum(pb = "proto::ServiceRuntimeStateHashes")]
struct ServiceRuntimeStateHashes {
    runtime_state_hashes: Vec<Vec<u8>>,
    service_state_hashes: Vec<ServiceStateHashes>,
}

impl ServiceRuntimeStateHashes {
    fn runtime(&self) -> Vec<Hash> {
        self.runtime_state_hashes
            .iter()
            .map(|bytes| Hash::from_bytes(bytes.into()).unwrap())
            .collect()
    }

    fn instances(&self) -> Vec<(InstanceId, Vec<Hash>)> {
        self.service_state_hashes
            .iter()
            .map(ServiceStateHashes::to_hashes)
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
