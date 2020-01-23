/*
 * Copyright 2020 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.core.storage.indices;

import com.exonum.binding.common.hash.HashCode;

/**
 * A hashable index provides a cryptographic hash which represents the complete state of this index.
 *
 * <p>Hashable indexes enable efficient verification of their contents. Two indexes contain the same
 * values if their index hashes are equal. This property is used in the consensus algorithm to
 * compare the database state on different nodes.
 *
 * <p>Index hashes are also used in verifiable index views. Such views contain a subset of index
 * elements and a proof that jointly allow to restore the index hash. The computed index hash allows
 * to verify that the elements come from an index with a certain state.
 *
 * <p>Hashable indexes may participate in <a
 * href="https://docs.rs/exonum-merkledb/0.13.0-rc.2/exonum_merkledb/#state-aggregation">state hash
 * aggregation</a>.
 *
 * @see <a
 *     href="https://docs.rs/exonum-merkledb/0.13.0-rc.2/exonum_merkledb/trait.ObjectHash.html">ObjectHash
 *     trait</a>
 */
public interface HashableIndex extends StorageIndex {

  /**
   * Returns the index hash which represents the complete state of this index. Any modifications to
   * the stored entries affect the index hash.
   *
   * <p>How index hash is computed depends on the index data structure implementation.
   */
  HashCode getIndexHash();
}
