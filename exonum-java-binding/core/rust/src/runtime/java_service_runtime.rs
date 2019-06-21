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

use jni::{
    self,
    errors::{Error, ErrorKind},
    objects::{GlobalRef, JObject},
    InitArgs, InitArgsBuilder, JavaVM, Result as JniResult,
};

use proxy::ServiceProxy;
use runtime::config::{self, Config, InternalConfig, JvmConfig, RuntimeConfig};
use std::{path::Path, sync::Arc};
use utils::{check_error_on_exception, convert_to_string, unwrap_jni};
use Executor;

const SERVICE_RUNTIME_BOOTSTRAP_PATH: &str = "com/exonum/binding/app/ServiceRuntimeBootstrap";
const CREATE_RUNTIME_SIGNATURE: &str = "(I)Lcom/exonum/binding/runtime/ServiceRuntime;";
const LOAD_ARTIFACT_SIGNATURE: &str = "(Ljava/lang/String;)Ljava/lang/String;";
const CREATE_SERVICE_SIGNATURE: &str =
    "(Ljava/lang/String;)Lcom/exonum/binding/service/adapters/UserServiceAdapter;";

/// Controls JVM and java service.
#[allow(dead_code)]
#[derive(Clone)]
pub struct JavaServiceRuntime {
    executor: Executor,
    service_runtime: GlobalRef,
}

impl JavaServiceRuntime {
    /// Creates new runtime from provided config.
    ///
    /// There can be only one `JavaServiceRuntime` instance at a time.
    pub fn new(config: Config, internal_config: InternalConfig) -> Self {
        let java_vm =
            Self::create_java_vm(&config.jvm_config, &config.runtime_config, internal_config);
        Self::create_with_jvm(Arc::new(java_vm), config.runtime_config.port)
    }

    /// Creates new runtime for given JVM and port.
    ///
    /// Also, this function is public for being used from integration tests.
    pub fn create_with_jvm(java_vm: Arc<JavaVM>, port: i32) -> Self {
        let executor = Executor::new(java_vm.clone());
        let service_runtime = Self::create_service_runtime_java(port, executor.clone());
        JavaServiceRuntime {
            executor,
            service_runtime,
        }
    }

    /// Creates service runtime that is responsible for services management.
    fn create_service_runtime_java(port: i32, executor: Executor) -> GlobalRef {
        unwrap_jni(executor.with_attached(|env| {
            let serviceRuntime = env
                .call_static_method(
                    SERVICE_RUNTIME_BOOTSTRAP_PATH,
                    "createServiceRuntime",
                    CREATE_RUNTIME_SIGNATURE,
                    &[port.into()],
                )?
                .l()?;
            env.new_global_ref(serviceRuntime)
        }))
    }

    /// Creates a new service instance using the given artifact id.
    ///
    /// Panics if there are errors on Java side.
    pub fn create_service(&self, artifact_id: &str) -> ServiceProxy {
        unwrap_jni(self.executor.with_attached(|env| {
            let artifact_id_obj: JObject = env.new_string(artifact_id)?.into();
            let service = check_error_on_exception(
                env,
                env.call_method(
                    self.service_runtime.as_obj(),
                    "createService",
                    CREATE_SERVICE_SIGNATURE,
                    &[artifact_id_obj.into()],
                ),
            )
            .unwrap_or_else(|err_msg| {
                panic!(
                    "Unable to create service for artifact_id [{}]: {}",
                    artifact_id, err_msg
                )
            })
            .l()?;
            let service = env.new_global_ref(service)?;
            Ok(ServiceProxy::from_global_ref(
                self.executor.clone(),
                service,
            ))
        }))
    }

    /// Loads an artifact from the specified location involving verification of the artifact.
    /// Returns an unique service artifact identifier that must be specified in subsequent
    /// operations with it.
    ///
    /// Panics if there are errors on Java side.
    pub fn load_artifact<P: AsRef<Path>>(&self, artifact_path: P) -> String {
        unwrap_jni(self.executor.with_attached(|env| {
            let artifact_path = artifact_path.as_ref().to_str().unwrap();
            let artifact_path_obj: JObject = env.new_string(artifact_path)?.into();
            let artifact_id = check_error_on_exception(
                env,
                env.call_method(
                    self.service_runtime.as_obj(),
                    "loadArtifact",
                    LOAD_ARTIFACT_SIGNATURE,
                    &[artifact_path_obj.into()],
                ),
            )
            .unwrap_or_else(|err_msg| {
                panic!("Unable to load artifact {}: {}", artifact_path, err_msg)
            })
            .l()?;
            convert_to_string(env, artifact_id)
        }))
    }

    /// Initializes JVM with provided configuration.
    ///
    /// # Panics
    ///
    /// - If user specified invalid additional JVM parameters.
    fn create_java_vm(
        jvm_config: &JvmConfig,
        runtime_config: &RuntimeConfig,
        internal_config: InternalConfig,
    ) -> JavaVM {
        let args = Self::build_jvm_arguments(jvm_config, runtime_config, internal_config)
            .expect("Unable to build arguments for JVM");

        jni::JavaVM::new(args)
            .map_err(Self::transform_jni_error)
            .expect("Unable to create JVM")
    }

    /// Transforms JNI errors by converting JNI error codes of error of type `Other` to its string
    /// representation.
    fn transform_jni_error(error: Error) -> Error {
        match error.0 {
            ErrorKind::Other(code) => match code {
                jni::sys::JNI_EINVAL => "Invalid arguments".into(),
                jni::sys::JNI_EEXIST => "VM already created".into(),
                jni::sys::JNI_ENOMEM => "Not enough memory".into(),
                jni::sys::JNI_EVERSION => "JNI version error".into(),
                jni::sys::JNI_ERR => "Unknown JNI error".into(),
                _ => error,
            },
            _ => error,
        }
    }

    /// Builds arguments for JVM initialization.
    fn build_jvm_arguments(
        jvm_config: &JvmConfig,
        runtime_config: &RuntimeConfig,
        internal_config: InternalConfig,
    ) -> JniResult<InitArgs> {
        let mut args_builder = jni::InitArgsBuilder::new().version(jni::JNIVersion::V8);

        let args_prepend = jvm_config.args_prepend.clone();
        let args_append = jvm_config.args_append.clone();

        // Prepend extra user arguments
        args_builder = Self::add_user_arguments(args_builder, args_prepend);

        // Add required arguments
        args_builder = Self::add_required_arguments(args_builder, runtime_config, internal_config);

        // Add optional arguments
        args_builder = Self::add_optional_arguments(args_builder, jvm_config);

        // Append extra user arguments
        args_builder = Self::add_user_arguments(args_builder, args_append);

        // Log JVM arguments
        Self::log_jvm_arguments(&args_builder);

        args_builder.build()
    }

    /// Adds extra user arguments (optional) to JVM configuration.
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

    /// Adds required EJB-related arguments to JVM configuration.
    fn add_required_arguments(
        args_builder: InitArgsBuilder,
        runtime_config: &RuntimeConfig,
        internal_config: InternalConfig,
    ) -> InitArgsBuilder {
        args_builder
            .option(&format!(
                "-Djava.library.path={}",
                internal_config.system_lib_path
            ))
            .option(&format!(
                "-Djava.class.path={}",
                internal_config.system_class_path
            ))
            .option(&format!(
                "-Dlog4j.configurationFile={}",
                runtime_config.log_config_path
            ))
    }

    /// Adds optional user arguments to JVM configuration.
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

    /// Logs JVM arguments collected by a particular builder.
    fn log_jvm_arguments(args_builder: &InitArgsBuilder) {
        let mut jvm_args_line = String::new();
        for option in args_builder.options().iter() {
            jvm_args_line.push(' ');
            jvm_args_line.push_str(option);
        }
        info!("JVM arguments:{}", jvm_args_line);
    }

    /// Returns a reference to the runtime's executor.
    pub fn get_executor(&self) -> &Executor {
        &self.executor
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn transform_jni_error_error_type_other() {
        let error = Error::from(ErrorKind::Other(jni::sys::JNI_EINVAL));
        assert_eq!(
            "Invalid arguments",
            JavaServiceRuntime::transform_jni_error(error).description()
        );
    }

    #[test]
    fn transform_jni_error_not_type_other() {
        let error_detached = Error::from(ErrorKind::ThreadDetached);
        assert_eq!(
            "Current thread is not attached to the java VM",
            JavaServiceRuntime::transform_jni_error(error_detached).description()
        );
    }

    #[test]
    fn transform_jni_error_type_other_code_not_in_range() {
        let error = Error::from(ErrorKind::Other(-42));
        assert_eq!(
            "JNI error",
            JavaServiceRuntime::transform_jni_error(error).description()
        );
    }
}
