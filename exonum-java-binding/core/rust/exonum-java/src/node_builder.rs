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

//use exonum_btc_anchoring::ServiceFactory as BtcAnchoringServiceFactory;
//use exonum_time::TimeServiceFactory;
use java_bindings::{
    services::{
        load_services_definition, EjbAppServices,
        PATH_TO_SERVICES_DEFINITION,
    },
    Command, EjbCommand, EjbCommandResult,
};

use java_bindings::exonum::blockchain::{BlockchainBuilder, InstanceCollection};
use java_bindings::exonum::exonum_merkledb::{Database, RocksDB};
use java_bindings::exonum::node::{ApiSender, Node, NodeChannel};
use java_bindings::exonum::runtime::rust::ServiceFactory;
use std::path::Path;
use std::sync::Arc;

pub fn run_node(command: Command) -> Result<(), failure::Error> {
    if let EjbCommandResult::EjbRun(config) = command.execute()? {
        let node_config = config.run_config.node_config;
        let service_factories = prepare_service_factories(PATH_TO_SERVICES_DEFINITION);
        let channel = NodeChannel::new(&node_config.mempool.events_pool_capacity);
        let database = Arc::new(RocksDB::open(
            config.run_config.db_path,
            &node_config.database,
        )?) as Arc<dyn Database>;

        let keypair = node_config.service_keypair();
        let api_sender = ApiSender::new(channel.api_requests.0.clone());
        let internal_requests = channel.internal_requests.0.clone();

        let blockchain = BlockchainBuilder::new(database, node_config.consensus.clone(), keypair)
            .with_rust_runtime(service_factories.into_iter().map(InstanceCollection::new))
            .finalize(api_sender, internal_requests)?;
        let node_config_path = config
            .run_config
            .node_config_path
            .to_str()
            .unwrap()
            .to_owned();
        let node = Node::with_blockchain(blockchain, channel, node_config, Some(node_config_path));
        node.run()
    } else {
        Ok(())
    }
}

// Prepares vector of `ServiceFactory` from services configuration file located at given path.
// Panics in case of problems with reading/validating of service configuration file.
fn prepare_service_factories<P: AsRef<Path>>(path: P) -> Vec<Box<dyn ServiceFactory>> {
    // Read services definition from file.
    let EjbAppServices {
        system_services,
        user_services,
    } = load_services_definition(path).expect("Unable to load services definition");

    let system_services = system_services.unwrap_or_default();

    // Prepare list of service factories from system and user services
    let mut resulting_factories = Vec::new();

    // Process system services first
    for service_name in system_services {
        let factory = system_service_factory_for_name(&service_name);
        resulting_factories.push(factory);
    }

    resulting_factories
}

fn system_service_factory_for_name(name: &str) -> Box<dyn ServiceFactory> {
    match name {
        //        BTC_ANCHORING_SERVICE => Box::new(BtcAnchoringServiceFactory) as Box<dyn ServiceFactory>,
        //        TIME_SERVICE => Box::new(TimeServiceFactory) as Box<dyn ServiceFactory>,
        _ => panic!("Unknown system service name \"{}\" has been found", name),
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use java_bindings::services::system_service_names::*;
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
        assert!(contains_service(CONFIGURATION_SERVICE, &factories));
        assert!(contains_service("service_name", &factories));
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
        assert!(contains_service(CONFIGURATION_SERVICE, &factories));
        assert!(contains_service(BTC_ANCHORING_SERVICE, &factories));
        assert!(contains_service(TIME_SERVICE, &factories));
        assert!(contains_service("service_name1", &factories));
        assert!(contains_service("service_name2", &factories));
    }

    #[test]
    fn system_service_factory_for_name_ok() {
        assert_eq!(
            "configuration",
            system_service_factory_for_name(CONFIGURATION_SERVICE)
                .artifact_id()
                .name
        );
        assert_eq!(
            "btc_anchoring",
            system_service_factory_for_name(BTC_ANCHORING_SERVICE)
                .artifact_id()
                .name
        );
        assert_eq!(
            "exonum_time",
            system_service_factory_for_name(TIME_SERVICE)
                .artifact_id()
                .name
        );
    }

    #[test]
    #[should_panic(expected = "Unknown system service name \"service_unknown\" has been found")]
    fn system_service_factory_for_name_unknown() {
        system_service_factory_for_name("service_unknown");
    }

    // Creates temporary config file.
    fn create_config(cfg: &str) -> TempPath {
        let mut cfg_file = Builder::new().tempfile().unwrap();
        writeln!(cfg_file, "{}", cfg).unwrap();
        cfg_file.into_temp_path()
    }

    // Checks whether particular service is presented in the given collection.
    fn contains_service(service_name: &str, factories: &[Box<dyn ServiceFactory>]) -> bool {
        let name = if service_name == CONFIGURATION_SERVICE
            || service_name == BTC_ANCHORING_SERVICE
            || service_name == TIME_SERVICE
        {
            system_service_factory_for_name(service_name)
                .artifact_id()
                .name
        } else {
            service_name.to_owned()
        };

        factories
            .iter()
            .any(|factory| factory.artifact_id().name == name)
    }
}
