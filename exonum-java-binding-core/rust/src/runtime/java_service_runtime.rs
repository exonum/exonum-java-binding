use jni;
use Executor;
use DumbExecutor;
use proxy::ServiceProxy;
use std::sync::Arc;
use utils::unwrap_jni;
use exonum::helpers::fabric::ServiceFactory;
use exonum::blockchain::Service;
use exonum::helpers::fabric::Context;

const SERVICE_BOOTSTRAP_PATH: &str = "com/exonum/binding/service/ServiceBootstrap";
const START_SERVICE_SIGNATURE: &str =
    "(Ljava/lang/String;I)Lcom/exonum/binding/service/adapters/UserServiceAdapter;";

pub struct JavaServiceRuntime {
    executor: DumbExecutor,
    service_proxy: ServiceProxy<DumbExecutor>,
}

impl JavaServiceRuntime {
    pub fn new(classpath: &str, port: i32) -> Self {
        let java_vm = {
            let args_builder = jni::InitArgsBuilder::new().version(jni::JNIVersion::V8);
            let args = args_builder.build().unwrap();
            jni::JavaVM::new(args).unwrap()
        };
        let executor = DumbExecutor { vm: Arc::new(java_vm) };
        Self::with_executor(executor, classpath, port)
    }

    pub fn with_executor(executor: DumbExecutor, classpath: &str, port: i32) -> Self {
        let service = unwrap_jni(executor.with_attached(|env| {
            let classpath = env.new_string(classpath).unwrap();
            let classpath: jni::objects::JObject = *classpath;
            let service = env.call_static_method(
                SERVICE_BOOTSTRAP_PATH,
                "startService",
                START_SERVICE_SIGNATURE,
                &[classpath.into(), port.into()],
            )?.l()?;
            env.new_global_ref(env.auto_local(service).as_obj())
        }));
        let service_proxy = ServiceProxy::from_global_ref(executor.clone(), service);
        Self {
            executor,
            service_proxy,
        }
    }
}

impl ServiceFactory for JavaServiceRuntime {
    fn make_service(&mut self, _: &Context) -> Box<Service> {
        Box::new(self.service_proxy.clone())
    }
}
