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

use exonum_cli::command::{
    finalize::Finalize,
    generate_config::{GenerateConfig, PUB_CONFIG_FILE_NAME, SEC_CONFIG_FILE_NAME},
    generate_template::GenerateTemplate,
    maintenance::Maintenance,
    run::Run as StandardRun,
    ExonumCommand, StandardResult,
};
use failure;
use serde::{Deserialize, Serialize};
use structopt::StructOpt;

use std::{path::PathBuf, str::FromStr};

use super::{paths::executable_directory, Config, JvmConfig, RuntimeConfig};

/// Exonum Java Bindings Application.
///
/// Configures and runs Exonum node with Java runtime enabled.
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
    fn execute(self) -> Result<EjbCommandResult, failure::Error> {
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

/// EJB-specific `run` command which collects standard Exonum Core parameters and
/// also additional Java runtime and JVM configuration parameters.
#[derive(Debug, StructOpt, Serialize, Deserialize)]
#[structopt(rename_all = "kebab-case")]
pub struct Run {
    #[structopt(flatten)]
    #[serde(flatten)]
    standard: StandardRun,
    /// A port of the HTTP server for Java services.
    ///
    /// Must be distinct from the ports used by Exonum.
    #[structopt(long)]
    ejb_port: i32,
    /// Path to the directory containing Java service artifacts.
    #[structopt(long)]
    artifacts_path: PathBuf,
    /// Path to log4j configuration file.
    #[structopt(long)]
    ejb_log_config_path: Option<PathBuf>,
    /// Overrides the standard path to native libraries, enabling running the non-packaged
    /// exonum-java application.
    ///
    /// Mostly for internal usage.
    #[structopt(long)]
    ejb_override_java_library_path: Option<PathBuf>,
    /// Allows JVM being remotely debugged.
    ///
    /// Takes a socket address as a parameter in form of `HOSTNAME:PORT`.
    /// For example, `localhost:8000`
    #[structopt(long)]
    jvm_debug: Option<String>,
    /// Additional parameters for JVM that precede the rest of arguments.
    ///
    /// Must not have a leading dash. For example, `Xmx2G`.
    #[structopt(long)]
    jvm_args_prepend: Vec<String>,
    /// Additional parameters for JVM that get appended to the rest of arguments.
    ///
    /// Must not have a leading dash. For example, `Xmx2G`.
    #[structopt(long)]
    jvm_args_append: Vec<String>,
}

/// EJB-specific `run-dev` command.
///
/// Automatically generates node configuration for one
/// validator and runs it using provided `artifacts_path` as a directory for Java service artifacts.
#[derive(Debug, StructOpt, Serialize, Deserialize)]
#[structopt(rename_all = "kebab-case")]
pub struct RunDev {
    /// Path to the directory containing Java service artifacts.
    #[structopt(long)]
    artifacts_path: PathBuf,
    /// Path to a directory for blockchain database and configuration files.
    ///
    /// Database is located in <blockchain_path>/db directory, node configuration files
    /// are located in <blockchain_path>/config directory. Existing files and directories are
    /// reused. To generate new node configuration and start a new blockchain, the user must
    /// manually delete existing <blockchain_path> directory or specify a new one.
    #[structopt(long)]
    blockchain_path: PathBuf,
    /// Path to log4j configuration file.
    #[structopt(long)]
    ejb_log_config_path: Option<PathBuf>,
}

impl RunDev {
    /// Automatically generates node configuration and returns a path to node configuration file.
    ///
    /// Does not alter existing configuration files.
    fn generate_node_configuration_if_needed(&self) -> Result<PathBuf, failure::Error> {
        let config_directory = concat_path(self.blockchain_path.clone(), "config");
        let node_config_path = concat_path(config_directory.clone(), "node.toml");

        // Configuration files exist, skip generation.
        if config_directory.exists() {
            return Ok(node_config_path);
        }

        let validators_count = 1;
        let peer_address = "127.0.0.1:6200".parse().unwrap();
        let public_api_address = "127.0.0.1:8080".parse().unwrap();
        let private_api_address = "127.0.0.1:8081".parse().unwrap();
        let public_allow_origin = "http://127.0.0.1:8080, http://localhost:8080".into();
        let private_allow_origin = "http://127.0.0.1:8081, http://localhost:8081".into();
        let common_config_path = concat_path(config_directory.clone(), "template.toml");
        let public_config_path = concat_path(config_directory.clone(), PUB_CONFIG_FILE_NAME);
        let secret_config_path = concat_path(config_directory.clone(), SEC_CONFIG_FILE_NAME);

        let generate_template = GenerateTemplate {
            common_config: common_config_path.clone(),
            validators_count,
        };
        generate_template.execute()?;

        let generate_config = GenerateConfig {
            common_config: common_config_path.clone(),
            output_dir: config_directory.clone(),
            peer_address,
            listen_address: None,
            no_password: true,
            master_key_pass: None,
            master_key_path: None,
        };
        generate_config.execute()?;

        let finalize = Finalize {
            secret_config_path,
            output_config_path: node_config_path.clone(),
            public_configs: vec![public_config_path],
            public_api_address: Some(public_api_address),
            private_api_address: Some(private_api_address),
            public_allow_origin: Some(public_allow_origin),
            private_allow_origin: Some(private_allow_origin),
        };
        finalize.execute()?;

        Ok(node_config_path)
    }
}

/// Possible output of the Java Bindings CLI commands.
pub enum EjbCommandResult {
    /// Output of the standard Exonum Core commands.
    Standard(StandardResult),
    /// Output of EJB-specific `run` command.
    EjbRun(Config),
}

impl From<StandardResult> for EjbCommandResult {
    fn from(result: StandardResult) -> Self {
        EjbCommandResult::Standard(result)
    }
}

/// Interface of Java Bindings CLI commands.
pub trait EjbCommand {
    /// Returns the result of command execution.
    fn execute(self) -> Result<EjbCommandResult, failure::Error>;
}

impl EjbCommand for Run {
    fn execute(self) -> Result<EjbCommandResult, failure::Error> {
        if let StandardResult::Run(node_run_config) = self.standard.execute()? {
            let jvm_config = JvmConfig {
                args_prepend: self.jvm_args_prepend,
                args_append: self.jvm_args_append,
                jvm_debug_socket: self.jvm_debug,
            };

            let log_config_path = self
                .ejb_log_config_path
                .unwrap_or_else(get_path_to_default_log_config);

            let override_system_lib_path = self
                .ejb_override_java_library_path
                .map(|p| p.to_string_lossy().into_owned());

            let runtime_config = RuntimeConfig {
                artifacts_path: self.artifacts_path,
                log_config_path,
                port: self.ejb_port,
                override_system_lib_path,
            };

            let config = Config {
                run_config: node_run_config,
                jvm_config,
                runtime_config,
            };

            Ok(EjbCommandResult::EjbRun(config))
        } else {
            unreachable!("Standard run command returned invalid result")
        }
    }
}

impl EjbCommand for RunDev {
    fn execute(self) -> Result<EjbCommandResult, failure::Error> {
        let db_path = concat_path(self.blockchain_path.clone(), "db");
        let node_config_path = self.generate_node_configuration_if_needed()?;

        let ejb_port = 6400;

        let standard_run = StandardRun {
            node_config: node_config_path,
            db_path,
            public_api_address: None,
            private_api_address: None,
            master_key_pass: Some(FromStr::from_str("pass:").unwrap()),
        };

        let run = Run {
            standard: standard_run,
            ejb_port,
            artifacts_path: self.artifacts_path,
            ejb_log_config_path: self.ejb_log_config_path,
            ejb_override_java_library_path: None,
            jvm_debug: None,
            jvm_args_prepend: vec![],
            jvm_args_append: vec![],
        };

        run.execute()
    }
}

/// Returns full path to the default log configuration file assuming the `exonum-java` app is
/// packaged/installed.
fn get_path_to_default_log_config() -> PathBuf {
    let mut path = executable_directory();
    path.push("log4j-fallback.xml");
    path
}

/// Concatenates PathBuf and string. Useful to make a `PathBuf` to a file in the specific directory.
fn concat_path(first: PathBuf, second: &str) -> PathBuf {
    let mut path = first;
    path.push(second);
    path
}
