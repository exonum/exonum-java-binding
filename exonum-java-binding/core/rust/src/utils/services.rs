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

/// Names of system services to refer in the services configuration file.
pub mod system_service_names {
    /// Exonum configuration service
    pub const CONFIGURATION_SERVICE: &str = "configuration";
    /// Exonum BTC anchoring service
    pub const BTC_ANCHORING_SERVICE: &str = "btc-anchoring";
    /// Exonum time oracle service
    pub const TIME_SERVICE: &str = "time";
}

/// Path to the services definition file.
pub const PATH_TO_SERVICES_DEFINITION: &str = "ejb_app_services.toml";

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
    use self::system_service_names::*;
    use super::*;
    use std::io::Write;
    use tempfile::{Builder, TempPath};

    #[test]
    fn parse_services_missed_system_services_section() {
        let cfg = r#"
            [user_services]
            service_name1 = "/path/to/artifact1"
            service_name2 = "/path/to/artifact2"
        "#
        .to_owned();
        let res = parse_services(cfg);
        assert!(res.is_ok());
    }

    #[test]
    #[should_panic(expected = "Invalid format of the file with EJB services definition")]
    fn parse_services_missed_user_services_section() {
        let cfg = r#"
            system_services = ["configuration", "btc-anchoring", "time"]
        "#
        .to_owned();
        let _result = parse_services(cfg);
    }

    #[test]
    fn parse_services_empty_list() {
        let cfg = r#"
            system_services = []
            [user_services]
        "#
        .to_owned();
        let res = parse_services(cfg);
        assert!(res.is_ok());
        let services = res.unwrap();
        assert_eq!(services.system_services.unwrap().len(), 0);
        assert_eq!(services.user_services.len(), 0);
    }

    #[test]
    fn parse_services_duplicated_system_service() {
        let cfg = r#"
            system_services = ["btc-anchoring", "btc-anchoring"]
            [user_services]
        "#
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
    fn parse_services_duplicated_user_service() {
        let cfg = r#"
            [user_services]
            service_name = "/path/to/artifact"
            service_name = "/path/to/artifact"
        "#
        .to_owned();
        let res = parse_services(cfg);
        assert!(res.is_ok());
        let EjbAppServices { user_services, .. } = res.unwrap();
        assert_eq!(user_services.len(), 1);
        assert_eq!(user_services["service_name"], "/path/to/artifact");
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
        let cfg_path = create_config(
            r#"
                system_services = []
                [user_services]
            "#,
        );
        let res = load_services_definition(cfg_path);
        assert!(res.is_ok());
    }

    #[test]
    fn check_service_enabled() {
        let cfg_path = create_config(
            r#"
                system_services = ["time"]
                [user_services]
                service_name1 = "/path/to/artifact1.jar"
            "#,
        );

        assert!(!is_service_enabled_in_config_file(
            CONFIGURATION_SERVICE,
            &cfg_path
        ));
        assert!(!is_service_enabled_in_config_file(
            BTC_ANCHORING_SERVICE,
            &cfg_path
        ));
        assert!(is_service_enabled_in_config_file(TIME_SERVICE, &cfg_path));
    }

    // Creates temporary config file.
    fn create_config(cfg: &str) -> TempPath {
        let mut cfg_file = Builder::new().tempfile().unwrap();
        writeln!(cfg_file, "{}", cfg).unwrap();
        cfg_file.into_temp_path()
    }
}
