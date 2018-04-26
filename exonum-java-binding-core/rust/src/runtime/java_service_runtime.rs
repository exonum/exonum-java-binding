use exonum::blockchain::Service;
use exonum::helpers::fabric::{Context, ServiceFactory};

use jni;

use std::sync::Arc;

use proxy::ServiceProxy;
use runtime::config::{Config, JvmConfig, ServiceConfig};
use runtime::cmd::GenerateNodeConfig;
use utils::unwrap_jni;
use {Executor, DumbExecutor};
use exonum::helpers::fabric::CommandExtension;

const SERVICE_BOOTSTRAP_PATH: &str = "com/exonum/binding/service/ServiceBootstrap";
const START_SERVICE_SIGNATURE: &str =
    "(Ljava/lang/String;I)Lcom/exonum/binding/service/adapters/UserServiceAdapter;";

/// Controls JVM and java service.
#[allow(dead_code)]
#[derive(Clone)]
pub struct JavaServiceRuntime {
    executor: DumbExecutor,
    service_proxy: ServiceProxy<DumbExecutor>,
}

impl JavaServiceRuntime {
    /// Create new runtime from config.
    pub fn new(config: Config) -> Self {
        let executor = Self::create_executor(config.jvm_config);
        let service_proxy = Self::create_service(config.service_config, executor.clone());
        Self {
            executor,
            service_proxy,
        }
    }

    /// Return internal service proxy.
    pub fn service_proxy(&self) -> ServiceProxy<DumbExecutor> {
        self.service_proxy.clone()
    }

    fn create_executor(config: JvmConfig) -> DumbExecutor {
        let java_vm = {
            let mut args_builder = jni::InitArgsBuilder::new()
                .version(jni::JNIVersion::V8);

            if config.debug {
                args_builder = args_builder.option("-Xcheck:jni").option("-Xdebug");
            }

            args_builder = args_builder.option(&format!("-Djava.class.path={}", config.class_path));
            args_builder = args_builder.option(&format!("-Djava.library.path={}", config.lib_path));

            let args = args_builder.build().unwrap();
            jni::JavaVM::new(args).unwrap()
        };
        DumbExecutor { vm: Arc::new(java_vm) }
    }

    fn create_service(config: ServiceConfig, executor: DumbExecutor) -> ServiceProxy<DumbExecutor> {
        let service = unwrap_jni(executor.with_attached(|env| {
            let module_name = env.new_string(config.module_name).unwrap();
            let module_name: jni::objects::JObject = *module_name;
            let service = env.call_static_method(
                SERVICE_BOOTSTRAP_PATH,
                "startService",
                START_SERVICE_SIGNATURE,
                &[module_name.into(), config.port.into()],
            )?.l()?;
            env.new_global_ref(env.auto_local(service).as_obj())
        }));
        ServiceProxy::from_global_ref(executor, service)
    }
}

use std::sync::Mutex;

lazy_static!{
    static ref JAVA_SERVICE_RUNTIME: Mutex<Option<JavaServiceRuntime>> = Mutex::new(None);
}

/// TODO
pub struct JavaServiceFactory;

impl ServiceFactory for JavaServiceFactory {
    fn command(&mut self, command: &str) -> Option<Box<CommandExtension>> {
        use exonum::helpers::fabric;
        match command {
            v if v == fabric::GenerateNodeConfig::name() => Some(Box::new(GenerateNodeConfig)),
            _ => None,
        }
    }

    fn make_service(&mut self, context: &Context) -> Box<Service> {
        let mut guard = JAVA_SERVICE_RUNTIME.lock().unwrap();
        let runtime = if guard.is_some() {
            guard.clone().unwrap()
        } else {
            use exonum::helpers::fabric::keys;
            let jvm_config: JvmConfig = context.get(keys::SERVICES_SECRET_CONFIGS).unwrap().get("ejb_jvm_config").unwrap().clone().try_into().unwrap();
            let service_config: ServiceConfig = context.get(keys::SERVICES_SECRET_CONFIGS).unwrap().get("ejb_service_config").unwrap().clone().try_into().unwrap();
            let runtime = JavaServiceRuntime::new(Config { jvm_config, service_config });
            *guard = Some(runtime.clone());
            runtime
        };

        Box::new(runtime.service_proxy().clone())
    }
}
