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

use super::{Config, EjbConfig, JvmConfig, ServiceConfig};
use exonum::helpers::fabric::keys;
use exonum::helpers::fabric::Argument;
use exonum::helpers::fabric::CommandExtension;
use exonum::helpers::fabric::Context;
use exonum::node::NodeConfig;
use failure;
use serde::Deserialize;
use std::collections::BTreeMap;
use toml::Value;

/// This code encapsulates the logic of processing of our extensions to the node's binary command
/// line arguments. The general idea: we have extensions to three regular commands of the node -
/// `generate-config`, `finalize` and `run`. We process them on every step and store intermediate
/// results to the persistent storage available on the specific step. Finally, after the `run` step
/// we compose the `Config` structure that contains all required info for service initialization.

const EJB_LOG_CONFIG_PATH: &str = "EJB_LOG_CONFIG_PATH";
const EJB_CLASSPATH: &str = "EJB_CLASSPATH";
const EJB_LIBPATH: &str = "EJB_LIBPATH";
const EJB_MODULE_NAME: &str = "EJB_MODULE_NAME";
const EJB_PORT: &str = "EJB_PORT";
const JVM_DEBUG_SOCKET: &str = "JVM_DEBUG_SOCKET";
const JVM_ARGS_PREPEND: &str = "JVM_ARGS_PREPEND";
const JVM_ARGS_APPEND: &str = "JVM_ARGS_APPEND";
pub const EJB_CONFIG_SECTION_NAME: &str = "ejb";
const EJB_CONFIG_KEY: &str = "ejb_config";
const SERVICE_CONFIG_KEY: &str = "service_config";

/// Encapsulates processing of extensions of the `generate-config` command. At this step we gather
/// required parameters for EJB configuration and store them under the section for the secret
/// configuration of services that becomes available at this step.
pub struct GenerateNodeConfig;

impl CommandExtension for GenerateNodeConfig {
    fn args(&self) -> Vec<Argument> {
        vec![
            Argument::new_named(
                EJB_LOG_CONFIG_PATH,
                false,
                "Path to log4j configuration file.",
                None,
                "ejb-log-config-path",
                false,
            ),
            Argument::new_named(
                EJB_CLASSPATH,
                true,
                "Java service classpath. Must include all its dependencies.",
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
        // Get the arguments for current step
        let log_config_path = context.arg(EJB_LOG_CONFIG_PATH).unwrap_or_default();
        let class_path = context.arg(EJB_CLASSPATH)?;
        let lib_path = context.arg(EJB_LIBPATH)?;

        let ejb_config = EjbConfig {
            class_path,
            lib_path,
            log_config_path,
        };

        // Store `EjbConfig` under the secret services configs section. Will be retrieved later at
        // the `Finalize` step for further processing.
        update_secret_services_config(&mut context, Value::try_from(ejb_config)?);

        Ok(context)
    }
}

/// Encapsulates processing of extensions of the `finalize` command. At this step we gather
/// required parameters for service configuration. Also, at this step the node config finalization
/// happens, so we store there our newly created service configuration as well as the EJB
/// configuration created at `generate-config` step.
pub struct Finalize;

impl CommandExtension for Finalize {
    fn args(&self) -> Vec<Argument> {
        vec![
            Argument::new_named(
                EJB_MODULE_NAME,
                true,
                "A fully-qualified class name of the user service module.",
                None,
                "ejb-module-name",
                false
            ),
            Argument::new_named(
                EJB_PORT,
                true,
                "A port of the HTTP server for Java services. Must be distinct from the ports used by Exonum.",
                None,
                "ejb-port",
                false
            ),
        ]
    }

    fn execute(&self, mut context: Context) -> Result<Context, failure::Error> {
        // Get the arguments for current step
        let module_name = context.arg(EJB_MODULE_NAME)?;
        let port = context.arg(EJB_PORT)?;

        let service_config = ServiceConfig { module_name, port };

        // Retrieve `EjbConfig` saved at the `GenerateNodeConfig` step
        let ejb_config: EjbConfig = get_services_secret_configs(&context)
            .get(EJB_CONFIG_KEY)
            .expect("Can't get EJB config")
            .clone()
            .try_into()?;

        // Store `ServiceConfig` and `EjbConfig` under the `keys::NODE_CONFIG`. Later they will be
        // retrieved to construct the `Config` entity at the `Run` stage
        let mut config: BTreeMap<String, Value> = BTreeMap::new();
        config.insert(EJB_CONFIG_KEY.to_owned(), Value::try_from(ejb_config)?);
        config.insert(
            SERVICE_CONFIG_KEY.to_owned(),
            Value::try_from(service_config)?,
        );

        // Put config to NodeConfig and store updated NodeConfig back to Context
        update_node_config(&mut context, Value::try_from(config)?);

        Ok(context)
    }
}

/// Encapsulates processing of extensions of the `run` command. At this step we gather optional
/// parameters for JVM configuration and produce the complete EJB configuration that gets stored to
/// the `Context` for further processing during the service initialization.
pub struct Run;

impl CommandExtension for Run {
    fn args(&self) -> Vec<Argument> {
        vec![
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
        // Get the arguments for current step
        let args_prepend: Vec<String> = context.arg_multiple(JVM_ARGS_PREPEND).unwrap_or_default();
        let args_append: Vec<String> = context.arg_multiple(JVM_ARGS_APPEND).unwrap_or_default();
        let jvm_debug_socket = context.arg(JVM_DEBUG_SOCKET).ok();

        // Gather optional user arguments for JVM configuration
        let jvm_config = JvmConfig {
            args_prepend,
            args_append,
            jvm_debug_socket,
        };

        // Retrieve EjbConfig & ServiceConfig stored at previous step
        let ejb_config: EjbConfig = extract_from_services_config(&context, EJB_CONFIG_KEY)?;
        let service_config: ServiceConfig =
            extract_from_services_config(&context, SERVICE_CONFIG_KEY)?;

        // Construct the complete EJB configuration
        let config = Config {
            jvm_config,
            ejb_config,
            service_config,
        };

        // Store configuration to the context. It will be used during service's bootstrap.
        update_node_config(&mut context, Value::try_from(config)?);

        Ok(context)
    }
}

/// Updates the `ejb` section of services secret configs with `value` and stores it to the `Context`
fn update_secret_services_config(context: &mut Context, value: Value) {
    let mut services_secret_configs = get_services_secret_configs(&context);
    services_secret_configs.extend(vec![(EJB_CONFIG_KEY.to_owned(), value)].into_iter());
    context.set(keys::SERVICES_SECRET_CONFIGS, services_secret_configs);
}

/// Returns underlying map of services configs section
fn get_services_secret_configs(context: &Context) -> BTreeMap<String, Value> {
    context
        .get(keys::SERVICES_SECRET_CONFIGS)
        .expect("Can't get services secret configs")
}

/// Updates the `ejb` section of service configs of `NodeConfig` with `value` and puts updated
/// `NodeConfig` back to `Context`
fn update_node_config(context: &mut Context, value: Value) {
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

/// Extracts entity from the service configs section of the `NodeConfig` in the `Context`
fn extract_from_services_config<'de, T>(context: &Context, key: &str) -> Result<T, failure::Error>
where
    T: Deserialize<'de>,
{
    //  Get a reference to the EJB configuration
    let service_config: BTreeMap<String, Value> = get_node_config(context)
        .services_configs
        .get(EJB_CONFIG_SECTION_NAME)
        .expect("Unable to read EJB configuration.")
        .clone()
        .try_into()?;

    // Retrieve config by key
    let config = service_config
        .get(key)
        .expect(&format!(
            "Unable to read config with key [{}] from node configuration",
            key
        ))
        .clone()
        .try_into()?;

    Ok(config)
}
