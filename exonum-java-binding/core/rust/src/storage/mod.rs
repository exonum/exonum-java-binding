// Copyright 2018 The Exonum Team
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

pub use self::access::*;
pub use self::blockchain::*;
pub use self::blockchain_data::*;
pub use self::key_set_index::*;
pub use self::list_index::*;
pub use self::map_index::*;
pub use self::pair_iter::PairIter;
pub use self::prefixed::*;
pub use self::proof_entry::*;
pub use self::proof_list_index::*;
pub use self::proof_map_index::*;
pub use self::proof_map_index_next::*;
pub use self::raw_proof_map_index::*;
pub use self::readonly_erased_access::*;
pub use self::temporarydb::*;
pub use self::value_set_index::*;

use exonum::merkledb::{Database, RocksDB};
use exonum_cli::command::run::NodeRunConfig;
use std::sync::Arc;

mod access;
mod blockchain;
mod blockchain_data;
mod entry;
mod fork;
mod key_set_index;
mod list_index;
mod map_index;
mod pair_iter;
mod prefixed;
mod proof_entry;
mod proof_list_index;
mod proof_map_index;
mod proof_map_index_next;
mod raw_proof_map_index;
mod readonly_erased_access;
mod temporarydb;
mod value_set_index;

pub(crate) type Key = Vec<u8>;
pub(crate) type Value = Vec<u8>;

/// Creates database.
pub fn create_database(config: &NodeRunConfig) -> Result<Arc<dyn Database>, failure::Error> {
    let database = Arc::new(RocksDB::open(
        &config.db_path,
        &config.node_config.private_config.database,
    )?) as Arc<dyn Database>;
    Ok(database)
}
