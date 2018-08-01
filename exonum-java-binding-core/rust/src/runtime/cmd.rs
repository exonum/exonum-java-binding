use super::{Config, PrivateConfig, PublicConfig};
use exonum::helpers::fabric::keys;
use exonum::helpers::fabric::Argument;
use exonum::helpers::fabric::CommandExtension;
use exonum::helpers::fabric::Context;
use exonum::node::NodeConfig;
use failure;
use toml::Value;

use std::{env, fs};

const EJB_JVM_ARGUMENTS: &str = "EJB_JVM_ARGUMENTS";
const EJB_LOG_CONFIG_PATH: &str = "EJB_LOG_CONFIG_PATH";
const EJB_SERVICE_CLASSPATH: &str = "EJB_SERVICE_CLASSPATH";
const EJB_MODULE_NAME: &str = "EJB_MODULE_NAME";
const EJB_PORT: &str = "EJB_PORT";
const EJB_PUBLIC_CONFIG_NAME: &str = "ejb_public_config";
pub const EJB_CONFIG_NAME: &str = "ejb";

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
        let mut services_public_configs = context.get(keys::SERVICES_CONFIG).unwrap_or_default();
        services_public_configs.extend(
            vec![(
                EJB_PUBLIC_CONFIG_NAME.to_owned(),
                Value::try_from(public_config).unwrap(),
            )].into_iter(),
        );
        context.set(keys::SERVICES_CONFIG, services_public_configs);

        Ok(context)
    }
}

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

        // Creating new private config.
        let private_config = PrivateConfig {
            user_parameters: Vec::new(),
            system_class_path: get_system_classpath(),
            service_class_path,
            log_config_path: String::new(),
            port: 0,
        };

        // Getting public config saved at first step out of common section of configuration.
        let common_config = context.get(keys::COMMON_CONFIG)?;
        let public_config = common_config
            .services_config
            .get(EJB_PUBLIC_CONFIG_NAME)
            .expect("EJB public config not found")
            .clone()
            .try_into()?;

        // Forming full EJB config.
        let config = Config {
            public_config,
            private_config,
        };

        // Writing EJB config to the node services configuration section.
        let mut node_config: NodeConfig = context.get(keys::NODE_CONFIG)?;
        node_config
            .services_configs
            .insert(EJB_CONFIG_NAME.to_owned(), Value::try_from(config)?);
        context.set(keys::NODE_CONFIG, node_config);
        Ok(context)
    }
}

pub struct Run;

impl CommandExtension for Run {
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
        let user_parameters = context.arg_multiple(EJB_JVM_ARGUMENTS).unwrap_or_default();
        let log_config_path = context.arg(EJB_LOG_CONFIG_PATH).unwrap_or_default();
        let port = context.arg(EJB_PORT)?;

        // Getting full EJB config saved at finalize step.
        let mut node_config: NodeConfig = context.get(keys::NODE_CONFIG)?;
        let mut ejb_config: Config = node_config
            .services_configs
            .get(EJB_CONFIG_NAME)
            .cloned()
            .unwrap()
            .try_into()?;

        // Updating parameters in EJB config using provided arguments.
        ejb_config.private_config.user_parameters = user_parameters;
        ejb_config.private_config.log_config_path = log_config_path;
        ejb_config.private_config.port = port;

        // Updating EJB config.
        node_config
            .services_configs
            .insert(EJB_CONFIG_NAME.to_owned(), Value::try_from(ejb_config)?);
        context.set(keys::NODE_CONFIG, node_config);
        Ok(context)
    }
}

fn get_system_classpath() -> String {
    let mut jars = Vec::new();
    let jars_directory = {
        let mut current_directory =
            env::current_dir().expect("Could not get current working directory");
        current_directory.push("lib/java");
        current_directory
    };
    for entry in fs::read_dir(jars_directory).unwrap() {
        let file = entry.unwrap();
        if file.file_type().unwrap().is_file() {
            jars.push(file.path());
        } else {
            continue;
        }
    }

    let jars = jars.iter().map(|p| p.to_str().unwrap());
    env::join_paths(jars).unwrap().into_string().unwrap()
}
