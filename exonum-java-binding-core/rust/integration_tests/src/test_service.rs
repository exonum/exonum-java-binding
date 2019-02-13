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

use java_bindings::exonum::crypto::Hash;
use java_bindings::exonum::storage::proof_map_index::ProofMapIndex;
use java_bindings::exonum::storage::Snapshot;
use java_bindings::utils::unwrap_jni;
use java_bindings::{JniExecutor, MainExecutor, ServiceProxy};

use mock::service::SERVICE_ADAPTER_CLASS;
use mock::NATIVE_FACADE_CLASS;

pub const INITIAL_ENTRY_KEY: &str = "initial key";
pub const INITIAL_ENTRY_VALUE: &str = "initial value";
pub const TEST_MAP_NAME: &str = "test_map";

/// Creates a test service.
pub fn create_test_service(executor: MainExecutor) -> ServiceProxy {
    let test_service = unwrap_jni(executor.with_attached(|env| {
        let test_service = env
            .call_static_method(
                NATIVE_FACADE_CLASS,
                "createTestService",
                format!("()L{};", SERVICE_ADAPTER_CLASS),
                &[],
            )?
            .l()?;
        env.new_global_ref(test_service)
    }));
    ServiceProxy::from_global_ref(executor, test_service)
}

pub fn create_test_map<V>(view: V, service_name: &str) -> ProofMapIndex<V, Hash, String>
where
    V: AsRef<Snapshot + 'static>,
{
    ProofMapIndex::new(format!("{}_{}", service_name, TEST_MAP_NAME), view)
}
