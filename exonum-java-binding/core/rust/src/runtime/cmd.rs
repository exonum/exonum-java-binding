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

use super::{Config, JvmConfig, RuntimeConfig, ServiceConfig};
use exonum::helpers::fabric::keys;
use exonum::helpers::fabric::Argument;
use exonum::helpers::fabric::CommandExtension;
use exonum::helpers::fabric::Context;
use exonum::node::NodeConfig;
use failure;
use toml::Value;

/// This code encapsulates the logic of processing of our extensions to the node's binary command
/// line arguments. The general idea: we have extensions to two regular commands of the node -
/// `finalize` and `run`. We process them on every step and store intermediate results to the
/// persistent storage available on the specific step. Finally, after the `run` step we compose the
/// `Config` structure that contains all required info for service initialization.

// Parameters for `finalize` command
const EJB_ARTIFACT_URI: &str = "EJB_ARTIFACT_URI";
// Parameters for `run` command
const EJB_LOG_CONFIG_PATH: &str = "EJB_LOG_CONFIG_PATH";
const EJB_CLASSPATH_SYSTEM: &str = "EJB_CLASSPATH_SYSTEM";
const EJB_LIBPATH: &str = "EJB_LIBPATH";
const EJB_PORT: &str = "EJB_PORT";
const JVM_DEBUG_SOCKET: &str = "JVM_DEBUG_SOCKET";
const JVM_ARGS_PREPEND: &str = "JVM_ARGS_PREPEND";
const JVM_ARGS_APPEND: &str = "JVM_ARGS_APPEND";
// TOML keys for EJB configuration
pub const EJB_CONFIG_SECTION_NAME: &str = "ejb";

/// Encapsulates processing of extensions of the `finalize` command. At this step we gather some of
/// the required parameters for private configuration. Also, at this step the node config
/// creation happens, so we store our newly created private configuration there.
pub struct Finalize;

impl CommandExtension for Finalize {
    fn args(&self) -> Vec<Argument> {
        vec![Argument::new_named(
            EJB_ARTIFACT_URI,
            true,
            "An URI of an EJB service artifact.",
            None,
            "ejb-artifact-uri",
            false,
        )]
    }

    fn execute(&self, mut context: Context) -> Result<Context, failure::Error> {
        // Getting the artifact's URI
        let artifact_uri = context.arg(EJB_ARTIFACT_URI)?;

        // Creating new EJB service config.
        let service_config = ServiceConfig { artifact_uri };

        // Writing EJB config to the node services configuration section.
        write_ejb_config(&mut context, Value::try_from(service_config)?);
        Ok(context)
    }
}

/// Encapsulates processing of extensions of the `run` command. At this step we gather additional
/// private parameters for service configuration and optional parameters for JVM configuration and
/// produce the complete EJB configuration that gets stored to the `Context` for further processing
/// during the service initialization.
pub struct Run;

impl CommandExtension for Run {
    fn args(&self) -> Vec<Argument> {
        vec![
            Argument::new_named(
                EJB_PORT,
                true,
                "A port of the HTTP server for Java services. Must be distinct from the ports used by Exonum.",
                None,
                "ejb-port",
                false
            ),
            Argument::new_named(
                EJB_LOG_CONFIG_PATH,
                false,
                "Path to log4j configuration file.",
                None,
                "ejb-log-config-path",
                false,
            ),
            Argument::new_named(
                JVM_DEBUG_SOCKET,
                false,
                "Allows JVM being remotely debugged. Takes a socket address as a parameter in form \
                of `HOSTNAME:PORT`. For example, `localhost:8000`",
                None,
                "jvm-debug",
                false,
            ),
            Argument::new_named(
                JVM_ARGS_PREPEND,
                false,
                "Additional parameters for JVM that precede the rest of arguments. Must not have a \
                leading dash. For example, `Xmx2G`",
                None,
                "jvm-args-prepend",
                true,
            ),
            Argument::new_named(
                JVM_ARGS_APPEND,
                false,
                "Additional parameters for JVM that get appended to the rest of arguments. Must not\
                 have a leading dash. For example, `Xmx2G`",
                None,
                "jvm-args-append",
                true,
            ),
            Argument::new_named(
                EJB_CLASSPATH_SYSTEM,
                true,
                "Java runtime classpath. Must include all its dependencies.",
                None,
                "ejb-classpath",
                false,
            ),
            Argument::new_named(
                EJB_LIBPATH,
                true,
                "Path to java-bindings shared library.",
                None,
                "ejb-libpath",
                false,
            ),
        ]
    }

    fn execute(&self, mut context: Context) -> Result<Context, failure::Error> {
        let log_config_path = context.arg(EJB_LOG_CONFIG_PATH).unwrap_or_default();
        let port = context.arg(EJB_PORT)?;
        let args_prepend: Vec<String> = context.arg_multiple(JVM_ARGS_PREPEND).unwrap_or_default();
        let args_append: Vec<String> = context.arg_multiple(JVM_ARGS_APPEND).unwrap_or_default();
        let jvm_debug_socket = context.arg(JVM_DEBUG_SOCKET).ok();
        let system_class_path = context.arg(EJB_CLASSPATH_SYSTEM)?;
        let system_lib_path = context.arg(EJB_LIBPATH)?;

        // Getting full EJB config saved at finalize step.
        let service_config: ServiceConfig = read_ejb_config(&context)?;

        let jvm_config = JvmConfig {
            args_prepend,
            args_append,
            jvm_debug_socket,
        };

        let runtime_config = RuntimeConfig {
            log_config_path,
            port,
            system_class_path,
            system_lib_path,
        };

        let config = Config {
            service_config,
            jvm_config,
            runtime_config,
        };

        // Write the final EJB configuration.
        write_ejb_config(&mut context, Value::try_from(config)?);
        Ok(context)
    }
}

/// Returns the `ejb` section of service configs of `NodeConfig`.
fn read_ejb_config<T: serde::de::DeserializeOwned>(
    context: &Context,
) -> Result<T, toml::de::Error> {
    let node_config = get_node_config(context);
    node_config
        .services_configs
        .get(EJB_CONFIG_SECTION_NAME)
        .expect("EJB config not found")
        .clone()
        .try_into()
}

/// Updates the `ejb` section of service configs of `NodeConfig` with `value` and puts updated
/// `NodeConfig` back to `Context`
fn write_ejb_config(context: &mut Context, value: Value) {
    let mut node_config = get_node_config(context);
    node_config
        .services_configs
        .insert(EJB_CONFIG_SECTION_NAME.to_owned(), value);
    context.set(keys::NODE_CONFIG, node_config);
}

/// Extracts the `NodeConfig` from `Context` for further processing
fn get_node_config(context: &Context) -> NodeConfig {
    context
        .get(keys::NODE_CONFIG)
        .expect("Unable to read node configuration.")
}
