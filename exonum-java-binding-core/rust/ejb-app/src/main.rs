extern crate exonum_btc_anchoring;
extern crate exonum_configuration;
extern crate java_bindings;
extern crate serde;
#[macro_use]
extern crate serde_derive;
extern crate toml;

#[cfg(test)]
extern crate tempfile;

mod node_builder;

fn main() {
    let builder = node_builder::create();
    builder.run()
}
