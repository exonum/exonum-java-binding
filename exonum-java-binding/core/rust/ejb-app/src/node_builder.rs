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
use java_bindings::utils::{
    load_enabled_services, BTC_ANCHORING_SERVICE, CONFIGURATION_SERVICE, EJB_SERVICE,
    PATH_TO_SERVICES_TO_ENABLE, TIME_SERVICE,
};
use java_bindings::JavaServiceFactory;

use std::collections::{HashMap, HashSet};
use std::path::Path;

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
    service_factories.insert(
        EJB_SERVICE.to_owned(),
        Box::new(JavaServiceFactory) as Box<ServiceFactory>,
    );
    service_factories
}

#[doc(hidden)]
pub fn services_to_enable<P: AsRef<Path>>(path: P) -> HashSet<String> {
    // Return default list if config file not found.
    let mut services = load_enabled_services(path).unwrap_or_else(|_| {
        let mut services = HashSet::new();
        services.insert(CONFIGURATION_SERVICE.to_owned());
        services
    });

    // Add EJB_SERVICE if it's missing
    services.insert(EJB_SERVICE.to_owned());

    services
}

pub fn create() -> fabric::NodeBuilder {
    let services = services_to_enable(PATH_TO_SERVICES_TO_ENABLE);
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
    builder
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::io::Write;
    use tempfile::{Builder, TempPath};

    fn create_config(filename: &str, cfg: &str) -> TempPath {
        let mut cfg_file = Builder::new().prefix(filename).tempfile().unwrap();
        writeln!(cfg_file, "{}", cfg).unwrap();
        cfg_file.into_temp_path()
    }

    #[test]
    fn no_config() {
        let services_to_enable = services_to_enable("");
        assert_eq!(services_to_enable.len(), 2);
        assert!(services_to_enable.contains(EJB_SERVICE));
        assert!(services_to_enable.contains(CONFIGURATION_SERVICE));
    }

    #[test]
    fn empty_list() {
        let cfg = create_config("empty_list.toml", "services = []");
        let services_to_enable = services_to_enable(cfg);
        assert_eq!(services_to_enable.len(), 1);
        assert!(services_to_enable.contains(EJB_SERVICE));
    }

    #[test]
    fn duplicated() {
        let cfg = create_config(
            "duplicated.toml",
            "services = [\"btc-anchoring\", \"btc-anchoring\"]",
        );
        let services_to_enable = services_to_enable(cfg);
        assert_eq!(services_to_enable.len(), 2);
        assert!(services_to_enable.contains(EJB_SERVICE));
        assert!(services_to_enable.contains(BTC_ANCHORING_SERVICE));
    }

    #[test]
    #[should_panic(expected = "Invalid list of services to enable")]
    fn broken_config() {
        let cfg = create_config("broken.toml", "not_list = 1");
        let _services_to_enable = services_to_enable(cfg);
    }

    #[test]
    fn with_anchoring() {
        let cfg = create_config("anchoring.toml", "services = [\"btc-anchoring\"]");
        let services_to_enable = services_to_enable(cfg);
        assert_eq!(services_to_enable.len(), 2);
        assert!(services_to_enable.contains(EJB_SERVICE));
        assert!(services_to_enable.contains(BTC_ANCHORING_SERVICE));
    }

    #[test]
    fn all_services() {
        let cfg = create_config(
            "all.toml",
            "services = [\"configuration\", \"btc-anchoring\", \"time\"]",
        );
        let services_to_enable = services_to_enable(cfg);
        assert_eq!(services_to_enable.len(), 4);
        assert!(services_to_enable.contains(EJB_SERVICE));
        assert!(services_to_enable.contains(CONFIGURATION_SERVICE));
        assert!(services_to_enable.contains(BTC_ANCHORING_SERVICE));
        assert!(services_to_enable.contains(TIME_SERVICE));

        let service_factories = service_factories();
        for service in &services_to_enable {
            assert!(service_factories.get(service).is_some())
        }
    }
}
