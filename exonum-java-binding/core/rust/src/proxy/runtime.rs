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

#[derive(Debug)]
pub struct JavaRuntimeProxy;

impl JavaRuntimeProxy {
    pub fn new(_executor: Executor, _adapter: GlobalRef) {}
}

impl Runtime for JavaRuntimeProxy {
    fn deploy_artifact(
        &mut self,
        artifact: ArtifactId,
        deploy_spec: Vec<u8>,
    ) -> Box<Future<Item = (), Error = ExecutionError>> {
        unimplemented!()
    }

    fn is_artifact_deployed(&self, id: &ArtifactId) -> bool {
        unimplemented!()
    }

    fn artifact_protobuf_spec(&self, id: &ArtifactId) -> Option<ArtifactProtobufSpec> {
        unimplemented!()
    }

    fn start_service(&mut self, spec: &InstanceSpec) -> Result<(), ExecutionError> {
        unimplemented!()
    }

    fn initialize_service(
        &self,
        fork: &Fork,
        instance: InstanceDescriptor,
        parameters: Vec<u8>,
    ) -> Result<(), ExecutionError> {
        unimplemented!()
    }

    fn stop_service(&mut self, descriptor: InstanceDescriptor) -> Result<(), ExecutionError> {
        unimplemented!()
    }

    fn execute(
        &self,
        context: &ExecutionContext,
        call_info: &CallInfo,
        arguments: &[u8],
    ) -> Result<(), ExecutionError> {
        unimplemented!()
    }

    fn state_hashes(&self, snapshot: &Snapshot) -> StateHashAggregator {
        unimplemented!()
    }

    fn before_commit(&self, dispatcher: &DispatcherRef, fork: &mut Fork) {
        unimplemented!()
    }

    fn after_commit(
        &self,
        dispatcher: &DispatcherSender,
        snapshot: &Snapshot,
        service_keypair: &(PublicKey, SecretKey),
        tx_sender: &ApiSender,
    ) {
        unimplemented!()
    }
}
