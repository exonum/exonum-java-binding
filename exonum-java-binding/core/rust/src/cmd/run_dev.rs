use exonum_cli::command::{
    finalize::Finalize,
    generate_config::{GenerateConfig, PUB_CONFIG_FILE_NAME, SEC_CONFIG_FILE_NAME},
    generate_template::GenerateTemplate,
    run::Run as StandardRun,
    ExonumCommand,
};
use failure;
use structopt::StructOpt;

use std::{path::PathBuf, str::FromStr};

use {concat_path, EjbCommand, EjbCommandResult, Run};

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
