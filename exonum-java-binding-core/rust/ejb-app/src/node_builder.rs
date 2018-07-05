use exonum_btc_anchoring::ServiceFactory as BtcAnchoringServiceFactory;
use exonum_configuration::ServiceFactory as ConfigurationServiceFactory;
use java_bindings::exonum::helpers::config::ConfigFile;
use java_bindings::exonum::helpers::fabric::{self, ServiceFactory};
use java_bindings::JavaServiceFactory;
use std::collections::{HashMap, HashSet};

const PATH_TO_SERVICES_TO_ENABLE: &str = "ejb_app_services.toml";
const CONFIGURATION_SERVICE: &str = "configuration";
const BTC_ANCHORING_SERVICE: &str = "btc-anchoring";
const EJB_SERVICE: &str = "ejb-service";

#[derive(Serialize, Deserialize)]
struct ServicesToEnable {
    services: HashSet<String>,
}

fn service_factories() -> HashMap<String, Box<ServiceFactory>> {
    let mut service_factories = HashMap::new();
    service_factories.insert(
        CONFIGURATION_SERVICE.to_owned(),
        Box::new(ConfigurationServiceFactory) as Box<ServiceFactory>,
    );
    service_factories.insert(
        BTC_ANCHORING_SERVICE.to_owned(),
        Box::new(BtcAnchoringServiceFactory) as Box<ServiceFactory>,
    );
    service_factories.insert(
        EJB_SERVICE.to_owned(),
        Box::new(JavaServiceFactory) as Box<ServiceFactory>,
    );
    service_factories
}

fn services_to_enable() -> HashSet<String> {
    let ServicesToEnable { mut services } =
        ConfigFile::load(PATH_TO_SERVICES_TO_ENABLE).unwrap_or(ServicesToEnable {
            services: {
                let mut services = HashSet::new();
                services.insert(CONFIGURATION_SERVICE.to_owned());
                services
            },
        });

    // Add EJB_SERVICE if it's missing
    services.insert(EJB_SERVICE.to_owned());

    services
}

pub fn create() -> fabric::NodeBuilder {
    let services = services_to_enable();
    let mut service_factories = service_factories();

    let mut builder = fabric::NodeBuilder::new();
    for service_name in &services {
        match service_factories.remove(service_name) {
            Some(factory) => {
                builder = builder.with_service(factory);
            }
            None => panic!("Found unknown service name {}", service_name),
        }
    }
    builder
}
