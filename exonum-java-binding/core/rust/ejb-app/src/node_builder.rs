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
use java_bindings::exonum::helpers::fabric::{self, ServiceFactory};
use java_bindings::utils::services::{
    load_services_definition, EjbAppServices, BTC_ANCHORING_SERVICE, CONFIGURATION_SERVICE,
    PATH_TO_SERVICES_DEFINITION, TIME_SERVICE,
};
use java_bindings::JavaServiceFactoryAdapter;

use std::collections::{HashMap, HashSet};

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

pub fn create() -> fabric::NodeBuilder {
    let EjbAppServices {
        system_services,
        user_services,
    } = load_services_definition(PATH_TO_SERVICES_DEFINITION)
        .expect("Unable to load services definition");

    let services = validate_services(system_services);

    let mut service_factories = service_factories();

    let mut builder = fabric::NodeBuilder::new();
    for service_name in &services {
        match service_factories.remove(service_name) {
            Some(factory) => {
                builder = builder.with_service(factory);
            }
            None => panic!("Found unknown service name {}", service_name),
        }
    }

    for (name, artifact_path) in user_services {
        builder = builder.with_service(Box::new(JavaServiceFactoryAdapter::new(
            name,
            artifact_path,
        )));
    }

    builder
}

// Extracts defined services or inserts the configuration service otherwise.
fn validate_services(services: Option<HashSet<String>>) -> HashSet<String> {
    services.unwrap_or_else(|| {
        let mut services = HashSet::new();
        services.insert(CONFIGURATION_SERVICE.to_owned());
        services
    })
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::io::Write;
    use tempfile::{Builder, TempPath};

    #[test]
    fn validate_services_empty() {
        let services = validate_services(None);
        assert_eq!(services.len(), 1);
        assert!(services.contains(CONFIGURATION_SERVICE));
    }

    #[test]
    fn validate_services_ok() {
        let mut services = HashSet::new();
        services.insert(BTC_ANCHORING_SERVICE.to_owned());
        services.insert(TIME_SERVICE.to_owned());
        let services = validate_services(Some(services));
        assert_eq!(services.len(), 2);
        assert!(services.contains(BTC_ANCHORING_SERVICE));
        assert!(services.contains(TIME_SERVICE));
    }

    #[test]
    fn all_services() {
        let cfg = create_config(
            "all.toml",
            "system_services = [\"configuration\", \"btc-anchoring\", \"time\"]\n\
            [user_services]\nservice_name1 = '/path/to/artifact1'\nservice_name2 = '/path/to/artifact2'",
        );
        let EjbAppServices {
            system_services,
            user_services,
        } = load_services_definition(cfg).unwrap();
        let system_services = system_services.unwrap();
        assert_eq!(system_services.len(), 3);
        assert!(system_services.contains(CONFIGURATION_SERVICE));
        assert!(system_services.contains(BTC_ANCHORING_SERVICE));
        assert!(system_services.contains(TIME_SERVICE));

        let service_factories = service_factories();
        for service in system_services {
            assert!(service_factories.get(&service).is_some())
        }

        assert_eq!(user_services.len(), 2);
        assert_eq!(&user_services["service_name1"], "/path/to/artifact1");
        assert_eq!(&user_services["service_name2"], "/path/to/artifact2");
    }

    // Creates temporary config file
    fn create_config(filename: &str, cfg: &str) -> TempPath {
        let mut cfg_file = Builder::new().prefix(filename).tempfile().unwrap();
        writeln!(cfg_file, "{}", cfg).unwrap();
        cfg_file.into_temp_path()
    }
}
