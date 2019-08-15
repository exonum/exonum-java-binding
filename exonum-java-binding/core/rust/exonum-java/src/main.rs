/*
 * Copyright 2019 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

extern crate env_logger;
//extern crate exonum_btc_anchoring; // FIXME: uncomment when anchoring uses merkledb
extern crate exonum_configuration;
extern crate exonum_time;
extern crate java_bindings;
#[macro_use]
extern crate log;

#[cfg(test)]
extern crate tempfile;

use java_bindings::get_lib_version;

mod node_builder;

fn main() {
    env_logger::init();
    // Panic if `_JAVA_OPTIONS` environmental variable is set.
    java_bindings::panic_if_java_options();

    // Log app's metadata
    log_app_metadata();

    let builder = node_builder::create();
    builder.run()
}

// Prints info about version and build mode of started app to the STDOUT.
fn log_app_metadata() {
    let version = get_lib_version();
    let build_type = if cfg!(debug_assertions) {
        "debug"
    } else {
        "release"
    };

    info!(
        "Started Exonum Java {} (built in {} mode)",
        version, build_type
    );
}
