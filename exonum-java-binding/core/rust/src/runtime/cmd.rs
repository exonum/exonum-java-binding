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

use super::{Config, PrivateConfig, PublicConfig};
use exonum::helpers::fabric::keys;
use exonum::helpers::fabric::Argument;
use exonum::helpers::fabric::CommandExtension;
use exonum::helpers::fabric::Context;
use exonum::node::NodeConfig;
use failure;
use toml::Value;

/// This code encapsulates the logic of processing of our extensions to the node's binary command
/// line arguments. The general idea: we have extensions to three regular commands of the node -
/// `generate-template`, `finalize` and `run`. We process them on every step and store intermediate
/// results to the persistent storage available on the specific step. Finally, after the `run` step
/// we compose the `Config` structure that contains all required info for service initialization.

// Parameters for `generate-template` command
const EJB_MODULE_NAME: &str = "EJB_MODULE_NAME";
// Parameters for `finalize` command
const EJB_SERVICE_CLASSPATH: &str = "EJB_SERVICE_CLASSPATH";
// Parameters for `run` command
const EJB_LOG_CONFIG_PATH: &str = "EJB_LOG_CONFIG_PATH";
const EJB_PORT: &str = "EJB_PORT";
const JVM_DEBUG_SOCKET: &str = "JVM_DEBUG_SOCKET";
const JVM_ARGS_PREPEND: &str = "JVM_ARGS_PREPEND";
const JVM_ARGS_APPEND: &str = "JVM_ARGS_APPEND";
// TOML keys for EJB configuration
pub const EJB_CONFIG_SECTION_NAME: &str = "ejb";
const EJB_PUBLIC_CONFIG_KEY: &str = "ejb_public_config";

/// Encapsulates processing of extensions of the `generate-template` command. At this step we gather
/// required parameters for the public part of the EJB configuration which is shared between all
/// nodes.
pub struct GenerateTemplate;

impl CommandExtension for GenerateTemplate {
    fn args(&self) -> Vec<Argument> {
        vec![Argument::new_named(
            EJB_MODULE_NAME,
            true,
            "A fully-qualified class name of the user service module.",
            None,
            "ejb-module-name",
            false,
        )]
    }

    fn execute(&self, mut context: Context) -> Result<Context, failure::Error> {
        let module_name = context.arg(EJB_MODULE_NAME)?;

        let public_config = PublicConfig { module_name };

        // Adding EJB public config to the services public configs section in common.toml.
        add_public_config(&mut context, Value::try_from(public_config)?);

        Ok(context)
    }
}

/// Encapsulates processing of extensions of the `finalize` command. At this step we gather some of
/// the required parameters for private configuration. Also, at this step the node config
/// creation happens, so we store there our newly created private configuration as well as the
/// public EJB configuration created at `generate-template` step.
pub struct Finalize;

impl CommandExtension for Finalize {
    fn args(&self) -> Vec<Argument> {
        vec![Argument::new_named(
            EJB_SERVICE_CLASSPATH,
            true,
            "Java service classpath. Shall not include Java Binding classes.",
            None,
            "ejb-service-classpath",
            false,
        )]
    }

    fn execute(&self, mut context: Context) -> Result<Context, failure::Error> {
        let service_class_path = context.arg(EJB_SERVICE_CLASSPATH)?;

        // Getting public config saved at first step out of common section of configuration.
        let public_config = get_public_config(&context)?;

        // Creating new private config.
        let private_config = PrivateConfig {
            service_class_path,
            ..Default::default()
        };

        // Forming full EJB config.
        let config = Config {
            public_config,
            private_config,
        };

        // Writing EJB config to the node services configuration section.
        write_ejb_config(&mut context, Value::try_from(config)?);
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
        ]
    }

    fn execute(&self, mut context: Context) -> Result<Context, failure::Error> {
        let log_config_path = context.arg(EJB_LOG_CONFIG_PATH).unwrap_or_default();
        let port = context.arg(EJB_PORT)?;
        let args_prepend: Vec<String> = context.arg_multiple(JVM_ARGS_PREPEND).unwrap_or_default();
        let args_append: Vec<String> = context.arg_multiple(JVM_ARGS_APPEND).unwrap_or_default();
        let jvm_debug_socket = context.arg(JVM_DEBUG_SOCKET).ok();

        // Getting full EJB config saved at finalize step.
        let mut config = read_ejb_config(&context)?;

        // Filling out the rest of private configuration.
        let private_config = PrivateConfig {
            service_class_path: config.private_config.service_class_path,
            log_config_path,
            port,
            args_append,
            args_prepend,
            jvm_debug_socket,
        };

        config.private_config = private_config;

        // Write the final EJB configuration.
        write_ejb_config(&mut context, Value::try_from(config)?);
        Ok(context)
    }
}

/// Adds the public part of EJB configuration to the service configs in common configuration.
fn add_public_config(context: &mut Context, value: Value) {
    let mut services_public_configs = context.get(keys::SERVICES_CONFIG).unwrap_or_default();
    services_public_configs.extend(vec![(EJB_PUBLIC_CONFIG_KEY.to_owned(), value)].into_iter());
    context.set(keys::SERVICES_CONFIG, services_public_configs);
}

/// Returns the public part of EJB configuration from common configuration.
fn get_public_config(context: &Context) -> Result<PublicConfig, toml::de::Error> {
    let common_config = context
        .get(keys::COMMON_CONFIG)
        .expect("Could not read common config");
    common_config
        .services_config
        .get(EJB_PUBLIC_CONFIG_KEY)
        .expect("EJB public config not found")
        .clone()
        .try_into()
}

/// Returns the `ejb` section of service configs of `NodeConfig`.
fn read_ejb_config(context: &Context) -> Result<Config, toml::de::Error> {
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
