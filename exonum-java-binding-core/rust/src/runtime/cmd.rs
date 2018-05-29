use exonum::helpers::fabric::CommandExtension;
use exonum::helpers::fabric::Argument;
use exonum::helpers::fabric::Context;
use failure;
use exonum::helpers::fabric::keys;
use toml::Value;
use super::{JvmConfig, ServiceConfig, Config};
use exonum::node::NodeConfig;

pub struct GenerateNodeConfig;

impl CommandExtension for GenerateNodeConfig {
    fn args(&self) -> Vec<Argument> {
        vec![
            Argument::new_named(
                "EJB_DEBUG",
                false,
                "Debug mode for JVM.",
                None,
                "ejb-debug",
                false
            ),
            Argument::new_named(
                "EJB_LOG_CONFIG_PATH",
                false,
                "Path to log4j configuration file.",
                None,
                "ejb-log-config-path",
                false
            ),
            Argument::new_named(
                "EJB_CLASSPATH",
                true,
                "Classpath for JVM.",
                None,
                "ejb-classpath",
                false
            ),
            Argument::new_named(
                "EJB_LIBPATH",
                true,
                "Libpath for JVM.",
                None,
                "ejb-libpath",
                false
            ),
        ]
    }

    fn execute(&self, mut context: Context) -> Result<Context, failure::Error> {
        let debug = context.arg("EJB_DEBUG").unwrap_or_default();
        let log_config_path = context.arg("EJB_LOG_CONFIG_PATH").unwrap_or_default();
        let class_path = context.arg("EJB_CLASSPATH")?;
        let lib_path = context.arg("EJB_LIBPATH")?;

        let jvm_config = JvmConfig {
            debug,
            class_path,
            lib_path,
            log_config_path,
        };

        let mut services_secret_configs = context
            .get(keys::SERVICES_SECRET_CONFIGS)
            .unwrap_or_default();
        services_secret_configs.extend(
            vec![
                (
                    "ejb_jvm_config".to_owned(),
                    Value::try_from(jvm_config).unwrap()
                ),
            ].into_iter(),
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
                "EJB_MODULE_NAME",
                true,
                "Module name for EJB.",
                None,
                "ejb-module-name",
                false
            ),
            Argument::new_named("EJB_PORT", true, "Port for EJB.", None, "ejb-port", false),
        ]
    }

    fn execute(&self, mut context: Context) -> Result<Context, failure::Error> {
        let module_name = context.arg("EJB_MODULE_NAME")?;
        let port = context.arg("EJB_PORT")?;

        let service_config = ServiceConfig { module_name, port };

        let jvm_config: JvmConfig = context
            .get(keys::SERVICES_SECRET_CONFIGS)
            .unwrap()
            .get("ejb_jvm_config")
            .unwrap()
            .clone()
            .try_into()?;

        let config = Config {
            jvm_config,
            service_config,
        };

        let mut node_config: NodeConfig = context.get(keys::NODE_CONFIG)?;
        node_config.services_configs.insert(
            "ejb".to_owned(),
            Value::try_from(config)?,
        );
        context.set(keys::NODE_CONFIG, node_config);
        Ok(context)
    }
}
