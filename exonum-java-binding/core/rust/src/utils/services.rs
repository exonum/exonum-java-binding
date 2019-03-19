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

use std::collections::HashMap;
use toml;

pub const PATH_TO_SERVICES_DEFINITION: &str = "ejb_app_services.toml";
pub const CONFIGURATION_SERVICE: &str = "configuration";
pub const BTC_ANCHORING_SERVICE: &str = "btc-anchoring";
pub const TIME_SERVICE: &str = "time";

#[derive(Serialize, Deserialize)]
pub struct EjbAppServices {
    pub system_services: Option<HashSet<String>>,
    pub user_services: HashMap<String, String>,
}

type Result<T> = std::result::Result<T, Box<std::error::Error>>;

/// Loads services definition from a specific TOML configuration file.
pub fn load_services_definition<P: AsRef<Path>>(path: P) -> Result<EjbAppServices> {
    let mut file = File::open(path)?;
    let mut toml = String::new();
    file.read_to_string(&mut toml)?;
    parse_services(toml)
}

// Produces the EjbAppServices structure from string representation in TOML format.
fn parse_services(data: String) -> Result<EjbAppServices> {
    let services: EjbAppServices =
        toml::from_str(&data).expect("Invalid format of the file with EJB services definition");
    Ok(services)
}

/// Determines whether particular system service is defined in the specific EJB services configuration file.
pub fn is_service_enabled_in_config_file<P: AsRef<Path>>(service_name: &str, path: P) -> bool {
    match load_services_definition(path) {
        Ok(services) => match services.system_services {
            Some(system_services) => system_services.contains(service_name),
            None => false,
        },
        Err(_) => false,
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::io::Write;
    use tempfile::{Builder, TempPath};

    #[test]
    fn missed_system_services_section() {
        let cfg = "[user_services]\nservice_name1 = '/path/to/artifact1'\nservice_name2 = '/path/to/artifact2'".to_owned();
        let res = parse_services(cfg);
        assert!(res.is_ok());
    }

    #[test]
    #[should_panic(expected = "Invalid format of the file with EJB services definition")]
    fn missed_user_services_section() {
        let cfg = "system_services = [\"configuration\", \"btc-anchoring\", \"time\"]".to_owned();
        let _result = parse_services(cfg);
    }

    #[test]
    fn empty_list() {
        let cfg = "system_services = []\n[user_services]".to_owned();
        let res = parse_services(cfg);
        assert!(res.is_ok());
        let services = res.unwrap();
        assert_eq!(services.system_services.unwrap().len(), 0);
        assert_eq!(services.user_services.len(), 0);
    }

    #[test]
    fn duplicated() {
        let cfg = "system_services = [\"btc-anchoring\", \"btc-anchoring\"]\n[user_services]\n"
            .to_owned();
        let res = parse_services(cfg);
        assert!(res.is_ok());
        let EjbAppServices {
            system_services, ..
        } = res.unwrap();
        assert!(system_services.is_some());
        let system_services = system_services.unwrap();
        assert_eq!(system_services.len(), 1);
        assert!(system_services.contains(BTC_ANCHORING_SERVICE));
    }

    #[test]
    #[should_panic(expected = "Invalid format of the file with EJB services definition")]
    fn broken_config() {
        let cfg = "wrong_format = 1".to_owned();
        let _result = parse_services(cfg);
    }

    #[test]
    fn no_config_file() {
        let res = load_services_definition("nonexistent");
        assert!(res.is_err());
    }

    #[test]
    fn config_file_ok() {
        let cfg = create_config(
            "services_list_ok.toml",
            "system_services = []\n[user_services]",
        );
        let res = load_services_definition(cfg);
        assert!(res.is_ok());
    }

    #[test]
    fn check_service_enabled() {
        let cfg = create_config(
            "service_enabled_test.toml",
            "system_services = [\"time\"]\n[user_services]\nservice_name1 = '/path/to/artifact1.jar'",
        );

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

    // Creates temporary config file
    fn create_config(filename: &str, cfg: &str) -> TempPath {
        let mut cfg_file = Builder::new().prefix(filename).tempfile().unwrap();
        writeln!(cfg_file, "{}", cfg).unwrap();
        cfg_file.into_temp_path()
    }
}
