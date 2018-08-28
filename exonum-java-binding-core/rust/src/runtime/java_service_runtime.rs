use exonum::blockchain::Service;
use exonum::helpers::fabric::{self, Command, CommandExtension, CommandName, Context, ServiceFactory};
use jni::{self, JavaVM};

use std::env;
use std::sync::{Arc, Once, ONCE_INIT};

use proxy::{JniExecutor, ServiceProxy};
use runtime::cmd::{Finalize, GenerateNodeConfig};
use runtime::config::{self, Config, JvmConfig, ServiceConfig};
use utils::unwrap_jni;
use MainExecutor;

static mut JAVA_SERVICE_RUNTIME: Option<JavaServiceRuntime> = None;
static JAVA_SERVICE_RUNTIME_INIT: Once = ONCE_INIT;

const SERVICE_BOOTSTRAP_PATH: &str = "com/exonum/binding/service/ServiceBootstrap";
const START_SERVICE_SIGNATURE: &str =
    "(Ljava/lang/String;I)Lcom/exonum/binding/service/adapters/UserServiceAdapter;";

/// Controls JVM and java service.
#[allow(dead_code)]
#[derive(Clone)]
pub struct JavaServiceRuntime {
    executor: MainExecutor,
    service_proxy: ServiceProxy,
}

impl JavaServiceRuntime {
    /// Creates new runtime from provided config or returns the one created earlier.
    ///
    /// There can be only one `JavaServiceRuntime` instance at a time.
    pub fn get_or_create(config: Config) -> Self {
        unsafe {
            // Initialize runtime if it wasn't created before.
            JAVA_SERVICE_RUNTIME_INIT.call_once(|| {
                let java_vm = Self::create_java_vm(config.jvm_config);
                let executor = MainExecutor::new(Arc::new(java_vm));
                let service_proxy = Self::create_service(config.service_config, executor.clone());
                let runtime = JavaServiceRuntime {
                    executor,
                    service_proxy,
                };
                JAVA_SERVICE_RUNTIME = Some(runtime);
            });
            // Return global runtime.
            JAVA_SERVICE_RUNTIME
                .clone()
                .expect("Trying to return runtime, but it's uninitialized")
        }
    }

    /// Returns internal service proxy.
    pub fn service_proxy(&self) -> ServiceProxy {
        self.service_proxy.clone()
    }

    /// Initializes JVM with provided configuration.
    ///
    /// # Panics
    ///
    /// - If user specified invalid additional JVM parameters.
    fn create_java_vm(config: JvmConfig) -> JavaVM {
        let mut args_builder = jni::InitArgsBuilder::new().version(jni::JNIVersion::V8);

        for param in &config.user_parameters {
            let option = config::validate_and_convert(param).unwrap();
            args_builder = args_builder.option(&option);
        }

        args_builder = args_builder.option(&format!("-Djava.class.path={}", config.class_path));
        args_builder = args_builder.option(&format!("-Djava.library.path={}", config.lib_path));
        args_builder = args_builder.option(&format!(
            "-Dlog4j.configurationFile={}",
            config.log_config_path
        ));

        let args = args_builder.build().unwrap();
        jni::JavaVM::new(args).unwrap()
    }

    /// Creates service proxy for interaction with Java side.
    fn create_service(config: ServiceConfig, executor: MainExecutor) -> ServiceProxy {
        let service = unwrap_jni(executor.with_attached(|env| {
            let module_name = env.new_string(config.module_name).unwrap();
            let module_name: jni::objects::JObject = *module_name;
            let service = env
                .call_static_method(
                    SERVICE_BOOTSTRAP_PATH,
                    "startService",
                    START_SERVICE_SIGNATURE,
                    &[module_name.into(), config.port.into()],
                )?
                .l()?;
            env.new_global_ref(service)
        }));
        ServiceProxy::from_global_ref(executor, service)
    }
}

/// Panics if `_JAVA_OPTIONS` environmental variable is set.
pub fn panic_if_java_options() {
    if env::var("_JAVA_OPTIONS").is_ok() {
        panic!(
            "_JAVA_OPTIONS environment variable is set. \
             Due to the fact that it will overwrite any JVM settings, \
             including ones set by EJB internally, this variable is \
             forbidden for EJB applications.\n\
             It is recommended to use `--ejb-jvm-args` command-line \
             parameter for setting custom JVM parameters."
        );
    }
}

/// Factory for particular Java service.
/// Initializes EJB runtime and creates `ServiceProxy`.
pub struct JavaServiceFactory;

impl ServiceFactory for JavaServiceFactory {
    fn service_name(&self) -> &str {
        "JAVA_SERVICE_FACTORY"
    }

    fn command(&mut self, command: CommandName) -> Option<Box<CommandExtension>> {
        // Execute EJB configuration steps along with standard Exonum Core steps.
        match command {
            v if v == fabric::GenerateNodeConfig.name() => Some(Box::new(GenerateNodeConfig)),
            v if v == fabric::Finalize.name() => Some(Box::new(Finalize)),
            _ => None,
        }
    }

    fn make_service(&mut self, context: &Context) -> Box<Service> {
        let runtime = {
            use exonum::helpers::fabric::keys;
            let config: Config = context
                .get(keys::NODE_CONFIG)
                .expect("Unable to read node configuration.")
                .services_configs
                .get(super::cmd::EJB_CONFIG_NAME)
                .expect("Unable to read EJB configuration.")
                .clone()
                .try_into()
                .expect("Invalid EJB configuration format.");
            JavaServiceRuntime::get_or_create(config)
        };

        Box::new(runtime.service_proxy().clone())
    }
}
