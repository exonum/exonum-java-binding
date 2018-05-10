use exonum::blockchain::Service;
use exonum::helpers::fabric::{Context, ServiceFactory};

use jni;

use proxy::ServiceProxy;
use proxy::JniExecutor;
use runtime::config::{Config, JvmConfig, ServiceConfig};
use runtime::cmd::{GenerateNodeConfig, Finalize};
use utils::unwrap_jni;
use MainExecutor;
use exonum::helpers::fabric::CommandExtension;

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
    pub fn service_proxy(&self) -> ServiceProxy {
        self.service_proxy.clone()
    }

    fn create_executor(config: JvmConfig) -> MainExecutor {
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
        MainExecutor::new(java_vm)
    }

    fn create_service(config: ServiceConfig, executor: MainExecutor) -> ServiceProxy {
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

use std::sync::{Once, ONCE_INIT};

static mut JAVA_SERVICE_RUNTIME: Option<JavaServiceRuntime> = None;
static JAVA_SERVICE_RUNTIME_INIT: Once = ONCE_INIT;

/// TODO
pub struct JavaServiceFactory;

impl ServiceFactory for JavaServiceFactory {
    fn command(&mut self, command: &str) -> Option<Box<CommandExtension>> {
        use exonum::helpers::fabric;
        match command {
            v if v == fabric::GenerateNodeConfig::name() => Some(Box::new(GenerateNodeConfig)),
            v if v == fabric::Finalize::name() => Some(Box::new(Finalize)),
            _ => None,
        }
    }

    fn make_service(&mut self, context: &Context) -> Box<Service> {
        let runtime = unsafe {
            JAVA_SERVICE_RUNTIME_INIT.call_once(|| {
                use exonum::helpers::fabric::keys;
                let config: Config = context.get(keys::NODE_CONFIG)
                    .unwrap()
                    .services_configs
                    .get("ejb")
                    .unwrap()
                    .clone()
                    .try_into()
                    .unwrap();
                JAVA_SERVICE_RUNTIME = Some(JavaServiceRuntime::new(config));
            });
            JAVA_SERVICE_RUNTIME.clone().unwrap()
        };

        Box::new(runtime.service_proxy().clone())
    }
}
