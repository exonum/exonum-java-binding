use exonum::{
    blockchain::Service,
    helpers::fabric::{self, Command, CommandExtension, Context, ServiceFactory},
};

use super::cmd::{Finalize, GenerateTemplate, Run};
use super::config::{Config, InternalConfig};
use super::java_service_runtime::JavaServiceRuntime;
use super::utils::{absolute_library_path, system_classpath};

/// Factory for particular Java service.
/// Initializes EJB runtime and creates `ServiceProxy`.
pub struct JavaServiceFactory;

impl ServiceFactory for JavaServiceFactory {
    fn service_name(&self) -> &str {
        "JAVA_SERVICE_FACTORY"
    }

    fn command(&mut self, command: &str) -> Option<Box<CommandExtension>> {
        // Execute EJB configuration steps along with standard Exonum Core steps.
        match command {
            v if v == fabric::GenerateCommonConfig.name() => Some(Box::new(GenerateTemplate)),
            v if v == fabric::Finalize.name() => Some(Box::new(Finalize)),
            v if v == fabric::Run.name() => Some(Box::new(Run)),
            _ => None,
        }
    }

    fn make_service(&mut self, context: &Context) -> Box<Service> {
        let runtime = {
            use exonum::helpers::fabric::keys;
            let config: Config = context
                .get(keys::NODE_CONFIG)
                .expect("Unable to read node configuration.")
                .services_configs
                .get(super::cmd::EJB_CONFIG_SECTION_NAME)
                .expect("Unable to read EJB configuration.")
                .clone()
                .try_into()
                .expect("Invalid EJB configuration format.");

            let internal_config = InternalConfig {
                system_class_path: system_classpath(),
                system_lib_path: Some(absolute_library_path()),
            };
            JavaServiceRuntime::get_or_create(config, internal_config)
        };

        Box::new(runtime.service_proxy().clone())
    }
}
