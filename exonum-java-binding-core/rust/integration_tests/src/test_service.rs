use java_bindings::{JniExecutor, MainExecutor, ServiceProxy};
use java_bindings::exonum::crypto::Hash;
use java_bindings::exonum::storage::Snapshot;
use java_bindings::exonum::storage::proof_map_index::ProofMapIndex;
use java_bindings::utils::unwrap_jni;

use mock::NATIVE_FACADE_CLASS;
use mock::service::SERVICE_ADAPTER_CLASS;

pub const INITIAL_ENTRY_KEY: &str = "initial key";
pub const INITIAL_ENTRY_VALUE: &str = "initial value";
pub const TEST_MAP_NAME: &str = "test_map";

/// Creates a test service.
pub fn create_test_service(executor: MainExecutor) -> ServiceProxy {
    let test_service = unwrap_jni(executor.with_attached(|env| {
        let test_service = env.call_static_method(
            NATIVE_FACADE_CLASS,
            "createTestService",
            format!("()L{};", SERVICE_ADAPTER_CLASS),
            &[],
        )?
            .l()?;
        env.new_global_ref(env.auto_local(test_service).as_obj())
    }));
    ServiceProxy::from_global_ref(executor, test_service)
}

pub fn create_test_map<V>(view: V, service_name: &str) -> ProofMapIndex<V, Hash, String>
where
    V: AsRef<Snapshot + 'static>,
{
    ProofMapIndex::new(format!("{}_{}", service_name, TEST_MAP_NAME), view)
}
