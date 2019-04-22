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

use super::mock::NATIVE_FACADE_CLASS;
use java_bindings::{
    jni::objects::{JObject, JValue},
    utils::{panic_on_exception, unwrap_jni},
    JniExecutor, MainExecutor,
};
use tempfile::{self, TempPath};

/// Creates valid service artifact.
pub fn create_service_artifact_valid(executor: &MainExecutor) -> TempPath {
    create_service_artifact(executor, "createValidServiceArtifact")
}

/// Creates service artifact that fails loading.
pub fn create_service_artifact_non_loadable(executor: &MainExecutor) -> TempPath {
    create_service_artifact(executor, "createUnloadableServiceArtifact")
}

/// Creates service artifact that provides service that is not possible to instantiate.
pub fn create_service_artifact_non_instantiable_service(executor: &MainExecutor) -> TempPath {
    create_service_artifact(executor, "createServiceArtifactWithNonInstantiableService")
}

// Does the actual communication with the Java part.
fn create_service_artifact(executor: &MainExecutor, method_name: &str) -> TempPath {
    unwrap_jni(executor.with_attached(|env| {
        let artifact_path = tempfile::Builder::new()
            .prefix("artifact")
            .suffix(".jar")
            .tempfile()
            .unwrap()
            .into_temp_path();
        let artifact_path_obj: JObject = env.new_string(artifact_path.to_str().unwrap())?.into();
        panic_on_exception(
            env,
            env.call_static_method(
                NATIVE_FACADE_CLASS,
                method_name,
                "(Ljava/lang/String;)V",
                &[JValue::from(artifact_path_obj)],
            )?
            .v(),
        );
        Ok(artifact_path)
    }))
}
