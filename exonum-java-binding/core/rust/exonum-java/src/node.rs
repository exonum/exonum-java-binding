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

use exonum_explorer_service::ExplorerFactory;
use exonum_supervisor::{mode::Mode as SupervisorMode, Supervisor};
use exonum_time::TimeServiceFactory;
use java_bindings::{
    create_java_vm, create_service_runtime,
    exonum::{
        blockchain::{
            config::{GenesisConfigBuilder, InstanceInitParams},
            Blockchain, BlockchainBuilder, BlockchainMut,
        },
        exonum_merkledb::{Database, RocksDB},
        node::{ApiSender, Node, NodeChannel, NodeConfig as CoreNodeConfig},
        runtime::rust::{DefaultInstance, RustRuntime, RustRuntimeBuilder, ServiceFactory},
    },
    Command, Config, DefaultConfigManager, EjbCommand, EjbCommandResult, Executor, InternalConfig,
    JavaRuntimeProxy,
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
    let events_pool_capacity = &node_config.private_config.mempool.events_pool_capacity;
    let channel = NodeChannel::new(events_pool_capacity);
    let blockchain = create_blockchain(&config, &channel)?;

    let config_manager = DefaultConfigManager::new(config.run_config.node_config_path);

    Ok(Node::with_blockchain(
        blockchain,
        channel,
        node_config.into(),
        Some(Box::new(config_manager)),
    ))
}

fn create_blockchain(
    config: &Config,
    channel: &NodeChannel,
) -> Result<BlockchainMut, failure::Error> {
    let node_config: CoreNodeConfig = config.run_config.node_config.clone().into();
    let database = create_database(config)?;
    let keypair = node_config.service_keypair();
    let api_sender = ApiSender::new(channel.api_requests.0.clone());

    let blockchain = Blockchain::new(database, keypair, api_sender);

    let supervisor_service = supervisor_service(&config);
    let genesis_config = GenesisConfigBuilder::with_consensus_config(node_config.consensus)
        .with_artifact(Supervisor.artifact_id())
        .with_instance(supervisor_service)
        .with_artifact(ExplorerFactory.artifact_id())
        .with_instance(ExplorerFactory.default_instance())
        .build();

    let rust_runtime = create_rust_runtime(channel);
    let java_runtime = create_java_runtime(&config);

    Ok(BlockchainBuilder::new(blockchain, genesis_config)
        .with_runtime(rust_runtime)
        .with_runtime(java_runtime)
        .build())
}

fn create_rust_runtime(channel: &NodeChannel) -> RustRuntime {
    RustRuntimeBuilder::new()
        .with_factory(TimeServiceFactory::default())
        .with_factory(Supervisor)
        .with_factory(ExplorerFactory)
        .build(channel.endpoints.0.clone())
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
        &config.run_config.node_config.private_config.database,
    )?) as Arc<dyn Database>;
    Ok(database)
}

fn supervisor_service(config: &Config) -> InstanceInitParams {
    let mode = &config
        .run_config
        .node_config
        .public_config
        .general
        .supervisor_mode;
    match *mode {
        SupervisorMode::Simple => Supervisor::simple(),
        SupervisorMode::Decentralized => Supervisor::decentralized(),
    }
}
