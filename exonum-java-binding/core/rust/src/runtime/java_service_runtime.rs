/*
 * Copyright 2019 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

use jni::{self, InitArgs, InitArgsBuilder, JavaVM, Result};

use std::sync::{Arc, Once, ONCE_INIT};

use proxy::{JniExecutor, ServiceProxy};
use runtime::config::{self, Config, InternalConfig, JvmConfig, RuntimeConfig, ServiceConfig};
use utils::{join_paths, unwrap_jni};
use MainExecutor;

static mut JAVA_SERVICE_RUNTIME: Option<JavaServiceRuntime> = None;
static JAVA_SERVICE_RUNTIME_INIT: Once = ONCE_INIT;

const SERVICE_BOOTSTRAP_PATH: &str = "com/exonum/binding/runtime/ServiceBootstrap";
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
    pub fn get_or_create(config: Config, internal_config: InternalConfig) -> Self {
        unsafe {
            // Initialize runtime if it wasn't created before.
            JAVA_SERVICE_RUNTIME_INIT.call_once(|| {
                let java_vm = Self::create_java_vm(
                    &config.jvm_config,
                    &config.runtime_config,
                    &config.service_config,
                    internal_config,
                );
                let executor = MainExecutor::new(Arc::new(java_vm));

                let service_proxy = Self::create_service(
                    &config.service_config.module_name,
                    config.runtime_config.port,
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
    fn create_java_vm(
        jvm_config: &JvmConfig,
        runtime_config: &RuntimeConfig,
        service_config: &ServiceConfig,
        internal_config: InternalConfig,
    ) -> JavaVM {
        let args =
            Self::build_jvm_arguments(jvm_config, runtime_config, service_config, internal_config)
                .expect("Unable to build arguments for JVM");
        jni::JavaVM::new(args).unwrap()
    }

    /// Builds arguments for JVM initialization.
    fn build_jvm_arguments(
        jvm_config: &JvmConfig,
        runtime_config: &RuntimeConfig,
        service_config: &ServiceConfig,
        internal_config: InternalConfig,
    ) -> Result<InitArgs> {
        let mut args_builder = jni::InitArgsBuilder::new().version(jni::JNIVersion::V8);

        let args_prepend = jvm_config.args_prepend.clone();
        let args_append = jvm_config.args_append.clone();

        // Prepend extra user arguments
        args_builder = Self::add_user_arguments(args_builder, args_prepend);

        // Add required arguments
        args_builder = Self::add_required_arguments(
            args_builder,
            runtime_config,
            service_config,
            internal_config,
        );

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
        mut args_builder: InitArgsBuilder,
        runtime_config: &RuntimeConfig,
        service_config: &ServiceConfig,
        internal_config: InternalConfig,
    ) -> InitArgsBuilder {
        // We do not use system library path in tests, because an absolute path to the native
        // library will be provided at compile time using RPATH.
        if internal_config.system_lib_path.is_some() {
            args_builder = args_builder.option(&format!(
                "-Djava.library.path={}",
                internal_config.system_lib_path.unwrap()
            ));
        }

        // We combine system and service class paths.
        let class_path = join_paths(&[
            &internal_config.system_class_path,
            &service_config.service_class_path,
        ]);

        args_builder
            .option(&format!("-Djava.class.path={}", class_path))
            .option(&format!(
                "-Dlog4j.configurationFile={}",
                runtime_config.log_config_path
            ))
    }

    /// Adds optional user arguments to JVM configuration
    fn add_optional_arguments(
        mut args_builder: InitArgsBuilder,
        jvm_config: &JvmConfig,
    ) -> InitArgsBuilder {
        if let Some(ref socket) = jvm_config.jvm_debug_socket {
            args_builder = args_builder.option(&format!(
                "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address={}",
                socket
            ));
        }
        args_builder
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
