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
use runtime::{cmd::Run, config::Config, java_service_runtime::JavaServiceRuntime};
use std::sync::{Once, ONCE_INIT};

static mut JAVA_SERVICE_RUNTIME: Option<JavaServiceRuntime> = None;
static JAVA_SERVICE_RUNTIME_INIT: Once = ONCE_INIT;
static SERVICE_FACTORY_EXTEND_COMMAND: Once = ONCE_INIT;

/// Adapts current single-service interface of Exonum to multiple EJB services until dynamic
/// services are implemented. Tracks the `JavaServiceRuntime` instantiation and command extension
/// and represents factory for every user service relying on current `ServiceFactory` interface.
pub struct JavaServiceFactoryAdapter {
    name: String,
    artifact_path: String,
}

impl JavaServiceFactoryAdapter {
    /// Creates new instance with given service name and path to the artifact.
    pub fn new(name: String, artifact_path: String) -> Self {
        JavaServiceFactoryAdapter {
            name,
            artifact_path,
        }
    }

    // Creates new runtime from provided config or returns the one created earlier. There can be
    // only one `JavaServiceRuntime` instance at a time.
    fn get_or_create_java_service_runtime(config: Config) -> JavaServiceRuntime {
        // Initialize runtime if it wasn't created before.
        JAVA_SERVICE_RUNTIME_INIT.call_once(|| {
            let runtime = JavaServiceRuntime::new(config);
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

    fn command(&mut self, command_name: &str) -> Option<Box<CommandExtension>> {
        use exonum::helpers::fabric;
        // Execute EJB configuration steps along with standard Exonum Core steps.
        // We extend the `Run` command only, any other commands are ignored.
        if command_name == fabric::Run.name() {
            // This callback gets called for every instance of ServiceFactory, but we have to do
            // this extension only once otherwise the underlying `clap` backend will complain about
            // non-unique argument names
            return extend_command_once();
        }
        None
    }

    fn make_service(&mut self, context: &Context) -> Box<Service> {
        let runtime = Self::get_or_create_java_service_runtime(extract_config(context));

        // load service from artifact and create corresponding proxy
        let artifact_id = runtime.load_artifact(&self.artifact_path);
        let service_proxy = runtime.create_service(&artifact_id);
        Box::new(service_proxy)
    }
}

// Returns the real command extension for the first call and `None` for any other call.
fn extend_command_once() -> Option<Box<CommandExtension>> {
    let mut command_ext: Option<Box<CommandExtension>> = None;
    SERVICE_FACTORY_EXTEND_COMMAND.call_once(|| command_ext = Some(Box::new(Run)));
    command_ext
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
