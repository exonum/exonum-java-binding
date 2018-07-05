extern crate exonum_btc_anchoring;
extern crate exonum_configuration;
extern crate java_bindings;
extern crate serde;
#[macro_use]
extern crate serde_derive;

mod service_factories;

fn main() {
    let builder = service_factories::create_node_builder();
    builder.run()
}
