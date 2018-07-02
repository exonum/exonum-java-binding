extern crate java_bindings;
extern crate exonum_configuration;

use java_bindings::exonum::helpers::fabric;
use exonum_configuration::ServiceFactory as ConfigurationServiceFactory;

fn main() {
    let builder =
        fabric::NodeBuilder::new()
            .with_service(Box::new(java_bindings::JavaServiceFactory))
            .with_service(Box::new(ConfigurationServiceFactory));
    builder.run()
}
