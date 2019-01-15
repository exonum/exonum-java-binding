extern crate exonum_btc_anchoring;
extern crate exonum_configuration;
extern crate java_bindings;
extern crate serde;
#[macro_use]
extern crate serde_derive;
extern crate toml;
extern crate env_logger;

#[cfg(test)]
extern crate tempfile;

mod node_builder;

fn main() {
    let _ = env_logger::try_init();
    // Panic if `_JAVA_OPTIONS` environmental variable is set.
    java_bindings::panic_if_java_options();

    let builder = node_builder::create();
    builder.run()
}
