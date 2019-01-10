use super::{Config, EjbConfig, JvmConfig, ServiceConfig};
use exonum::helpers::fabric::keys;
use exonum::helpers::fabric::Argument;
use exonum::helpers::fabric::CommandExtension;
use exonum::helpers::fabric::Context;
use exonum::node::NodeConfig;
use failure;
use std::collections::BTreeMap;
use toml::Value;

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
        let log_config_path = context.arg(EJB_LOG_CONFIG_PATH).unwrap_or_default();
        let class_path = context.arg(EJB_CLASSPATH)?;
        let lib_path = context.arg(EJB_LIBPATH)?;

        let ejb_config = EjbConfig {
            class_path,
            lib_path,
            log_config_path,
        };

        // Keep `EjbConfig` under the `keys::SERVICES_SECRET_CONFIGS` section. Will be retrieved
        // later at the `Finalize` step for further processing.
        let mut services_secret_configs = context
            .get(keys::SERVICES_SECRET_CONFIGS)
            .unwrap_or_default();
        services_secret_configs.extend(
            vec![(
                EJB_CONFIG_KEY.to_owned(),
                Value::try_from(ejb_config).unwrap(),
            )]
            .into_iter(),
        );

        context.set(keys::SERVICES_SECRET_CONFIGS, services_secret_configs);

        Ok(context)
    }
}

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
        let module_name = context.arg(EJB_MODULE_NAME)?;
        let port = context.arg(EJB_PORT)?;

        let service_config = ServiceConfig { module_name, port };

        // Retrieve `EjbConfig` saved at the `GenerateNodeConfig` step
        let ejb_config: EjbConfig = context
            .get(keys::SERVICES_SECRET_CONFIGS)
            .expect("Can't get services secret configs")
            .get(EJB_CONFIG_KEY)
            .expect("Can't get JVM config")
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

        let mut node_config: NodeConfig = context.get(keys::NODE_CONFIG)?;
        node_config
            .services_configs
            .insert(EJB_CONFIG_SECTION_NAME.to_owned(), Value::try_from(config)?);
        context.set(keys::NODE_CONFIG, node_config);
        Ok(context)
    }
}

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
        let args_prepend: Vec<String> = context.arg_multiple(JVM_ARGS_PREPEND).unwrap_or_default();
        let args_append: Vec<String> = context.arg_multiple(JVM_ARGS_APPEND).unwrap_or_default();
        let jvm_debug_socket = context.arg(JVM_DEBUG_SOCKET).ok();

        // Gather optional user arguments for JVM configuration
        let jvm_config = JvmConfig {
            args_prepend,
            args_append,
            jvm_debug_socket,
        };

        // Get a reference to the parts of configuration stored at previous step
        let svc_config: BTreeMap<String, Value> = context
            .get(keys::NODE_CONFIG)
            .expect("Unable to read node configuration.")
            .services_configs
            .get(EJB_CONFIG_SECTION_NAME)
            .expect("Unable to read EJB configuration.")
            .clone()
            .try_into()?;

        // Retrieve `EjbConfig` and `ServiceConfig` saved at the `Finalize` step
        let ejb_config = svc_config
            .get(EJB_CONFIG_KEY)
            .expect("Unable to read EjbConfig from node configuration")
            .clone()
            .try_into()?;

        let service_config = svc_config
            .get(SERVICE_CONFIG_KEY)
            .expect("Unable to read ServiceConfig from node configuration")
            .clone()
            .try_into()?;

        // Now we're ready to construct the full configuration
        let config = Config {
            jvm_config,
            ejb_config,
            service_config,
        };

        // Store configuration to the context. It will be used during service's bootstrap.
        let mut node_config: NodeConfig = context.get(keys::NODE_CONFIG)?;
        node_config
            .services_configs
            .insert(EJB_CONFIG_SECTION_NAME.to_owned(), Value::try_from(config)?);
        context.set(keys::NODE_CONFIG, node_config);

        Ok(context)
    }
}
