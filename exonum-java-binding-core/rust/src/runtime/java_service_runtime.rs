use jni;
use Executor;
use DumbExecutor;
use proxy::ServiceProxy;
use std::sync::Arc;
use utils::unwrap_jni;
use exonum::helpers::fabric::ServiceFactory;
use exonum::blockchain::Service;
use exonum::helpers::fabric::Context;
use runtime::config::{Config, JvmConfig, ServiceConfig};

const SERVICE_BOOTSTRAP_PATH: &str = "com/exonum/binding/service/ServiceBootstrap";
const START_SERVICE_SIGNATURE: &str =
    "(Ljava/lang/String;I)Lcom/exonum/binding/service/adapters/UserServiceAdapter;";

pub struct JavaServiceRuntime {
    pub executor: DumbExecutor,
    pub service_proxy: ServiceProxy<DumbExecutor>,
}

impl JavaServiceRuntime {
    pub fn new(config: Config) -> Self {
        let executor = Self::create_executor(config.jvm_config);
        let service_proxy = Self::create_service(config.service_config, executor.clone());
        Self {
            executor,
            service_proxy,
        }
    }

    pub fn create_executor(config: JvmConfig) -> DumbExecutor {
        let java_vm = {
            let mut args_builder = jni::InitArgsBuilder::new().version(jni::JNIVersion::V8);
            if config.debug {
                args_builder = args_builder.option("-Xcheck:jni").option("-Xdebug");
            }
            let args = args_builder.build().unwrap();
            jni::JavaVM::new(args).unwrap()
        };
        DumbExecutor { vm: Arc::new(java_vm) }
    }

    pub fn create_service(config: ServiceConfig, executor: DumbExecutor) -> ServiceProxy<DumbExecutor> {
        let service = unwrap_jni(executor.with_attached(|env| {
            let classpath = env.new_string(config.classpath).unwrap();
            let classpath: jni::objects::JObject = *classpath;
            let service = env.call_static_method(
                SERVICE_BOOTSTRAP_PATH,
                "startService",
                START_SERVICE_SIGNATURE,
                &[classpath.into(), config.port.into()],
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
