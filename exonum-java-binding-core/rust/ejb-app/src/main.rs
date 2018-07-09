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
    /// Panic if `_JAVA_OPTIONS` environmental variable is set.
    java_bindings::panic_if_java_options();

    let builder = node_builder::create();
    builder.run()
}
