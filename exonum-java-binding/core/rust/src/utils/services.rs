// Copyright 2019 The Exonum Team
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

use std::{collections::HashSet, fs::File, io::Read, path::Path};

use toml;

pub const PATH_TO_SERVICES_TO_ENABLE: &str = "ejb_app_services.toml";
pub const CONFIGURATION_SERVICE: &str = "configuration";
pub const BTC_ANCHORING_SERVICE: &str = "btc-anchoring";
pub const TIME_SERVICE: &str = "time";
pub const EJB_SERVICE: &str = "ejb-service";

#[derive(Serialize, Deserialize)]
struct ServicesToEnable {
    services: HashSet<String>,
}

type Result<T> = std::result::Result<T, Box<std::error::Error>>;

/// Loads service names from a specific TOML configuration file
pub fn load_enabled_services<P: AsRef<Path>>(path: P) -> Result<HashSet<String>> {
    let mut file = File::open(path)?;
    let mut toml = String::new();
    file.read_to_string(&mut toml)?;
    let ServicesToEnable { services } =
        toml::from_str(&toml).expect("Invalid list of services to enable");
    Ok(services)
}

/// Determines whether particular service name is defined in the specific TOML configuration file.
pub fn is_service_enabled_in_config_file<P: AsRef<Path>>(service_name: &str, path: P) -> bool {
    load_enabled_services(path)
        .map(|services| services.contains(service_name))
        .unwrap_or(false)
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::io::Write;
    use tempfile::{Builder, TempPath};

    #[test]
    fn no_config() {
        let res = load_enabled_services("nonexistent");
        assert!(res.is_err());
    }

    #[test]
    fn all_services() {
        let cfg = create_config(
            "all_services_test.toml",
            "services = [\"ejb-service\", \"configuration\", \"btc-anchoring\", \"time\"]",
        );

        let res = load_enabled_services(cfg);
        assert!(res.is_ok());
        let services_to_enable = res.unwrap();
        assert_eq!(services_to_enable.len(), 4);
        assert!(services_to_enable.contains(EJB_SERVICE));
        assert!(services_to_enable.contains(CONFIGURATION_SERVICE));
        assert!(services_to_enable.contains(BTC_ANCHORING_SERVICE));
        assert!(services_to_enable.contains(TIME_SERVICE));
    }

    #[test]
    fn empty_list() {
        let cfg = create_config("empty_list.toml", "services = []");
        let res = load_enabled_services(cfg);
        assert!(res.is_ok());
        let services_to_enable = res.unwrap();
        assert_eq!(services_to_enable.len(), 0);
    }

    #[test]
    fn check_service_enabled() {
        let cfg = create_config(
            "service_enabled_test.toml",
            "services = [\"ejb-service\", \"time\"]",
        );

        assert!(is_service_enabled_in_config_file(EJB_SERVICE, &cfg));
        assert!(!is_service_enabled_in_config_file(
            CONFIGURATION_SERVICE,
            &cfg
        ));
        assert!(!is_service_enabled_in_config_file(
            BTC_ANCHORING_SERVICE,
            &cfg
        ));
        assert!(is_service_enabled_in_config_file(TIME_SERVICE, &cfg));
    }

    fn create_config(filename: &str, cfg: &str) -> TempPath {
        let mut cfg_file = Builder::new().prefix(filename).tempfile().unwrap();
        writeln!(cfg_file, "{}", cfg).unwrap();
        cfg_file.into_temp_path()
    }
}
