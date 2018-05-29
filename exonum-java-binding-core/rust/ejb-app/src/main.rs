extern crate java_bindings;

use java_bindings::exonum::helpers::fabric;

fn main() {
    let builder =
        fabric::NodeBuilder::new().with_service(Box::new(java_bindings::JavaServiceFactory));
    builder.run()
}
