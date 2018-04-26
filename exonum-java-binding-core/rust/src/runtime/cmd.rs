use exonum::helpers::fabric::CommandExtension;
use exonum::helpers::fabric::Argument;
use exonum::helpers::fabric::Context;
use failure;
use exonum::helpers::fabric::keys;
use toml::Value;
use super::{JvmConfig, ServiceConfig};

pub struct GenerateNodeConfig;

impl CommandExtension for GenerateNodeConfig {
    fn args(&self) -> Vec<Argument> {
        vec![
            Argument::new_named("EJB_DEBUG", false, "Debug mode for JVM.", None, "ejb-debug", false),
            Argument::new_named("EJB_CLASSPATH", true, "Classpath for JVM.", None, "ejb-classpath", false),
            Argument::new_named("EJB_LIBPATH", true, "Libpath for JVM.", None, "ejb-libpath", false),
            Argument::new_named("EJB_MODULE_NAME", true, "Module name for EJB.", None, "ejb-module-name", false),
            Argument::new_named("EJB_PORT", true, "Port for EJB.", None, "ejb-port", false),
        ]
    }

    fn execute(&self, mut context: Context) -> Result<Context, failure::Error> {
        let debug = context.arg("EJB_DEBUG").unwrap_or_default();
        let class_path = context.arg("EJB_CLASSPATH")?;
        let lib_path = context.arg("EJB_LIBPATH")?;

        let module_name = context.arg("EJB_MODULE_NAME")?;
        let port = context.arg("EJB_PORT")?;

        let jvm_config = JvmConfig {
            debug,
            class_path,
            lib_path,
        };

        let service_config = ServiceConfig {
            module_name,
            port,
        };

        let mut services_public_configs = context
            .get(keys::SERVICES_PUBLIC_CONFIGS)
            .unwrap_or_default();

        services_public_configs.extend(
            vec![
                (
                    "ejb_service_config".to_owned(),
                    Value::try_from(service_config).unwrap(),
                ),
            ].into_iter(),
        );

        let mut services_secret_configs = context
            .get(keys::SERVICES_SECRET_CONFIGS)
            .unwrap_or_default();
        services_secret_configs.extend(
            vec![
                (
                    "ejb_jvm_config".to_owned(),
                    Value::try_from(jvm_config).unwrap(),
                ),
            ].into_iter(),
        );

        context.set(keys::SERVICES_PUBLIC_CONFIGS, services_public_configs);
        context.set(keys::SERVICES_SECRET_CONFIGS, services_secret_configs);

        Ok(context)
    }
}
