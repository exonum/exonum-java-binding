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

package com.exonum.binding.core.service;

import com.exonum.binding.core.blockchain.Block;
import com.exonum.binding.core.storage.indices.ProofEntryIndexProxy;
import com.exonum.binding.core.storage.indices.ProofListIndexProxy;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;

/**
 * A schema of the collections (a.k.a. indices) of a service.
 *
 * <p>To verify the integrity of the database state on each node in the network,
 * the core automatically tracks every Merkelized collection used by the user
 * services. It aggregates state hashes of these collections into a single
 * Merkelized meta-map. The hash of this meta-map is considered the hash of the
 * entire blockchain state and is recorded as such in {@linkplain Block#getStateHash() blocks}
 * and Precommit messages.
 *
 * <p>Exonum starts aggregating a service collection state hash once it is <em>initialized</em>:
 * created for the first time with a {@link com.exonum.binding.core.storage.database.Fork}.
 *
 * <p>Please note that if the service does not use any Merkelized collections,
 * the framework will not be able to verify that its transactions cause the same
 * results on different nodes.
 *
 * @see ProofListIndexProxy#getIndexHash()
 * @see ProofMapIndexProxy#getIndexHash()
 * @see ProofEntryIndexProxy#getIndexHash()
 * @see com.exonum.binding.core.blockchain.Blockchain
 */
public interface Schema {
}
