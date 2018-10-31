/*
 * Copyright 2018 The Exonum Team
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
 *
 */

package com.exonum.binding.blockchain;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.indices.ListIndex;
import com.exonum.binding.storage.indices.ProofListIndexProxy;
import com.google.common.annotations.VisibleForTesting;

/**
 * Provides read-only access to the blockchain state that is maintained by Exonum core:
 * blocks, transaction messages, execution results. Please refer to the
 * <a href="https://docs.rs/exonum/latest/exonum/blockchain/struct.Schema.html">doc</a> for details.
 */
public final class Blockchain {

  private final CoreSchemaProxy schema;

  @VisibleForTesting
  Blockchain(CoreSchemaProxy schema) {
    this.schema = schema;
  }

  /**
   * Constructs a new blockchain instance for the given database view.
   */
  public static Blockchain newInstance(View view) {
    CoreSchemaProxy coreSchema = CoreSchemaProxy.newInstance(view);
    return new Blockchain(coreSchema);
  }

  /**
   * Returns the height of the latest committed block in the blockchain.
   * Height is a number of blocks in the blockchain starting from {@code h = 0} zero block,
   * which is the "genesis block" (first, initial block in the blockchain).
   *
   * @throws RuntimeException if the "genesis block" was not created
   */
  public long getHeight() {
    return schema.getHeight();
  }

  /**
   * Returns a list of all block hashes, indexed by the block height.
   * For example, the "genesis block" will be at index 0,
   * the block at height {@code h = 10} — at index **10**.
   * The last committed block will be at height {@code h = getAllBlockHashes().size() - 1}.
   */
  public ListIndex<HashCode> getAllBlockHashes() {
    return schema.getAllBlockHashes();
  }

  /**
   * Returns a proof list of transaction hashes within the block by the given height.
   *
   * @param height block height starting from 0
   * @throws IllegalArgumentException if the height is negative
   */
  public ProofListIndexProxy<HashCode> getBlockTransactions(long height) {
    return schema.getBlockTransactions(height);
  }

}
