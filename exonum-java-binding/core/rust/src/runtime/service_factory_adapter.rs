use exonum::{
    blockchain::Service,
    helpers::fabric::{Command, CommandExtension, Context, ServiceFactory},
};
use runtime::{cmd::Run, config::Config, java_service_runtime::JavaServiceRuntime};
use std::sync::{Once, ONCE_INIT};

static mut JAVA_SERVICE_RUNTIME: Option<JavaServiceRuntime> = None;
static JAVA_SERVICE_RUNTIME_INIT: Once = ONCE_INIT;
static JAVA_SERVICE_CREATE_COMMAND: Once = ONCE_INIT;

/// Adapts current single-service interface of Exonum to multiple EJB services until dynamic
/// services are implemented. Tracks the `JavaServiceRuntime` instantiation and command extension
/// and represents factory for every user service relying on current `ServiceFactory` interface.
pub struct JavaServiceFactoryAdapter {
    name: String,
    artifact_path: String,
}

impl JavaServiceFactoryAdapter {
    /// Creates new instance with given service name and path to the artifact.
    pub fn new<S: AsRef<str>>(name: S, artifact_uri: S) -> Self {
        JavaServiceFactoryAdapter {
            name: name.as_ref().to_owned(),
            artifact_path: artifact_uri.as_ref().to_owned(),
        }
    }

    // Creates new runtime from provided config or returns the one created earlier. There can be
    // only one `JavaServiceRuntime` instance at a time.
    fn get_or_create_java_service_runtime(context: &Context) -> JavaServiceRuntime {
        // Initialize runtime if it wasn't created before.
        JAVA_SERVICE_RUNTIME_INIT.call_once(|| {
            // Prepare config for service runtime configuration
            let config = Self::extract_config(context);

            let runtime = JavaServiceRuntime::create_java_runtime(config);
            unsafe {
                JAVA_SERVICE_RUNTIME = Some(runtime);
            }
        });

        unsafe { JAVA_SERVICE_RUNTIME.clone() }
            .expect("Trying to return runtime, but it's uninitialized")
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
}

impl ServiceFactory for JavaServiceFactoryAdapter {
    fn service_name(&self) -> &str {
        &self.name
    }

    fn command(&mut self, command_name: &str) -> Option<Box<CommandExtension>> {
        use exonum::helpers::fabric;
        // Execute EJB configuration steps along with standard Exonum Core steps.
        let mut command_ext: Option<Box<CommandExtension>> = None;
        if command_name == fabric::Run.name() {
            // We return command extension only once for all instances of ServiceFactory otherwise
            // the `clap` backend will complain about non-unique argument names
            JAVA_SERVICE_CREATE_COMMAND.call_once(|| command_ext = Some(Box::new(Run)));
        }
        command_ext
    }

    fn make_service(&mut self, context: &Context) -> Box<Service> {
        let runtime = Self::get_or_create_java_service_runtime(context);

        // load service from artifact and create corresponding proxy
        let artifact_id = runtime
            .load_artifact(&self.artifact_path)
            .expect("Unable to load artifact");
        let service_proxy = runtime.create_service(&artifact_id);
        Box::new(service_proxy)
    }
}
