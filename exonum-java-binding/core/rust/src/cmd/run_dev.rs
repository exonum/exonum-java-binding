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

use anyhow;
use exonum_cli::command::{ExonumCommand, RunDev as StandardRunDev, StandardResult};
use serde::{Deserialize, Serialize};
use structopt::StructOpt;

use std::path::PathBuf;

use crate::{
    get_path_to_default_log_config, Config, EjbCommand, EjbCommandResult, JvmConfig, RuntimeConfig,
};

/// EJB-specific `run-dev` command.
///
/// Automatically generates node configuration for one
/// validator and runs it using provided `artifacts_path` as a directory for Java service artifacts.
#[derive(Debug, StructOpt, Serialize, Deserialize)]
#[structopt(rename_all = "kebab-case")]
pub struct RunDev {
    #[structopt(flatten)]
    #[serde(flatten)]
    standard: StandardRunDev,
    /// Path to the directory containing Java service artifacts.
    #[structopt(long)]
    artifacts_path: PathBuf,
    /// Path to log4j configuration file.
    #[structopt(long)]
    ejb_log_config_path: Option<PathBuf>,
}

impl EjbCommand for RunDev {
    fn execute(self) -> Result<EjbCommandResult, anyhow::Error> {
        if let StandardResult::Run(node_run_config) = self.standard.execute()? {
            let jvm_config = JvmConfig {
                args_prepend: Vec::new(),
                args_append: Vec::new(),
                jvm_debug_socket: None,
            };

            let log_config_path = self
                .ejb_log_config_path
                .unwrap_or_else(get_path_to_default_log_config);

            let runtime_config = RuntimeConfig {
                artifacts_path: self.artifacts_path,
                log_config_path,
                port: 6400,
                override_system_lib_path: None,
            };

            let config = Config {
                run_config: *node_run_config,
                jvm_config,
                runtime_config,
            };

            Ok(EjbCommandResult::EjbRun(config))
        } else {
            panic!()
        }
    }
}
