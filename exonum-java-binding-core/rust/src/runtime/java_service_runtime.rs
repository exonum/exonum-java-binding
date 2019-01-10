use exonum::blockchain::Service;
use exonum::helpers::fabric::{Command, CommandExtension, Context, ServiceFactory};
use jni::{self, InitArgs, InitArgsBuilder, JavaVM, Result};

use std::env;
use std::sync::{Arc, Once, ONCE_INIT};

use proxy::{JniExecutor, ServiceProxy};
use runtime::cmd::{Finalize, GenerateNodeConfig, Run};
use runtime::config::{self, Config, EjbConfig, JvmConfig, ServiceConfig};
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
                let java_vm = Self::create_java_vm(config.jvm_config, config.ejb_config);
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
    fn create_java_vm(jvm_config: JvmConfig, ejb_config: EjbConfig) -> JavaVM {
        let args = Self::build_jvm_arguments(jvm_config, ejb_config)
            .expect("Unable to build arguments for JVM");
        jni::JavaVM::new(args).unwrap()
    }

    /// Builds arguments for JVM initialization.
    fn build_jvm_arguments(jvm_config: JvmConfig, ejb_config: EjbConfig) -> Result<InitArgs> {
        let mut args_builder = jni::InitArgsBuilder::new().version(jni::JNIVersion::V8);

        let args_prepend = jvm_config.args_prepend.clone();
        let args_append = jvm_config.args_append.clone();

        // Prepend extra user arguments
        args_builder = Self::add_user_arguments(args_builder, args_prepend);

        // Add required arguments
        args_builder = Self::add_required_arguments(args_builder, ejb_config);

        // Add optional arguments
        args_builder = Self::add_optional_arguments(args_builder, jvm_config);

        // Append extra user arguments
        args_builder = Self::add_user_arguments(args_builder, args_append);

        args_builder.build()
    }

    /// Adds extra user arguments (optional) to JVM configuration
    fn add_user_arguments<I>(mut args_builder: InitArgsBuilder, user_args: I) -> InitArgsBuilder
    where
        I: IntoIterator<Item = String>,
    {
        for param in user_args {
            let option = config::validate_and_convert(&param).unwrap();
            args_builder = args_builder.option(&option);
        }
        args_builder
    }

    /// Adds required EJB-related arguments to JVM configuration
    fn add_required_arguments(
        args_builder: InitArgsBuilder,
        ejb_config: EjbConfig,
    ) -> InitArgsBuilder {
        args_builder
            .option(&format!("-Djava.class.path={}", ejb_config.class_path))
            .option(&format!("-Djava.library.path={}", ejb_config.lib_path))
            .option(&format!(
                "-Dlog4j.configurationFile={}",
                ejb_config.log_config_path
            ))
    }

    /// Adds optional user arguments to JVM configuration
    fn add_optional_arguments(
        mut args_builder: InitArgsBuilder,
        jvm_config: JvmConfig,
    ) -> InitArgsBuilder {
        if let Some(socket) = jvm_config.jvm_debug_socket {
            args_builder = args_builder.option(&format!(
                "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address={}",
                socket
            ));
        }
        args_builder
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

    fn command(&mut self, command: &str) -> Option<Box<CommandExtension>> {
        use exonum::helpers::fabric;
        // Execute EJB configuration steps along with standard Exonum Core steps.
        match command {
            v if v == fabric::GenerateNodeConfig.name() => Some(Box::new(GenerateNodeConfig)),
            v if v == fabric::Finalize.name() => Some(Box::new(Finalize)),
            v if v == fabric::Run.name() => Some(Box::new(Run)),
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
                .get(super::cmd::EJB_CONFIG_SECTION_NAME)
                .expect("Unable to read EJB configuration.")
                .clone()
                .try_into()
                .expect("Invalid EJB configuration format.");
            JavaServiceRuntime::get_or_create(config)
        };

        Box::new(runtime.service_proxy().clone())
    }
}
