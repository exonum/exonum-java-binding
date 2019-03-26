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

use exonum_btc_anchoring::ServiceFactory as BtcAnchoringServiceFactory;
use exonum_configuration::ServiceFactory as ConfigurationServiceFactory;
use exonum_time::TimeServiceFactory;
use java_bindings::{
    exonum::helpers::fabric::{self, ServiceFactory},
    utils::services::{
        load_services_definition, system_service_names::*, EjbAppServices,
        PATH_TO_SERVICES_DEFINITION,
    },
    JavaServiceFactoryAdapter,
};

use std::{
    collections::{HashMap, HashSet},
    path::Path,
};

/// Creates `NodeBuilder` using services configuration from `ejb_app_services.toml` located in the working directory.
pub fn create() -> fabric::NodeBuilder {
    let service_factories = prepare_service_factories(PATH_TO_SERVICES_DEFINITION);
    let mut builder = fabric::NodeBuilder::new();
    for (_, service_factory) in service_factories {
        builder = builder.with_service(service_factory);
    }
    builder
}

// Prepares vector of `ServiceFactory` from services configuration file located at given path.
// Panics in case of problems with reading/validating of service configuration file.
fn prepare_service_factories<P: AsRef<Path>>(path: P) -> HashMap<String, Box<dyn ServiceFactory>> {
    // Read services definition from file.
    let EjbAppServices {
        system_services,
        user_services,
    } = load_services_definition(path).expect("Unable to load services definition");

    // Make sure there is at least one user service defined.
    if user_services.is_empty() {
        panic!("At least one user service should be defined in the \"ejb_app_services.toml\" file");
    }

    // Check whether we have system services defined or insert the configuration service otherwise.
    let system_services = system_services.unwrap_or_else(|| {
        let mut services = HashSet::new();
        services.insert(CONFIGURATION_SERVICE.to_owned());
        services
    });

    // Prepare list of service factories from system and user services
    let mut resulting_factories = HashMap::new();

    // Process system services first
    let mut all_system_service_factories = service_factories();
    for service_name in system_services {
        // Move factory to the resulting map
        match all_system_service_factories.remove(&service_name) {
            Some(factory) => {
                resulting_factories.insert(service_name, factory);
            }
            None => panic!(
                "Unknown system service name {} has been found",
                service_name
            ),
        }
    }

    // Process user services
    for (name, artifact_path) in user_services {
        resulting_factories.insert(
            name.clone(),
            Box::new(JavaServiceFactoryAdapter::new(name, artifact_path)),
        );
    }

    resulting_factories
}

// Returns map of all system service factories
fn service_factories() -> HashMap<String, Box<ServiceFactory>> {
    let mut service_factories = HashMap::new();
    service_factories.insert(
        CONFIGURATION_SERVICE.to_owned(),
        Box::new(ConfigurationServiceFactory) as Box<ServiceFactory>,
    );
    service_factories.insert(
        BTC_ANCHORING_SERVICE.to_owned(),
        Box::new(BtcAnchoringServiceFactory) as Box<ServiceFactory>,
    );
    service_factories.insert(
        TIME_SERVICE.to_owned(),
        Box::new(TimeServiceFactory) as Box<ServiceFactory>,
    );
    service_factories
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::io::Write;
    use tempfile::{Builder, TempPath};

    #[test]
    #[should_panic(expected = "Unable to load services definition")]
    fn prepare_service_factories_nonexistent_file() {
        prepare_service_factories("nonexistent");
    }

    #[test]
    #[should_panic(expected = "At least one user service should be defined")]
    fn prepare_service_factories_missing_user_services() {
        let cfg_path = create_config(
            r#"
                system_services = ["configuration", "btc-anchoring", "time"]
                [user_services]
             "#,
        );
        prepare_service_factories(cfg_path);
    }

    #[test]
    #[should_panic(expected = "Unknown system service name")]
    fn prepare_service_factories_unknown_system_service() {
        let cfg_path = create_config(
            r#"
                system_services = ["configuration", "time", "unknown"]
                [user_services]
                service_name = "/path/to/artifact"
             "#,
        );
        prepare_service_factories(cfg_path);
    }

    #[test]
    fn prepare_service_factories_missing_system_services() {
        let cfg_path = create_config(
            r#"
                [user_services]
                service_name = "/path/to/artifact"
             "#,
        );

        let factories = prepare_service_factories(cfg_path);
        assert_eq!(factories.len(), 2);
        assert!(factories.contains_key(CONFIGURATION_SERVICE));
        assert!(factories.contains_key("service_name"));
    }

    #[test]
    fn prepare_service_factories_all_system_plus_user_services() {
        let cfg_path = create_config(
            r#"
                system_services = ["configuration", "btc-anchoring", "time"]
                [user_services]
                service_name1 = "/path/to/artifact1"
                service_name2 = "/path/to/artifact2"
             "#,
        );

        let factories = prepare_service_factories(cfg_path);
        assert_eq!(factories.len(), 5);
        assert!(factories.contains_key(CONFIGURATION_SERVICE));
        assert!(factories.contains_key(BTC_ANCHORING_SERVICE));
        assert!(factories.contains_key(TIME_SERVICE));
        assert!(factories.contains_key("service_name1"));
        assert!(factories.contains_key("service_name2"));
    }

    // Creates temporary config file.
    fn create_config(cfg: &str) -> TempPath {
        let mut cfg_file = Builder::new().tempfile().unwrap();
        writeln!(cfg_file, "{}", cfg).unwrap();
        cfg_file.into_temp_path()
    }
}
