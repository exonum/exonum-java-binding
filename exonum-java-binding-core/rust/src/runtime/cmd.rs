use super::{Config, JvmConfig, ServiceConfig};
use exonum::helpers::fabric::keys;
use exonum::helpers::fabric::Argument;
use exonum::helpers::fabric::CommandExtension;
use exonum::helpers::fabric::Context;
use exonum::node::NodeConfig;
use failure;
use toml::Value;

const EJB_JVM_ARGUMENTS: &str = "EJB_JVM_ARGUMENTS";
const EJB_LOG_CONFIG_PATH: &str = "EJB_LOG_CONFIG_PATH";
const EJB_CLASSPATH: &str = "EJB_CLASSPATH";
const EJB_LIBPATH: &str = "EJB_LIBPATH";
const EJB_MODULE_NAME: &str = "EJB_MODULE_NAME";
const EJB_PORT: &str = "EJB_PORT";
const EJB_JVM_CONFIG_NAME: &str = "ejb_jvm_config";
pub const EJB_CONFIG_NAME: &str = "ejb";

pub struct GenerateNodeConfig;

impl CommandExtension for GenerateNodeConfig {
    fn args(&self) -> Vec<Argument> {
        vec![
            Argument::new_named(
                EJB_JVM_ARGUMENTS,
                false,
                "Additional parameters for JVM. Must not have a leading dash. \
                 For example, `Xmx2G` or `Xdebug`",
                None,
                "ejb-jvm-args",
                true,
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
        let user_parameters = context.arg_multiple(EJB_JVM_ARGUMENTS).unwrap_or_default();
        let log_config_path = context.arg(EJB_LOG_CONFIG_PATH).unwrap_or_default();
        let class_path = context.arg(EJB_CLASSPATH)?;
        let lib_path = context.arg(EJB_LIBPATH)?;

        let jvm_config = JvmConfig {
            user_parameters,
            class_path,
            lib_path,
            log_config_path,
        };

        let mut services_secret_configs = context
            .get(keys::SERVICES_SECRET_CONFIGS)
            .unwrap_or_default();
        services_secret_configs.extend(
            vec![(
                EJB_JVM_CONFIG_NAME.to_owned(),
                Value::try_from(jvm_config).unwrap(),
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

        let jvm_config: JvmConfig = context
            .get(keys::SERVICES_SECRET_CONFIGS)
            .expect("Can't get services secret configs")
            .get(EJB_JVM_CONFIG_NAME)
            .expect("Can't get JVM config")
            .clone()
            .try_into()?;

        let config = Config {
            jvm_config,
            service_config,
        };

        let mut node_config: NodeConfig = context.get(keys::NODE_CONFIG)?;
        node_config
            .services_configs
            .insert(EJB_CONFIG_NAME.to_owned(), Value::try_from(config)?);
        context.set(keys::NODE_CONFIG, node_config);
        Ok(context)
    }
}
