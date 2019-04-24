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

use super::{paths::executable_directory, Config, JvmConfig, RuntimeConfig};
use exonum::{
    helpers::fabric::{keys, Argument, CommandExtension, Context},
    node::NodeConfig,
};
use failure;
use toml::Value;

use std::path::PathBuf;

/// This code encapsulates our extension to the node's binary command line arguments. We extend the
/// regular `run` command of the node with processing of custom EJB arguments and compose the
/// `Config` structure that contains all required data for initialization of service runtime.

// Parameters for `run` command
const EJB_LOG_CONFIG_PATH: &str = "EJB_LOG_CONFIG_PATH";
const EJB_PORT: &str = "EJB_PORT";
const JVM_DEBUG_SOCKET: &str = "JVM_DEBUG_SOCKET";
const JVM_ARGS_PREPEND: &str = "JVM_ARGS_PREPEND";
const JVM_ARGS_APPEND: &str = "JVM_ARGS_APPEND";
// TOML keys for EJB configuration
pub const EJB_CONFIG_SECTION_NAME: &str = "ejb";

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
        ]
    }

    fn execute(&self, mut context: Context) -> Result<Context, failure::Error> {
        let log_config_path = context
            .arg(EJB_LOG_CONFIG_PATH)
            .unwrap_or_else(|_| get_path_to_default_log_config());
        let port = context.arg(EJB_PORT)?;
        let args_prepend: Vec<String> = context.arg_multiple(JVM_ARGS_PREPEND).unwrap_or_default();
        let args_append: Vec<String> = context.arg_multiple(JVM_ARGS_APPEND).unwrap_or_default();
        let jvm_debug_socket = context.arg(JVM_DEBUG_SOCKET).ok();

        let jvm_config = JvmConfig {
            args_prepend,
            args_append,
            jvm_debug_socket,
        };

        let runtime_config = RuntimeConfig {
            log_config_path,
            port,
        };

        let config = Config {
            jvm_config,
            runtime_config,
        };

        // Write the final EJB configuration.
        write_ejb_config(&mut context, Value::try_from(config)?);
        Ok(context)
    }
}

/// Updates the `ejb` section of service configs of `NodeConfig` with `value` and puts updated
/// `NodeConfig` back to `Context`.
fn write_ejb_config(context: &mut Context, value: Value) {
    let mut node_config = get_node_config(context);
    node_config
        .services_configs
        .insert(EJB_CONFIG_SECTION_NAME.to_owned(), value);
    context.set(keys::NODE_CONFIG, node_config);
}

/// Extracts the `NodeConfig` from `Context` for further processing.
fn get_node_config(context: &Context) -> NodeConfig<PathBuf> {
    context
        .get(keys::NODE_CONFIG)
        .expect("Unable to read node configuration.")
}

/// Returns full path to the default log configuration file assuming the `exonum-java` app is
/// packaged/installed.
fn get_path_to_default_log_config() -> String {
    let log_fallback_path = {
        let mut path = executable_directory();
        path.push("log4j-fallback.xml");
        path
    };
    log_fallback_path.to_string_lossy().into_owned()
}
