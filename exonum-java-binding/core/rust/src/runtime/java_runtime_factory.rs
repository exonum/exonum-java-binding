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
    Executor, InitArgs, InitArgsBuilder, JavaVM, Result as JniResult,
};

use crate::{
    runtime::config::{self, InternalConfig, JvmConfig, RuntimeConfig},
    utils::unwrap_jni,
    JavaRuntimeProxy,
};

const SERVICE_RUNTIME_BOOTSTRAP_PATH: &str = "com/exonum/binding/app/ServiceRuntimeBootstrap";
const CREATE_RUNTIME_ADAPTER_SIGNATURE: &str =
    "(Ljava/lang/String;I)Lcom/exonum/binding/core/runtime/ServiceRuntimeAdapter;";

/// Instantiates JavaRuntimeProxy using provided Executor and runtime configuration parameters.
pub fn create_service_runtime(
    executor: Executor,
    runtime_config: &RuntimeConfig,
) -> JavaRuntimeProxy {
    let runtime_adapter = create_service_runtime_adapter(&executor, &runtime_config);
    JavaRuntimeProxy::new(executor, runtime_adapter)
}

/// Creates service runtime adapter for JavaRuntimeProxy.
fn create_service_runtime_adapter(executor: &Executor, config: &RuntimeConfig) -> GlobalRef {
    unwrap_jni(executor.with_attached(|env| {
        let artifacts_path = config
            .artifacts_path
            .to_str()
            .expect("Unable to convert artifacts_path to string");
        let artifacts_path = JObject::from(env.new_string(artifacts_path)?);
        let serviceRuntime = env
            .call_static_method(
                SERVICE_RUNTIME_BOOTSTRAP_PATH,
                "createServiceRuntime",
                CREATE_RUNTIME_ADAPTER_SIGNATURE,
                &[artifacts_path.into(), config.port.into()],
            )?
            .l()?;
        env.new_global_ref(serviceRuntime)
    }))
}

/// Initializes JVM with provided configuration.
///
/// # Panics
///
/// - If user specified invalid additional JVM parameters.
pub fn create_java_vm(
    jvm_config: &JvmConfig,
    runtime_config: &RuntimeConfig,
    internal_config: InternalConfig,
) -> JavaVM {
    let args = build_jvm_arguments(jvm_config, runtime_config, internal_config)
        .expect("Unable to build arguments for JVM");

    jni::JavaVM::new(args)
        .map_err(transform_jni_error)
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
    args_builder = add_user_arguments(args_builder, args_prepend);

    // Add required arguments
    args_builder = add_required_arguments(args_builder, runtime_config, internal_config);

    // Add optional arguments
    args_builder = add_optional_arguments(args_builder, jvm_config);

    // Append extra user arguments
    args_builder = add_user_arguments(args_builder, args_append);

    // Log JVM arguments
    log_jvm_arguments(&args_builder);

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
    // Use overridden system library path if any.
    let system_lib_path = runtime_config
        .override_system_lib_path
        .clone()
        .unwrap_or(internal_config.system_lib_path);

    args_builder
        .option(&format!("-Djava.library.path={}", system_lib_path))
        .option(&format!(
            "-Djava.class.path={}",
            internal_config.system_class_path
        ))
        .option(&format!(
            "-Dlog4j.configurationFile={}",
            runtime_config.log_config_path.to_string_lossy()
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

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn transform_jni_error_error_type_other() {
        let error = Error::from(ErrorKind::Other(jni::sys::JNI_EINVAL));
        assert_eq!(
            "Invalid arguments",
            transform_jni_error(error).description()
        );
    }

    #[test]
    fn transform_jni_error_not_type_other() {
        let error_detached = Error::from(ErrorKind::ThreadDetached);
        assert_eq!(
            "Current thread is not attached to the java VM",
            transform_jni_error(error_detached).description()
        );
    }

    #[test]
    fn transform_jni_error_type_other_code_not_in_range() {
        let error = Error::from(ErrorKind::Other(-42));
        assert_eq!("JNI error", transform_jni_error(error).description());
    }
}
