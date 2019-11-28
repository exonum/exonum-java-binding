// Copyright 2019 The Exonum Team
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

use java_bindings::{
    exonum::crypto::Hash,
    exonum_merkledb::{proof_map_index::ProofMapIndex, Entry, IndexAccess},
    jni::objects::{JObject, JValue},
    utils::{panic_on_exception, unwrap_jni},
    Executor,
};
use tempfile::{self, TempPath};

pub const TEST_SERVICE_NAME: &str = "experimentalTestService";
pub const INITIAL_ENTRY_KEY: &str = "initial key";
pub const INITIAL_ENTRY_VALUE: &str = "initial value";
pub const BEFORE_COMMIT_ENTRY_KEY: &str = "bc key";
pub const INIT_MAP_NAME: &str = "init_map";
pub const BEFORE_COMMIT_MAP_NAME: &str = "before_commit_map";
pub const TX_ENTRY_NAME: &str = "test_entry";

pub const SET_ENTRY_TX: u32 = 1;
pub const THROW_SOE_TX: u32 = 2;
pub const SRVC_ERR_ON_EXEC_TX: u32 = 3;
pub const FAIL_ON_EXEC_TX: u32 = 4;

const NATIVE_FACADE_CLASS: &str = "com/exonum/binding/fakes/NativeFacade";

/// Creates valid service artifact.
pub fn create_service_artifact_valid(
    executor: &Executor,
    artifact_id: &str,
    artifact_version: &str,
) -> TempPath {
    create_service_artifact(
        executor,
        "createValidServiceArtifact",
        artifact_id,
        artifact_version,
    )
}

/// Creates service artifact that fails loading.
pub fn create_service_artifact_non_loadable(
    executor: &Executor,
    artifact_id: &str,
    artifact_version: &str,
) -> TempPath {
    create_service_artifact(
        executor,
        "createUnloadableServiceArtifact",
        artifact_id,
        artifact_version,
    )
}

/// Creates service artifact that provides service that is not possible to instantiate.
pub fn create_service_artifact_non_instantiable_service(
    executor: &Executor,
    artifact_id: &str,
    artifact_version: &str,
) -> TempPath {
    create_service_artifact(
        executor,
        "createServiceArtifactWithNonInstantiableService",
        artifact_id,
        artifact_version,
    )
}

// Does the actual communication with the Java part.
fn create_service_artifact(
    executor: &Executor,
    method_name: &str,
    artifact_id: &str,
    artifact_version: &str,
) -> TempPath {
    unwrap_jni(executor.with_attached(|env| {
        let name = artifact_id.to_string().replace(":", "_");
        let artifact_path = tempfile::Builder::new()
            .prefix(&name)
            .suffix(".jar")
            .tempfile()
            .unwrap()
            .into_temp_path();

        let artifact_path_obj: JObject = env.new_string(artifact_path.to_str().unwrap())?.into();
        let artifact_id_obj: JObject = env.new_string(artifact_id)?.into();
        let artifact_version_obj: JObject = env.new_string(artifact_version)?.into();
        panic_on_exception(
            env,
            env.call_static_method(
                NATIVE_FACADE_CLASS,
                method_name,
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                &[
                    JValue::from(artifact_id_obj),
                    JValue::from(artifact_version_obj),
                    JValue::from(artifact_path_obj),
                ],
            )?
            .v(),
        );
        Ok(artifact_path)
    }))
}

pub fn create_init_service_test_map<V>(view: V, service_name: &str) -> ProofMapIndex<V, Hash, String>
where
    V: IndexAccess,
{
    ProofMapIndex::new(format!("{}_{}", service_name, INIT_MAP_NAME), view)
}

pub fn create_before_commit_test_map<V>(
    view: V,
    service_name: &str,
) -> ProofMapIndex<V, Hash, String>
where
    V: IndexAccess,
{
    ProofMapIndex::new(format!("{}_{}", service_name, BEFORE_COMMIT_MAP_NAME), view)
}

pub fn create_tx_test_entry<V>(view: V, _service_name: &str) -> Entry<V, String>
where
    V: IndexAccess,
{
    Entry::new(format!("{}", TX_ENTRY_NAME), view)
}
