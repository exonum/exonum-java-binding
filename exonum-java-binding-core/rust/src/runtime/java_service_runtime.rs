use exonum::blockchain::Service;
use exonum::helpers::fabric::{Context, ServiceFactory};

use jni;
use std::sync::Arc;

use proxy::ServiceProxy;
use runtime::config::{Config, JvmConfig, ServiceConfig};
use utils::unwrap_jni;
use {Executor, DumbExecutor};

const SERVICE_BOOTSTRAP_PATH: &str = "com/exonum/binding/service/ServiceBootstrap";
const START_SERVICE_SIGNATURE: &str =
    "(Ljava/lang/String;I)Lcom/exonum/binding/service/adapters/UserServiceAdapter;";

/// Controls JVM and java service.
#[allow(dead_code)]
pub struct JavaServiceRuntime {
    executor: DumbExecutor,
    service_proxy: ServiceProxy<DumbExecutor>,
}

impl JavaServiceRuntime {
    /// Create new runtime from config.
    pub fn new(config: Config) -> Self {
        let executor = Self::create_executor(config.jvm_config);
        Self::with_executor(executor, config.service_config)
    }

    /// Create new runtime with prepared `Executor`.
    pub fn with_executor(executor: DumbExecutor, config: ServiceConfig) -> Self {
        let service_proxy = Self::create_service(config, executor.clone());
        Self {
            executor,
            service_proxy,
        }
    }

    fn create_executor(config: JvmConfig) -> DumbExecutor {
        let java_vm = {
            let mut args_builder = jni::InitArgsBuilder::new().version(jni::JNIVersion::V8);

            if config.debug {
                args_builder = args_builder.option("-Xcheck:jni").option("-Xdebug");
            }

            if let Some(class_path) = config.class_path {
                args_builder = args_builder.option(&format!("-Djava.class.path={}", class_path));
            }

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

impl ServiceFactory for JavaServiceRuntime {
    fn make_service(&mut self, _: &Context) -> Box<Service> {
        Box::new(self.service_proxy.clone())
    }
}
