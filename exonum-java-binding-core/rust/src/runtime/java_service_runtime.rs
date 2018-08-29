use exonum::blockchain::Service;
use exonum::helpers::fabric::{CommandExtension, Context, ServiceFactory};
use jni::{self, JavaVM};

use std::env;
use std::fs;
use std::sync::{Arc, Once, ONCE_INIT};

use proxy::{JniExecutor, ServiceProxy};
use runtime::cmd::{Finalize, GenerateTemplate, Run};
use runtime::config::{self, Config, InternalConfig, PrivateConfig};
use utils::{executable_directory, join_paths, unwrap_jni};
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
    pub fn get_or_create(config: Config, internal: InternalConfig) -> Self {
        unsafe {
            // Initialize runtime if it wasn't created before.
            JAVA_SERVICE_RUNTIME_INIT.call_once(|| {
                let java_vm = Self::create_java_vm(&config.private_config, internal);
                let executor = MainExecutor::new(Arc::new(java_vm));
                let service_proxy = Self::create_service(
                    &config.public_config.module_name,
                    config.private_config.port,
                    executor.clone(),
                );
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
    fn create_java_vm(config: &PrivateConfig, internal_config: InternalConfig) -> JavaVM {
        let mut args_builder = jni::InitArgsBuilder::new().version(jni::JNIVersion::V8);

        for param in &config.user_parameters {
            let option = config::validate_and_convert(param).unwrap();
            args_builder = args_builder.option(&option);
        }

        let class_path = join_paths(&[
            &internal_config.system_class_path,
            &config.service_class_path,
        ]);

        args_builder = args_builder.option(&format!("-Djava.class.path={}", class_path));
        if internal_config.system_lib_path.is_some() {
            args_builder = args_builder.option(&format!(
                "-Djava.library.path={}",
                internal_config.system_lib_path.unwrap()
            ));
        }
        args_builder = args_builder.option(&format!(
            "-Dlog4j.configurationFile={}",
            config.log_config_path
        ));

        let args = args_builder.build().unwrap();
        jni::JavaVM::new(args).unwrap()
    }

    /// Creates service proxy for interaction with Java side.
    fn create_service(module_name: &str, port: i32, executor: MainExecutor) -> ServiceProxy {
        let service = unwrap_jni(executor.with_attached(|env| {
            let module_name = env.new_string(module_name).unwrap();
            let module_name: jni::objects::JObject = *module_name;
            let service = env
                .call_static_method(
                    SERVICE_BOOTSTRAP_PATH,
                    "startService",
                    START_SERVICE_SIGNATURE,
                    &[module_name.into(), port.into()],
                )?
                .l()?;
            env.new_global_ref(service)
        }));
        ServiceProxy::from_global_ref(executor, service)
    }
}

/// Returns path to <ejb-app location>/lib directory in an absolute form.
fn absolute_library_path() -> String {
    let library_path = {
        let mut executable_directory = executable_directory();
        executable_directory.push("lib/native");
        executable_directory
    };
    library_path.to_string_lossy().into_owned()
}

fn system_classpath() -> String {
    let mut jars = Vec::new();
    let jars_directory = {
        let mut executable_directory = executable_directory();
        executable_directory.push("lib/java");
        executable_directory
    };
    for entry in fs::read_dir(jars_directory).expect("Could not read java classes directory") {
        let file = entry.unwrap();
        if file.file_type().unwrap().is_file() {
            jars.push(file.path());
        } else {
            continue;
        }
    }

    let jars = jars.iter().map(|p| p.to_str().unwrap());
    env::join_paths(jars).unwrap().into_string().unwrap()
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
    fn command(&mut self, command: &str) -> Option<Box<CommandExtension>> {
        use exonum::helpers::fabric;
        // Execute EJB configuration steps along with standard Exonum Core steps.
        match command {
            v if v == fabric::GenerateCommonConfig::name() => Some(Box::new(GenerateTemplate)),
            v if v == fabric::Finalize::name() => Some(Box::new(Finalize)),
            v if v == fabric::Run::name() => Some(Box::new(Run)),
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
            let internal_config = InternalConfig {
                system_class_path: system_classpath(),
                system_lib_path: Some(absolute_library_path()),
            };
            JavaServiceRuntime::get_or_create(config, internal_config)
        };

        Box::new(runtime.service_proxy().clone())
    }
}
