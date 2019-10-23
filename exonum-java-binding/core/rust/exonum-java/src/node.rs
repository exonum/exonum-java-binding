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

use exonum_time::{time_provider::SystemTimeProvider, TimeServiceFactory};
use java_bindings::{
    create_java_vm, create_service_runtime,
    exonum::{
        blockchain::{Blockchain, BlockchainBuilder, InstanceCollection},
        exonum_merkledb::{Database, RocksDB},
        node::{ApiSender, Node, NodeChannel},
        runtime::rust::ServiceFactory,
    },
    Command, Config, EjbCommand, EjbCommandResult, Executor, InternalConfig, JavaRuntimeProxy,
};

use std::sync::Arc;

pub fn run_node(command: Command) -> Result<(), failure::Error> {
    if let EjbCommandResult::EjbRun(config) = command.execute()? {
        let node = create_node(config)?;
        node.run()
    } else {
        Ok(())
    }
}

fn create_node(config: Config) -> Result<Node, failure::Error> {
    let node_config = config.run_config.node_config.clone();
    let events_pool_capacity = &node_config.mempool.events_pool_capacity;
    let channel = NodeChannel::new(events_pool_capacity);
    let blockchain = create_blockchain(&config, &channel)?;

    let node_config_path = config
        .run_config
        .node_config_path
        .to_str()
        .expect("Cannot convert node_config_path to String")
        .to_owned();

    Ok(Node::with_blockchain(
        blockchain,
        channel,
        node_config,
        Some(node_config_path),
    ))
}

fn create_blockchain(config: &Config, channel: &NodeChannel) -> Result<Blockchain, failure::Error> {
    let node_config = &config.run_config.node_config;
    let service_factories = standard_exonum_service_factories();
    let database = create_database(config)?;
    let keypair = node_config.service_keypair();
    let api_sender = ApiSender::new(channel.api_requests.0.clone());
    let internal_requests = channel.internal_requests.0.clone();

    let java_runtime = create_java_runtime(&config);

    BlockchainBuilder::new(database, node_config.consensus.clone(), keypair)
        .with_additional_runtime(java_runtime)
        .with_rust_runtime(service_factories.into_iter().map(InstanceCollection::new))
        .finalize(api_sender, internal_requests)
}

fn create_java_runtime(config: &Config) -> JavaRuntimeProxy {
    let executor = Executor::new(Arc::new(create_java_vm(
        &config.jvm_config,
        &config.runtime_config,
        InternalConfig::default(),
    )));

    create_service_runtime(executor, &config.runtime_config)
}

fn create_database(config: &Config) -> Result<Arc<dyn Database>, failure::Error> {
    let database = Arc::new(RocksDB::open(
        &config.run_config.db_path,
        &config.run_config.node_config.database,
    )?) as Arc<dyn Database>;
    Ok(database)
}

fn standard_exonum_service_factories() -> Vec<Box<dyn ServiceFactory>> {
    // TODO(ECR-3714): add anchoring service
    vec![Box::new(TimeServiceFactory::with_provider(
        SystemTimeProvider,
    ))]
}
