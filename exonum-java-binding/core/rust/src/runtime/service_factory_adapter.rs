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

use exonum::{
    blockchain::Service,
    helpers::fabric::{Command, CommandExtension, Context, ServiceFactory},
};
use runtime::{
    cmd::Run,
    config::{Config, InternalConfig},
    java_service_runtime::JavaServiceRuntime,
    utils::{absolute_library_path, system_classpath},
};
use std::sync::{Arc, Once, ONCE_INIT};

static mut JAVA_SERVICE_RUNTIME: Option<JavaServiceRuntime> = None;
static JAVA_SERVICE_RUNTIME_INIT: Once = ONCE_INIT;
static FIRST_INSTANCE_CREATED: Once = ONCE_INIT;

/// Adapts current single-service interface of Exonum to multiple EJB services until dynamic
/// services are implemented. Tracks the `JavaServiceRuntime` instantiation and command extension
/// and represents factory for every user service relying on current `ServiceFactory` interface.
pub struct JavaServiceFactoryAdapter {
    name: String,
    artifact_path: String,
    // Indicates whether this instance was created before others.
    is_first_instance_created: bool,
}

impl JavaServiceFactoryAdapter {
    /// Creates new instance with given service name and path to the artifact.
    pub fn new(name: String, artifact_path: String) -> Self {
        JavaServiceFactoryAdapter {
            name,
            artifact_path,
            is_first_instance_created: is_first_instance_created(),
        }
    }

    // Creates new runtime from provided config or returns the one created earlier. There can be
    // only one `JavaServiceRuntime` instance at a time.
    fn get_or_create_java_service_runtime(config: Config) -> JavaServiceRuntime {
        // Initialize runtime if it wasn't created before.
        JAVA_SERVICE_RUNTIME_INIT.call_once(|| {
            let internal_config = InternalConfig {
                system_class_path: system_classpath(),
                system_lib_path: Some(absolute_library_path()),
            };

            let vm = JavaServiceRuntime::create_java_vm(
                &config.jvm_config,
                &config.runtime_config,
                internal_config,
            );
            let runtime = JavaServiceRuntime::new(Arc::new(vm), config.runtime_config.port);
            unsafe {
                JAVA_SERVICE_RUNTIME = Some(runtime);
            }
        });

        unsafe { JAVA_SERVICE_RUNTIME.clone() }
            .expect("Trying to return runtime, but it's uninitialized")
    }
}

impl ServiceFactory for JavaServiceFactoryAdapter {
    fn service_name(&self) -> &str {
        &self.name
    }

    fn command(&mut self, command_name: &str) -> Option<Box<dyn CommandExtension>> {
        use exonum::helpers::fabric;
        // Execute EJB configuration steps along with standard Exonum Core steps.
        // We extend the `Run` command only AND only the first created instance of
        // JavaServiceFactoryAdapter is allowed to extend the command.
        if command_name == fabric::Run.name() && self.is_first_instance_created {
            return Some(Box::new(Run));
        }
        None
    }

    fn make_service(&mut self, context: &Context) -> Box<dyn Service> {
        let runtime = Self::get_or_create_java_service_runtime(extract_config(context));

        // load service from artifact and create corresponding proxy
        let artifact_id = runtime.load_artifact(&self.artifact_path);
        let service_proxy = runtime.create_service(&artifact_id);
        Box::new(service_proxy)
    }
}

// Returns the real command extension for the first call and `None` for any other call.
fn is_first_instance_created() -> bool {
    let mut is_first = false;
    FIRST_INSTANCE_CREATED.call_once(|| is_first = true);
    is_first
}

// Extracts EJB and JVM configuration from Context
fn extract_config(context: &Context) -> Config {
    use exonum::helpers::fabric::keys;
    context
        .get(keys::NODE_CONFIG)
        .expect("Unable to read node configuration.")
        .services_configs
        .get(super::cmd::EJB_CONFIG_SECTION_NAME)
        .expect("Unable to read EJB configuration.")
        .clone()
        .try_into()
        .expect("Invalid EJB configuration format.")
}
