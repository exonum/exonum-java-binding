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

use log::info;

use crate::node::run_node;
use java_bindings::{get_lib_version, Command};

mod node;

#[tokio::main]
async fn main() -> Result<(), anyhow::Error> {
    env_logger::init();
    // Panic if `_JAVA_OPTIONS` environmental variable is set.
    java_bindings::panic_if_java_options();

    // Log app's metadata
    log_app_metadata();

    run_node(Command::from_args()).await
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
