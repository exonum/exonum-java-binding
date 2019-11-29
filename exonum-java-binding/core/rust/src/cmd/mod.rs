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

use exonum::node::NodeConfig;
use exonum_cli::{
    command::{
        finalize::Finalize as StandardFinalize,
        generate_config::{
            GenerateConfig as StandardGenerateConfig, PUB_CONFIG_FILE_NAME, SEC_CONFIG_FILE_NAME,
        },
        generate_template::GenerateTemplate as StandardGenerateTemplate,
        maintenance::Maintenance,
        run::Run as StandardRun,
        ExonumCommand, StandardResult,
    },
    {
        config::{CommonConfigTemplate, SharedConfig},
        io::{load_config_file, save_config_file},
    },
};
use failure::{self, format_err};
use serde::{Deserialize, Serialize};
use structopt::StructOpt;

use std::path::Path;
use std::{path::PathBuf, str::FromStr};

use super::{executable_directory, Config, JvmConfig, RuntimeConfig};

mod supervisor_mode;
pub use self::supervisor_mode::SupervisorMode;

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

/// EJB-specific `generate-template` command. Takes an additional
/// `supervisor_mode` parameter and saves it in the generated config file.
#[derive(Debug, StructOpt, Serialize, Deserialize)]
#[structopt(rename_all = "kebab-case")]
pub struct GenerateTemplate {
    #[structopt(flatten)]
    #[serde(flatten)]
    standard: StandardGenerateTemplate,
    /// Supervisor service mode. Can be "simple" or "decentralized".
    #[structopt(long)]
    supervisor_mode: SupervisorMode,
}

/// EJB-specific `generate-config` command. Only does passing `supervisor_mode`
/// from common config to node public config.
#[derive(Debug, StructOpt, Serialize, Deserialize)]
pub struct GenerateConfig {
    #[structopt(flatten)]
    standard: StandardGenerateConfig,
}

/// EJB-specific `finalize` command. Only does passing `supervisor_mode` from
/// node public configs to final node configuration. Returns error in case
/// supervisor mode isn't equal in nodes public configs.
#[derive(Debug, StructOpt, Serialize, Deserialize)]
pub struct Finalize {
    #[structopt(flatten)]
    standard: StandardFinalize,
}

impl Finalize {
    /// Returns `SupervisorMode` from public configs. Returns error if
    /// `SupervisorMode` is not equal in every public config.
    fn supervisor_mode(&self) -> Result<SupervisorMode, failure::Error> {
        let mut public_configs: Vec<EjbSharedConfig> = Vec::new();
        for path in &self.standard.public_configs {
            public_configs.push(load_config_file(path)?);
        }
        let supervisor_mode = public_configs[0].supervisor_mode;

        if public_configs
            .iter()
            .any(|config| config.supervisor_mode != supervisor_mode)
        {
            return Err(format_err!("Different supervisor modes in public configs"));
        }

        Ok(supervisor_mode)
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
        let config_directory = concat_path(&self.blockchain_path, "config");
        let node_config_path = concat_path(&config_directory, "node.toml");

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
        let common_config_path = concat_path(&config_directory, "template.toml");
        let public_config_path = concat_path(&config_directory, PUB_CONFIG_FILE_NAME);
        let secret_config_path = concat_path(&config_directory, SEC_CONFIG_FILE_NAME);

        let generate_template = GenerateTemplate {
            standard: StandardGenerateTemplate {
                common_config: common_config_path.clone(),
                validators_count,
            },
            supervisor_mode: SupervisorMode::Simple,
        };
        generate_template.execute()?;

        let generate_config = GenerateConfig {
            standard: StandardGenerateConfig {
                common_config: common_config_path.clone(),
                output_dir: config_directory.clone(),
                peer_address,
                listen_address: None,
                no_password: true,
                master_key_pass: None,
                master_key_path: None,
            },
        };
        generate_config.execute()?;

        let finalize = Finalize {
            standard: StandardFinalize {
                secret_config_path,
                output_config_path: node_config_path.clone(),
                public_configs: vec![public_config_path],
                public_api_address: Some(public_api_address),
                private_api_address: Some(private_api_address),
                public_allow_origin: Some(public_allow_origin),
                private_allow_origin: Some(private_allow_origin),
            },
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

/// EJB-specific common (`template`) config format. Includes additional
/// `supervisor_mode` parameter.
#[derive(Debug, Serialize, Deserialize)]
struct EjbTemplateConfig {
    #[serde(flatten)]
    standard: CommonConfigTemplate,
    /// Mode of the Supervisor service.
    supervisor_mode: SupervisorMode,
}

impl EjbCommand for GenerateTemplate {
    fn execute(self) -> Result<EjbCommandResult, failure::Error> {
        if let StandardResult::GenerateTemplate {
            template_config_path,
        } = self.standard.execute()?
        {
            // Load standard config file, add supervisor_mode parameter,
            //   save the EJB-specific common config to the same file.
            let standard_config: CommonConfigTemplate = load_config_file(&template_config_path)?;
            let ejb_config = EjbTemplateConfig {
                standard: standard_config,
                supervisor_mode: self.supervisor_mode,
            };
            save_config_file(&ejb_config, &template_config_path)?;

            Ok(StandardResult::GenerateTemplate {
                template_config_path,
            }
            .into())
        } else {
            unreachable!("Standard generate-template command returned invalid result")
        }
    }
}

/// EJB-specific `SharedConfig` (node public config) with an additional
/// `supervisor_mode` parameter.
#[derive(Debug, Serialize, Deserialize)]
pub struct EjbSharedConfig {
    #[serde(flatten)]
    standard: SharedConfig,
    /// Mode of the Supervisor service.
    supervisor_mode: SupervisorMode,
}

impl EjbCommand for GenerateConfig {
    fn execute(self) -> Result<EjbCommandResult, failure::Error> {
        let ejb_template_config: EjbTemplateConfig =
            load_config_file(&self.standard.common_config)?;
        let supervisor_mode = ejb_template_config.supervisor_mode;

        if let StandardResult::GenerateConfig {
            public_config_path,
            secret_config_path,
            master_key_path,
        } = self.standard.execute()?
        {
            // Load standard public node config, add `supervisor_mode` parameter,
            //  save the modified config file.
            let public_config: SharedConfig = load_config_file(&public_config_path)?;
            let config = EjbSharedConfig {
                standard: public_config,
                supervisor_mode,
            };
            save_config_file(&config, &public_config_path)?;

            Ok(StandardResult::GenerateConfig {
                public_config_path,
                secret_config_path,
                master_key_path,
            }
            .into())
        } else {
            unreachable!("Standard generate-config command returned invalid result")
        }
    }
}

/// EJB-specific final node configuration. Includes additional `supervisor_mode`
/// parameter.
#[derive(Debug, Serialize, Deserialize)]
pub struct EjbNodeConfig {
    #[serde(flatten)]
    standard: NodeConfig,
    /// Mode of the Supervisor service.
    supervisor_mode: SupervisorMode,
}

impl EjbCommand for Finalize {
    fn execute(self) -> Result<EjbCommandResult, failure::Error> {
        let supervisor_mode = self.supervisor_mode()?;
        if let StandardResult::Finalize { node_config_path } = self.standard.execute()? {
            // Load standard node config file, add `supervisor_mode` parameter,
            //   save the modified file.
            let config = EjbNodeConfig {
                standard: load_config_file(&node_config_path)?,
                supervisor_mode,
            };
            save_config_file(&config, &node_config_path)?;

            Ok(StandardResult::Finalize { node_config_path }.into())
        } else {
            unreachable!("Standard finalize command returned invalid result")
        }
    }
}

impl EjbCommand for Run {
    fn execute(self) -> Result<EjbCommandResult, failure::Error> {
        let EjbNodeConfig {
            supervisor_mode, ..
        } = load_config_file(&self.standard.node_config)?;
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
                supervisor_mode,
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
fn concat_path<P: AsRef<Path>>(first: P, second: &str) -> PathBuf {
    let mut path = first.as_ref().to_owned();
    path.push(second);
    path
}

#[cfg(test)]
mod tests {
    use super::*;
    use exonum::blockchain::ValidatorKeys;
    use exonum_cli::config::NodePublicConfig;

    #[test]
    fn run_node_with_simple_supervisor() {
        run_node_with_supervisor(SupervisorMode::Simple).unwrap();
    }

    #[test]
    fn run_node_with_decentralized_supervisor() {
        run_node_with_supervisor(SupervisorMode::Decentralized).unwrap();
    }

    #[test]
    fn different_suprevisor_modes_in_public_configs() -> Result<(), failure::Error> {
        let pub_config_1 = ejb_shared_config(SupervisorMode::Simple);
        let pub_config_2 = ejb_shared_config(SupervisorMode::Decentralized);

        let testnet_dir = tempfile::tempdir()?;
        let pub_config_1_path = concat_path(testnet_dir.path(), "pub1.toml");
        let pub_config_2_path = concat_path(testnet_dir.path(), "pub2.toml");

        save_config_file(&pub_config_1, &pub_config_1_path)?;
        save_config_file(&pub_config_2, &pub_config_2_path)?;

        let finalize = Finalize {
            standard: StandardFinalize {
                secret_config_path: concat_path(testnet_dir.path(), "sec.toml"),
                output_config_path: concat_path(testnet_dir.path(), "node.toml"),
                public_configs: vec![pub_config_1_path, pub_config_2_path],
                public_api_address: None,
                private_api_address: None,
                public_allow_origin: None,
                private_allow_origin: None,
            },
        };
        let err = finalize.execute().err().unwrap();
        assert!(err
            .to_string()
            .contains("Different supervisor modes in public configs"));

        Ok(())
    }

    fn ejb_shared_config(supervisor_mode: SupervisorMode) -> EjbSharedConfig {
        EjbSharedConfig {
            standard: SharedConfig {
                common: Default::default(),
                node: NodePublicConfig {
                    address: "".to_string(),
                    validator_keys: ValidatorKeys {
                        consensus_key: Default::default(),
                        service_key: Default::default(),
                    },
                },
            },
            supervisor_mode,
        }
    }

    fn run_node_with_supervisor(supervisor_mode: SupervisorMode) -> Result<(), failure::Error> {
        let testnet_dir = tempfile::tempdir()?;

        let common_config_path = concat_path(testnet_dir.path(), "common.toml");

        let generate_template = GenerateTemplate {
            standard: StandardGenerateTemplate {
                common_config: common_config_path.clone(),
                validators_count: 1,
            },
            supervisor_mode,
        };
        generate_template.execute()?;

        let generate_config = GenerateConfig {
            standard: StandardGenerateConfig {
                common_config: common_config_path.clone(),
                output_dir: testnet_dir.path().to_owned(),
                peer_address: "127.0.0.1:5400".parse().unwrap(),
                listen_address: None,
                no_password: true,
                master_key_pass: None,
                master_key_path: None,
            },
        };
        let (public_config, secret_config) = match generate_config.execute()? {
            EjbCommandResult::Standard(StandardResult::GenerateConfig {
                public_config_path,
                secret_config_path,
                ..
            }) => (public_config_path, secret_config_path),
            _ => unreachable!("Invalid result of generate-config"),
        };

        let node_config_path = concat_path(testnet_dir.path(), "node.toml");

        let finalize = Finalize {
            standard: StandardFinalize {
                secret_config_path: secret_config,
                output_config_path: node_config_path.clone(),
                public_configs: vec![public_config],
                public_api_address: None,
                private_api_address: None,
                public_allow_origin: None,
                private_allow_origin: None,
            },
        };
        finalize.execute()?;

        let run = Run {
            standard: StandardRun {
                node_config: node_config_path.clone(),
                db_path: testnet_dir.path().to_owned(),
                public_api_address: None,
                private_api_address: None,
                master_key_pass: Some(FromStr::from_str("pass:")?),
            },
            ejb_port: 0,
            artifacts_path: testnet_dir.path().to_owned(),
            ejb_log_config_path: None,
            ejb_override_java_library_path: None,
            jvm_debug: None,
            jvm_args_prepend: vec![],
            jvm_args_append: vec![],
        };

        if let EjbCommandResult::EjbRun(config) = run.execute()? {
            assert_eq!(config.supervisor_mode, supervisor_mode);
        } else {
            unreachable!("Invalid result of run");
        }

        Ok(())
    }
}
