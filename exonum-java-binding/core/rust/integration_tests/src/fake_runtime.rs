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

use java_bindings::{utils::unwrap_jni, Executor, JavaRuntimeProxy};

const NATIVE_FACADE_CLASS: &str = "com/exonum/binding/fakes/NativeFacade";
const CREATE_FAKE_RUNTIME_ADAPTER_SIGNATURE: &str =
    "()Lcom/exonum/binding/core/runtime/ServiceRuntimeAdapter;";

/// Instantiates JavaRuntimeProxy using provided Executor and runtime configuration parameters.
pub fn create_fake_service_runtime_adapter(executor: Executor, method: &str) -> JavaRuntimeProxy {
    let runtime_adapter = unwrap_jni(executor.with_attached(|env| {
        let service_runtime = env
            .call_static_method(
                NATIVE_FACADE_CLASS,
                method,
                CREATE_FAKE_RUNTIME_ADAPTER_SIGNATURE,
                &[],
            )?
            .l()?;
        env.new_global_ref(service_runtime)
    }));
    JavaRuntimeProxy::new(executor, runtime_adapter)
}
