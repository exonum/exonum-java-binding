use exonum::blockchain::Service;
use exonum::helpers::fabric::{Context, ServiceFactory};

use jni;

use std::sync::Arc;
use std::path::PathBuf;
use std::path::Path;

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
        let service_proxy = Self::create_service(config.service_config, executor.clone());
        Self {
            executor,
            service_proxy,
        }
    }

    fn create_executor(config: JvmConfig) -> DumbExecutor {
        let java_vm = {
            let mut args_builder = jni::InitArgsBuilder::new()
                .version(jni::JNIVersion::V8)
                .option(&get_libpath_option());

            if config.debug {
                args_builder = args_builder.option("-Xcheck:jni").option("-Xdebug");
            }

            args_builder = args_builder.option(&format!("-Djava.class.path={}", config.class_path));

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

fn get_libpath_option() -> String {
    let library_path = rust_project_root_dir()
        .join(target_path())
        .canonicalize()
        .expect(
            "Target path not found, but there should be \
            the libjava_bindings dynamically loading library",
        );
    let library_path = library_path.to_str().expect(
        "Failed to convert FS path into utf-8",
    );

    format!("-Djava.library.path={}", library_path)
}

fn rust_project_root_dir() -> PathBuf {
    Path::new(env!("CARGO_MANIFEST_DIR"))
        .canonicalize()
        .unwrap()
}

#[cfg(debug_assertions)]
fn target_path() -> &'static str {
    "target/debug"
}

#[cfg(not(debug_assertions))]
fn target_path() -> &'static str {
    "target/release"
}

impl ServiceFactory for JavaServiceRuntime {
    fn make_service(&mut self, _: &Context) -> Box<Service> {
        Box::new(self.service_proxy.clone())
    }
}
