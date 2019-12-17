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

use exonum_supervisor::Supervisor;
use exonum_time::{time_provider::SystemTimeProvider, TimeServiceFactory};
use java_bindings::{
    create_java_vm, create_service_runtime,
    exonum::{
        blockchain::{
            config::{GenesisConfigBuilder, InstanceInitParams},
            Blockchain, BlockchainBuilder, BlockchainMut,
        },
        exonum_merkledb::{Database, RocksDB},
        node::{ApiSender, Node, NodeChannel},
        runtime::rust::{DefaultInstance, RustRuntime, ServiceFactory},
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

fn create_blockchain(
    config: &Config,
    channel: &NodeChannel,
) -> Result<BlockchainMut, failure::Error> {
    let node_config = &config.run_config.node_config;
    let database = create_database(config)?;
    let keypair = node_config.service_keypair();
    let api_sender = ApiSender::new(channel.api_requests.0.clone());

    let blockchain = Blockchain::new(database, keypair, api_sender);

    let supervisor_service = supervisor_service();
    let genesis_config = GenesisConfigBuilder::with_consensus_config(node_config.consensus.clone())
        .with_artifact(Supervisor.artifact_id())
        .with_instance(supervisor_service)
        .build();

    let rust_runtime = create_rust_runtime(channel);
    let java_runtime = create_java_runtime(&config);

    BlockchainBuilder::new(blockchain, genesis_config)
        .with_runtime(rust_runtime)
        .with_runtime(java_runtime)
        .build()
}

fn create_rust_runtime(channel: &NodeChannel) -> RustRuntime {
    let service_factories = standard_exonum_service_factories();
    service_factories.into_iter().fold(
        RustRuntime::new(channel.endpoints.0.clone()),
        |runtime, factory| runtime.with_factory(factory),
    )
}

fn create_java_runtime(config: &Config) -> JavaRuntimeProxy {
    let executor = Executor::new(Arc::new(create_java_vm(
        &config.jvm_config,
        &config.runtime_config,
        InternalConfig::app_config(),
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
    vec![
        Box::new(TimeServiceFactory::with_provider(SystemTimeProvider)),
        Box::new(Supervisor),
    ]
}

fn supervisor_service() -> InstanceInitParams {
    Supervisor::simple()
}
