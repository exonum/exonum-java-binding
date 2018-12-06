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

import com.exonum.binding.common.configuration.StoredConfiguration;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.indices.ListIndex;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.storage.indices.ProofListIndexProxy;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.google.common.annotations.VisibleForTesting;

/**
 * Provides read-only access to the subset of
 * <a href="https://docs.rs/exonum/latest/exonum/blockchain/struct.Schema.html">
 * blockchain::Schema</a> features in the Core API: blocks, transaction messages, execution
 * results.
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
   * The height can be considered as a count of blocks in the blockchain,
   * where the first block has height {@code h = 0}. For example,
   * the "genesis block" (first, initial block in the blockchain) has height {@code h = 0}.
   * The latest committed block has height {@code h = getAllBlockHashes().size() - 1}.
   *
   * @throws RuntimeException if the "genesis block" was not created
   */
  public long getHeight() {
    return schema.getHeight();
  }

  /**
   * Returns a list of all block hashes, indexed by the block height.
   * For example, the "genesis block" will be at index 0,
   * the block at height {@code h = 10} — at index 10.
   * The last committed block will be at height {@code h = getAllBlockHashes().size() - 1}.
   */
  public ListIndex<HashCode> getAllBlockHashes() {
    return schema.getAllBlockHashes();
  }

  /**
   * Returns a proof list of transaction hashes committed in the block at the given height or
   * an empty list if the block at the given height doesn't exist.
   *
   * @param height block height starting from 0
   * @throws IllegalArgumentException if the height is negative
   */
  public ProofListIndexProxy<HashCode> getBlockTransactions(long height) {
    return schema.getBlockTransactions(height);
  }

  /**
   * Returns a proof list of transaction hashes committed in the block with given id or an empty
   * list if the block with given id doesn't exist.
   *
   * @param blockId id of the block
   */
  public ProofListIndexProxy<HashCode> getBlockTransactions(HashCode blockId) {
    Block block = getBlock(blockId);
    return getBlockTransactions(block.getHeight());
  }

  /**
   * Returns a proof list of transaction hashes committed in the given block or an empty list if
   * the block doesn't exist.
   *
   * @param block block of which list of transaction hashes should be returned
   */
  public ProofListIndexProxy<HashCode> getBlockTransactions(Block block) {
    return getBlockTransactions(block.getHeight());
  }

  /**
   * Returns a map of transaction messages identified by their SHA-256 hashes. Both committed and
   * in-pool (not yet processed) transactions are returned.
   */
  public MapIndex<HashCode, TransactionMessage> getTxMessages() {
    return schema.getTxMessages();
  }

  /**
   * Returns a map with a key-value pair of a transaction hash and execution result.
   */
  public ProofMapIndexProxy<HashCode, TransactionResult> getTxResults() {
    return schema.getTxResults();
  }

  /**
   * Returns a transaction execution result for given message hash.
   * @param messageHash a message hash
   */
  public TransactionResult getTxResult(HashCode messageHash) {
    ProofMapIndexProxy<HashCode, TransactionResult> txResults = getTxResults();
    return txResults.get(messageHash);
  }

  /**
   * Returns a map that keeps the transaction position inside the blockchain for every transaction
   * hash.
   */
  public MapIndex<HashCode, TransactionLocation> getTxLocations() {
    return schema.getTxLocations();
  }

  /**
   * Returns transaction position inside the blockchain for given message hash.
   * @param messageHash message hash
   */
  public TransactionLocation getTxLocation(HashCode messageHash) {
    MapIndex<HashCode, TransactionLocation> txLocations = getTxLocations();
    return txLocations.get(messageHash);
  }

  /**
   * Returns a map that stores a block object for every block hash.
   */
  public MapIndex<HashCode, Block> getBlocks() {
    return schema.getBlocks();
  }

  /**
   * Returns a block object for given block hash.
   */
  public Block getBlock(HashCode blockHash) {
    MapIndex<HashCode, Block> blocks = getBlocks();
    return blocks.get(blockHash);
  }

  /**
   * Returns the latest committed block.
   *
   * @throws RuntimeException if the "genesis block" was not created
   */
  public Block getLastBlock() {
    return schema.getLastBlock();
  }

  /**
   * Returns the configuration for the latest height of the blockchain, including services and their
   * parameters.
   *
   * @throws RuntimeException if the "genesis block" was not created
   */
  public StoredConfiguration getActualConfiguration() {
    return schema.getActualConfiguration();
  }

}
