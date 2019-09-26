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
    helpers::{Height, ValidatorId},
    messages::BinaryValue,
    node::ApiSender,
    proto::Any,
    runtime::{
        api::ServiceApiBuilder,
        dispatcher::{Dispatcher, DispatcherSender},
        ArtifactId, ArtifactProtobufSpec, CallInfo, ErrorKind, ExecutionContext, ExecutionError,
        InstanceDescriptor, InstanceId, InstanceSpec, Runtime, RuntimeIdentifier,
        StateHashAggregator,
    },
};
use exonum_merkledb::{Fork, Snapshot};
use futures::{Future, IntoFuture};
use jni::{
    objects::{GlobalRef, JObject, JValue},
    signature::JavaType,
    Executor,
};
use proto;
use proxy::node::NodeContext;
use semver::Version;
use std::collections::HashMap;
use std::collections::HashSet;
use std::fmt;
use std::str::FromStr;
use storage::View;
use to_handle;
use utils::{jni_cache::runtime_adapter, panic_on_exception, unwrap_jni};
use Handle;
use JniErrorKind;
use JniResult;

/// A proxy for `ServiceRuntimeAdapter`s.
#[derive(Clone)]
pub struct JavaRuntimeProxy {
    exec: Executor,
    runtime_adapter: GlobalRef,
    deployed_artifacts: HashSet<JavaArtifactId>,
    started_services: HashMap<InstanceId, Instance>,
    started_services_by_name: HashMap<String, InstanceId>,
}

/// Service identification properties within `JavaRuntimeProxy`
#[derive(Debug, Clone)]
struct Instance {
    id: InstanceId,
    name: String,
}

/// Artifact identification properties within `JavaRuntimeProxy`
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct JavaArtifactId {
    /// `groupId` maven property
    pub group: String,
    /// `artifactId` maven property
    pub artifact: String,
    /// `version` maven property in format {major.}{minor}.{patch}
    pub version: Version,
}

struct AfterCommitContext<'a> {
    dispatcher: &'a DispatcherSender,
    snapshot: &'a dyn Snapshot,
    service_keypair: &'a (PublicKey, SecretKey),
    tx_sender: &'a ApiSender,
}

/// List of possible Java runtime errors.
#[derive(Debug, Copy, Clone)]
#[repr(u8)]
pub enum Error {
    /// Unable to parse artifact identifier or specified artifact has non-empty spec.
    IncorrectArtifactId = 0,
    /// Artifact already deployed
    AlreadyDeployed = 1,
    /// Service already started
    AlreadyStarted = 2,
    /// Checked java exception is occurred
    JavaException = 3,
    /// Unspecified error
    UnspecifiedError = 4,
    /// Not supported operation
    NotSupportedOperation = 5,
}

#[derive(Serialize, Deserialize, Clone, ProtobufConvert, PartialEq)]
#[exonum(pb = "proto::ServiceStateHashes")]
struct ServiceStateHashes {
    instance_id: u32,
    state_hashes: Vec<Vec<u8>>,
}

#[derive(Serialize, Deserialize, Clone, ProtobufConvert, PartialEq)]
#[exonum(pb = "proto::ServiceRuntimeStateHashes")]
struct ServiceRuntimeStateHashes {
    runtime_state_hashes: Vec<Vec<u8>>,
    service_state_hashes: Vec<ServiceStateHashes>,
}

impl JavaRuntimeProxy {
    /// Runtime Identifier
    pub const RUNTIME_ID: RuntimeIdentifier = RuntimeIdentifier::Java;

    /// Creates new `JavaRuntimeProxy` for given `ServiceRuntimeAdapter` object
    pub fn new(executor: Executor, adapter: GlobalRef) -> Self {
        JavaRuntimeProxy {
            exec: executor,
            runtime_adapter: adapter,
            deployed_artifacts: HashSet::new(),
            started_services: HashMap::new(),
            started_services_by_name: HashMap::new(),
        }
    }

    fn parse_artifact(&self, artifact: &ArtifactId) -> Result<JavaArtifactId, ExecutionError> {
        if artifact.runtime_id != Self::RUNTIME_ID as u32 {
            Err(Error::IncorrectArtifactId.into())
        } else {
            artifact
                .name
                .parse()
                .map_err(|_| Error::IncorrectArtifactId.into())
        }
    }

    fn can_deploy_artifact(&self, id: &JavaArtifactId) -> Result<(), ExecutionError> {
        if self.deployed_artifacts.contains(id) {
            Err(Error::AlreadyDeployed.into())
        } else {
            Ok(())
        }
    }

    fn add_deployed_artifact(&mut self, id: JavaArtifactId) {
        self.deployed_artifacts.insert(id);
    }

    fn can_start_service(&self, instance: &Instance) -> Result<(), ExecutionError> {
        if self.started_services.contains_key(&instance.id) {
            Err(Error::AlreadyStarted.into())
        } else {
            Ok(())
        }
    }

    fn add_started_service(&mut self, instance: Instance) {
        self.started_services_by_name
            .insert(instance.name.clone(), instance.id);
        self.started_services.insert(instance.id, instance);
    }

    fn remove_started_service(&mut self, id: &InstanceId) {
        let service = self.started_services.remove(id);
        if let Some(instance) = service {
            self.started_services_by_name.remove(&instance.name);
        }
    }

    fn parse_jni<T>(res: JniResult<T>) -> Result<T, ExecutionError> {
        res.map_err(|err| match err.0 {
            JniErrorKind::JavaException => Error::JavaException.into(),
            _ => Error::UnspecifiedError.into(),
        })
    }
}

impl fmt::Debug for JavaRuntimeProxy {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "JavaRuntimeProxy()")
    }
}

impl From<Error> for ExecutionError {
    fn from(value: Error) -> ExecutionError {
        ExecutionError::new(
            ErrorKind::runtime(value as u8),
            format!("{:?}", value.clone()),
        )
    }
}

impl Runtime for JavaRuntimeProxy {
    fn deploy_artifact(
        &mut self,
        artifact: ArtifactId,
        deploy_spec: Any,
    ) -> Box<dyn Future<Item = (), Error = ExecutionError>> {

        let id = match self.parse_artifact(&artifact) {
            Ok(id) => id,
            Err(err) => return Box::new(Err(err).into_future()),
        };

        if let Err(err) = self.can_deploy_artifact(&id) {
            return Box::new(Err(err).into_future());
        }

        Box::new(Self::parse_jni(self.exec.with_attached(|env| {
            let artifact_id = JObject::from(env.new_string(id.to_string())?);
            let spec = JObject::from(env.byte_array_from_slice(&deploy_spec.into_bytes())?);

            env.call_method_unchecked(
                self.runtime_adapter.as_obj(),
                runtime_adapter::deploy_artifact_id(),
                JavaType::from_str("V").unwrap(),
                &[
                    JValue::from(artifact_id),
                    JValue::from(spec),
                ],
            )?;
            Ok(())
        })).map(|result| {
            self.add_deployed_artifact(id);
            result
        })
        .into_future())
    }

    fn artifact_protobuf_spec(&self, id: &ArtifactId) -> Option<ArtifactProtobufSpec> {
        let id = self.parse_artifact(id).ok()?;

        if self.deployed_artifacts.contains(&id) {
            // TODO: call `ServiceRuntimeAdapter`
            Some(ArtifactProtobufSpec::default())
        } else {
            None
        }
    }

    fn start_service(&mut self, spec: &InstanceSpec) -> Result<(), ExecutionError> {
        let adapter = self.runtime_adapter.as_obj().clone();
        let service_name = spec.name.clone();
        let id = spec.id;
        let artifact = self.parse_artifact(&spec.artifact)?;

        let instance = Instance::new(spec.id, spec.name.clone());
        self.can_start_service(&instance)?;

        Self::parse_jni(self.exec.with_attached(|env| {
            let name = JObject::from(env.new_string(service_name)?);
            let artifact_id = JObject::from(env.new_string(artifact.to_string())?);

            env.call_method_unchecked(
                adapter,
                runtime_adapter::create_service_id(),
                JavaType::from_str("V").unwrap(),
                &[
                    JValue::from(name),
                    JValue::from(id as i32),
                    JValue::from(artifact_id),
                ],
            )
            .map(|_| ())
        }))?;

        self.add_started_service(instance);
        Ok(())
    }

    fn configure_service(
        &self,
        fork: &Fork,
        descriptor: InstanceDescriptor,
        parameters: Any,
    ) -> Result<(), ExecutionError> {

        Self::parse_jni(self.exec.with_attached(|env| {
            let id = descriptor.id as i32;
            let view_handle = to_handle(View::from_ref_fork(fork));
            let params = JObject::from(env.byte_array_from_slice(&parameters.into_bytes())?);

            env.call_method_unchecked(
                self.runtime_adapter.as_obj(),
                runtime_adapter::configure_service_id(),
                JavaType::from_str("V").unwrap(),
                &[
                    JValue::from(id),
                    JValue::from(view_handle),
                    JValue::from(params),
                ],
            )?;

            Ok(())
        }))
    }

    fn stop_service(&mut self, descriptor: InstanceDescriptor) -> Result<(), ExecutionError> {
        let adapter = self.runtime_adapter.as_obj();

        Self::parse_jni(self.exec.with_attached(|env| {
            let id = descriptor.id as i32;

            env.call_method_unchecked(
                adapter,
                runtime_adapter::stop_service_id(),
                JavaType::from_str("V").unwrap(),
                &[
                    JValue::from(id),
                ],
            )
            .map(|_| ())
        }))?;

        self.remove_started_service(&descriptor.id);
        Ok(())
    }

    fn execute(
        &self,
        _dispatcher: &Dispatcher,
        context: &mut ExecutionContext,
        call_info: CallInfo,
        arguments: &[u8],
    ) -> Result<(), ExecutionError> {
        let tx = if let (Some(key), Some(hash)) =
            (context.caller.author(), context.caller.transaction_hash())
        {
            (key, hash)
        } else {
            // TODO: caller is Blockchain (not Transaction) is not supported  yet
            return Err(Error::NotSupportedOperation.into());
        };

        Self::parse_jni(self.exec.with_attached(|env| {
            let service_id = call_info.instance_id as i32;
            let tx_id = call_info.method_id as i32;
            let args = JObject::from(env.byte_array_from_slice(arguments)?);
            let view_handle = to_handle(View::from_ref_fork(context.fork));
            let pub_key = JObject::from(env.byte_array_from_slice(&tx.0.to_bytes())?);
            let hash = JObject::from(env.byte_array_from_slice(&tx.1.to_bytes())?);

            env.call_method_unchecked(
                self.runtime_adapter.as_obj(),
                runtime_adapter::execute_tx_id(),
                JavaType::from_str("V").unwrap(),
                &[
                    JValue::from(service_id),
                    JValue::from(tx_id),
                    JValue::from(args),
                    JValue::from(view_handle),
                    JValue::from(pub_key),
                    JValue::from(hash),
                ],
            )?;
            Ok(())
        }))
    }

    fn state_hashes(&self, snapshot: &Snapshot) -> StateHashAggregator {
        let bytes = unwrap_jni(self.exec.with_attached(|env| {
            let view_handle = to_handle(View::from_ref_snapshot(snapshot));
            let java_runtime_hashes = panic_on_exception(
                env,
                env.call_method_unchecked(
                    self.runtime_adapter.as_obj(),
                    runtime_adapter::state_hashes_id(),
                    JavaType::from_str("[B").unwrap(),
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

    fn before_commit(&self, _dispatcher: &Dispatcher, _fork: &mut Fork) {
        // TODO: is not supported by ServiceRuntimeAdapter
    }

    fn after_commit(
        &self,
        dispatcher: &DispatcherSender,
        snapshot: &Snapshot,
        service_keypair: &(PublicKey, SecretKey),
        tx_sender: &ApiSender,
    ) {
        let context = AfterCommitContext::new(dispatcher, snapshot, service_keypair, tx_sender);

        unwrap_jni(self.exec.with_attached(|env| {
            let view_handle = context.view_handle();
            let validator_id = context.validator_id_or(-1);
            let height = context.height().0 as i64;

            panic_on_exception(
                env,
                env.call_method_unchecked(
                    self.runtime_adapter.as_obj(),
                    runtime_adapter::after_commit_id(),
                    JavaType::from_str("V").unwrap(),
                    &[
                        JValue::from(view_handle),
                        JValue::from(validator_id),
                        JValue::from(height),
                    ],
                ),
            );
            Ok(())
        }));
    }

    fn api_endpoints(&self, context: &ApiContext) -> Vec<(String, ServiceApiBuilder)> {
        let started_ids: Vec<i32> = self
            .started_services
            .values()
            .map(|instance| instance.id as i32)
            .collect();
        let node = NodeContext::new(self.exec.clone(), context.clone());

        unwrap_jni(self.exec.with_attached(|env| {
            let node_handle = to_handle(node);
            let ids_array = env.new_int_array(started_ids.capacity() as i32)?;
            env.set_int_array_region(ids_array, 0, &started_ids)?;
            let service_ids = JObject::from(ids_array);

            panic_on_exception(
                env,
                env.call_method_unchecked(
                    self.runtime_adapter.as_obj(),
                    runtime_adapter::connect_apis_id(),
                    JavaType::from_str("V").unwrap(),
                    &[
                        JValue::from(service_ids),
                        JValue::from(node_handle),
                    ],
                ),
            );
            Ok(())
        }));

        self.started_services
            .values()
            .map(|instance| {
                let builder = ServiceApiBuilder::new(
                    context.clone(),
                    InstanceDescriptor {
                        id: instance.id,
                        name: instance.name.as_ref(),
                    },
                );
                (["services/", &instance.name].concat(), builder)
            })
            .collect()
    }
}

impl Instance {
    fn new(id: InstanceId, name: String) -> Self {
        Self { id, name }
    }
}

impl JavaArtifactId {
    /// Creates new artifact description
    pub fn new(group: &str, artifact: &str, major: u64, minor: u64, patch: u64) -> Self {
        Self {
            group: group.to_owned(),
            artifact: artifact.to_owned(),
            version: Version::new(major, minor, patch),
        }
    }
}

impl From<JavaArtifactId> for ArtifactId {
    fn from(inner: JavaArtifactId) -> Self {
        Self {
            runtime_id: JavaRuntimeProxy::RUNTIME_ID as u32,
            name: inner.to_string(),
        }
    }
}

impl fmt::Display for JavaArtifactId {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "{}:{}:{}", self.group, self.artifact, self.version)
    }
}

impl FromStr for JavaArtifactId {
    type Err = failure::Error;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        let split = s.split(':').take(3).collect::<Vec<_>>();
        match &split[..] {
            [group, artifact, version] => {
                Ok(Self {
                    group: group.to_string(),
                    artifact: artifact.to_string(),
                    version: Version::parse(version)?,
                })
            },
            _ => Err(failure::format_err!("Wrong java artifact name format, it should be in form \"groupId:artifactId:version\""))
        }
    }
}

impl<'a> AfterCommitContext<'a> {
    /// Create context for the `after_commit` method.
    pub(crate) fn new(
        dispatcher: &'a DispatcherSender,
        snapshot: &'a dyn Snapshot,
        service_keypair: &'a (PublicKey, SecretKey),
        tx_sender: &'a ApiSender,
    ) -> Self {
        Self {
            dispatcher,
            snapshot,
            service_keypair,
            tx_sender,
        }
    }

    /// Returns the current database snapshot. This snapshot is used to
    /// retrieve schema information from the database.
    pub fn snapshot(&self) -> &dyn Snapshot {
        self.snapshot
    }

    /// If the current node is a validator, return its identifier, for other nodes return `None`.
    pub fn validator_id(&self) -> Option<ValidatorId> {
        CoreSchema::new(self.snapshot)
            .actual_configuration()
            .validator_keys
            .iter()
            .position(|validator| self.service_keypair.0 == validator.service_key)
            .map(|id| ValidatorId(id as u16))
    }

    /// If the current node is a validator, return its identifier, for other nodes return `default`.
    pub fn validator_id_or(&self, default: i32) -> i32 {
        self.validator_id().map_or(default, |id| i32::from(id.0))
    }

    /// Returns the public key of the current node.
    pub fn public_key(&self) -> &PublicKey {
        &self.service_keypair.0
    }

    /// Returns the secret key of the current node.
    pub fn secret_key(&self) -> &SecretKey {
        &self.service_keypair.1
    }

    /// Returns the current blockchain height. This height is "height of the last committed block".
    pub fn height(&self) -> Height {
        CoreSchema::new(self.snapshot).height()
    }

    /// Returns reference to communication channel with dispatcher.
    pub(crate) fn dispatcher_channel(&self) -> &DispatcherSender {
        self.dispatcher
    }

    /// Returns a transaction broadcaster.
    pub fn transaction_broadcaster(&self) -> ApiSender {
        self.tx_sender.clone()
    }

    pub fn view_handle(&self) -> Handle {
        to_handle(View::from_ref_snapshot(self.snapshot))
    }
}

impl From<&ServiceStateHashes> for (InstanceId, Vec<Hash>) {
    fn from(value: &ServiceStateHashes) -> Self {
        let hashes: Vec<Hash> = value
            .state_hashes
            .iter()
            .map(|bytes| Hash::from_bytes(bytes.into()).unwrap())
            .collect();
        (value.instance_id, hashes)
    }
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