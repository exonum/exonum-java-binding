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

use java_bindings::{
    Command, EjbCommand, EjbCommandResult,
};

use java_bindings::exonum::blockchain::{BlockchainBuilder, InstanceCollection};
use java_bindings::exonum::exonum_merkledb::{Database, RocksDB};
use java_bindings::exonum::node::{ApiSender, Node, NodeChannel};
use java_bindings::exonum::runtime::rust::ServiceFactory;
use std::sync::Arc;

pub fn run_node(command: Command) -> Result<(), failure::Error> {
    if let EjbCommandResult::EjbRun(config) = command.execute()? {
        let node_config = config.run_config.node_config;
        let service_factories = standard_exonum_service_factories();
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

fn standard_exonum_service_factories() -> Vec<Box<dyn ServiceFactory>> {
    // TODO: add anchoring & time services
    vec![]
}