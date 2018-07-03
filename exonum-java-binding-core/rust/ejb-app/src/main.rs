extern crate exonum_btc_anchoring;
extern crate exonum_configuration;
extern crate java_bindings;
extern crate serde;
#[macro_use]
extern crate serde_derive;

use exonum_btc_anchoring::ServiceFactory as BtcAnchoringServiceFactory;
use exonum_configuration::ServiceFactory as ConfigurationServiceFactory;
use java_bindings::exonum::helpers::config::ConfigFile;
use java_bindings::exonum::helpers::fabric;

const PATH_TO_SERVICES_TO_ENABLE: &str = "ejb_app_services.toml";

#[derive(Serialize, Deserialize)]
struct ServicesToEnable {
    services: Vec<String>,
}

fn main() {
    let services: ServicesToEnable = ConfigFile::load(PATH_TO_SERVICES_TO_ENABLE).unwrap();
    let mut builder =
        fabric::NodeBuilder::new().with_service(Box::new(java_bindings::JavaServiceFactory));
    for service in &services.services {
        if service == "configuration" {
            builder = builder.with_service(Box::new(ConfigurationServiceFactory));
        } else if service == "btc-anchoring" {
            builder = builder.with_service(Box::new(BtcAnchoringServiceFactory));
        } else {
            panic!("Found unknown service name {}", service);
        }
    }
    builder.run()
}
