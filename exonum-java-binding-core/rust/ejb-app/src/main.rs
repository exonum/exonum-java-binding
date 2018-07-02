extern crate exonum_configuration;
extern crate java_bindings;

use exonum_configuration::ServiceFactory as ConfigurationServiceFactory;
use java_bindings::exonum::helpers::fabric;

fn main() {
    let builder = fabric::NodeBuilder::new()
        .with_service(Box::new(java_bindings::JavaServiceFactory))
        .with_service(Box::new(ConfigurationServiceFactory));
    builder.run()
}
