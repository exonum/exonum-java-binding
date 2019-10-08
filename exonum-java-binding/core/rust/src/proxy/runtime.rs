use exonum::crypto::{PublicKey, SecretKey};
use exonum::exonum_merkledb::{Fork, Snapshot};
use exonum::node::ApiSender;
use exonum::runtime::dispatcher::{DispatcherRef, DispatcherSender};
use exonum::runtime::{
    ArtifactId, ArtifactProtobufSpec, CallInfo, ExecutionContext, ExecutionError,
    InstanceDescriptor, InstanceSpec, Runtime, StateHashAggregator,
};
use futures::Future;
use jni::objects::GlobalRef;
use jni::Executor;

/// Java Runtime stub
#[derive(Debug)]
pub struct JavaRuntimeProxy;

impl JavaRuntimeProxy {
    /// Create new stub
    pub fn new(_executor: Executor, _adapter: GlobalRef) -> Self {
        Self {}
    }
}

impl Runtime for JavaRuntimeProxy {
    fn deploy_artifact(
        &mut self,
        _artifact: ArtifactId,
        _deploy_spec: Vec<u8>,
    ) -> Box<Future<Item = (), Error = ExecutionError>> {
        unimplemented!()
    }

    fn is_artifact_deployed(&self, _id: &ArtifactId) -> bool {
        unimplemented!()
    }

    fn artifact_protobuf_spec(&self, _id: &ArtifactId) -> Option<ArtifactProtobufSpec> {
        unimplemented!()
    }

    fn start_service(&mut self, _spec: &InstanceSpec) -> Result<(), ExecutionError> {
        unimplemented!()
    }

    fn initialize_service(
        &self,
        _fork: &Fork,
        _instance: InstanceDescriptor,
        _parameters: Vec<u8>,
    ) -> Result<(), ExecutionError> {
        unimplemented!()
    }

    fn stop_service(&mut self, _descriptor: InstanceDescriptor) -> Result<(), ExecutionError> {
        unimplemented!()
    }

    fn execute(
        &self,
        _context: &ExecutionContext,
        _call_info: &CallInfo,
        _arguments: &[u8],
    ) -> Result<(), ExecutionError> {
        unimplemented!()
    }

    fn state_hashes(&self, _snapshot: &Snapshot) -> StateHashAggregator {
        unimplemented!()
    }

    fn before_commit(&self, _dispatcher: &DispatcherRef, _fork: &mut Fork) {
        unimplemented!()
    }

    fn after_commit(
        &self,
        _dispatcher: &DispatcherSender,
        _snapshot: &Snapshot,
        _service_keypair: &(PublicKey, SecretKey),
        _tx_sender: &ApiSender,
    ) {
        unimplemented!()
    }
}
