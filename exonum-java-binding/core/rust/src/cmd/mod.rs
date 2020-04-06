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

use std::path::PathBuf;

use anyhow;
use exonum_cli::command::{
    ExonumCommand, Finalize, GenerateConfig, GenerateTemplate, Maintenance, StandardResult,
};
pub use exonum_cli::DefaultConfigManager;
use structopt::StructOpt;

use super::Config;

pub use self::run::*;
pub use self::run_dev::*;

mod run;
mod run_dev;

/// Exonum Java Bindings Application.
///
/// Configures and runs Exonum node with Java runtime enabled.
///
/// See https://exonum.com/doc/version/1.0.0/get-started/java-binding/#node-configuration
#[derive(StructOpt, Debug)]
#[structopt(author, about)]
#[allow(clippy::large_enum_variant)]
pub enum Command {
    /// Generate common part of the nodes configuration.
    #[structopt(name = "generate-template")]
    GenerateTemplate(GenerateTemplate),
    /// Generate public and private configs of the node.
    #[structopt(name = "generate-config")]
    GenerateConfig(GenerateConfig),
    /// Generate final node configuration using public configs
    /// of other nodes in the network.
    #[structopt(name = "finalize")]
    Finalize(Finalize),
    /// Run the node with provided node config and Java runtime enabled.
    #[structopt(name = "run")]
    Run(Run),
    /// Run the node in development mode (generate configuration and db files automatically).
    ///
    /// Runs one node with public API address 127.0.0.1:8080, private API address 127.0.0.1:8081,
    /// EJB port 6400.
    #[structopt(name = "run-dev")]
    RunDev(RunDev),
    /// Perform different maintenance actions.
    #[structopt(name = "maintenance")]
    Maintenance(Maintenance),
}

impl Command {
    /// Parse arguments from the command line arguments.  Print the
    /// error message and quit the program in case of failure.
    pub fn from_args() -> Self {
        <Self as StructOpt>::from_args()
    }
}

impl EjbCommand for Command {
    fn execute(self) -> Result<EjbCommandResult, anyhow::Error> {
        match self {
            Command::GenerateTemplate(c) => c.execute().map(Into::into),
            Command::GenerateConfig(c) => c.execute().map(Into::into),
            Command::Finalize(c) => c.execute().map(Into::into),
            Command::Run(c) => c.execute(),
            Command::RunDev(c) => c.execute(),
            Command::Maintenance(c) => c.execute().map(Into::into),
        }
    }
}

/// Possible output of the Java Bindings CLI commands.
pub enum EjbCommandResult {
    /// Output of the standard Exonum Core commands.
    Standard(StandardResult),
    /// Output of EJB-specific `run` command.
    EjbRun(Box<Config>),
}

impl From<StandardResult> for EjbCommandResult {
    fn from(result: StandardResult) -> Self {
        EjbCommandResult::Standard(result)
    }
}

/// Interface of Java Bindings CLI commands.
pub trait EjbCommand {
    /// Returns the result of command execution.
    fn execute(self) -> Result<EjbCommandResult, anyhow::Error>;
}

/// Concatenates PathBuf and string. Useful to make a `PathBuf` to a file in the specific directory.
pub fn concat_path(first: PathBuf, second: &str) -> PathBuf {
    let mut path = first;
    path.push(second);
    path
}
