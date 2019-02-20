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

import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.common.configuration.StoredConfiguration;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.common.transaction.TransactionLocation;
import com.exonum.binding.common.transaction.TransactionResult;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.indices.ListIndex;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.storage.indices.ProofListIndexProxy;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;

/**
 * Provides read-only access to the subset of
 * <a href="https://docs.rs/exonum/latest/exonum/blockchain/struct.Schema.html">
 * blockchain::Schema</a> features in the Core API: blocks, transaction messages, execution
 * results.
 *
 * <p>All method arguments are non-null by default.
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
   * Returns true if the blockchain contains <em>exactly</em> the same block as the passed
   * value; false if it does not contain such block. Please note that all block fields
   * are compared, not only its hash.
   *
   * @param block a value to check for presence in the blockchain
   */
  public boolean containsBlock(Block block) {
    return findBlock(block.getBlockHash())
        .map(block::equals)
        .orElse(false);
  }

  /**
   * Returns the <em>blockchain height</em> which is the height of the latest committed block
   * in the blockchain. The block height is a distance between the last block
   * and the "genesis", or initial, block. Therefore, the blockchain height is equal to the number
   * of blocks plus one.
   *
   * <p>For example, the "genesis" block has height {@code h = 0}. The latest committed block
   * has height {@code h = getBlockHashes().size() - 1}.
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
   * The last committed block will be at height {@code h = getBlockHashes().size() - 1}.
   */
  public ListIndex<HashCode> getBlockHashes() {
    return schema.getBlockHashes();
  }

  /**
   * Returns a proof list of transaction hashes committed in the block at the given height.
   *
   * @param height block height starting from 0
   * @throws IllegalArgumentException if the height is invalid: negative or exceeding
   *     the {@linkplain #getHeight() blockchain height}
   */
  public ProofListIndexProxy<HashCode> getBlockTransactions(long height) {
    return schema.getBlockTransactions(height);
  }

  /**
   * Returns a proof list of transaction hashes committed in the block with the given id.
   *
   * @param blockId id of the block
   * @throws IllegalArgumentException if there is no block with given id
   */
  public ProofListIndexProxy<HashCode> getBlockTransactions(HashCode blockId) {
    Optional<Block> block = findBlock(blockId);
    checkArgument(block.isPresent(), "No block found for given id %s", blockId);
    return getBlockTransactions(block.get().getHeight());
  }

  /**
   * Returns a proof list of transaction hashes committed in the given block.
   * The given block must match exactly the block that is stored in the database.
   *
   * @param block block of which list of transaction hashes should be returned
   * @throws IllegalArgumentException if there is no such block in the blockchain
   */
  public ProofListIndexProxy<HashCode> getBlockTransactions(Block block) {
    checkArgument(containsBlock(block), "No such block (%s) in the database", block);
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
   *
   * @return a transaction execution result, or {@code Optional.empty()} if this transaction
   *         is unknown or was not yet executed
   */
  public Optional<TransactionResult> getTxResult(HashCode messageHash) {
    ProofMapIndexProxy<HashCode, TransactionResult> txResults = getTxResults();
    TransactionResult transactionResult = txResults.get(messageHash);
    return Optional.ofNullable(transactionResult);
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
   *
   * @return a transaction execution result, or {@code Optional.empty()} if this transaction
   *         is unknown or was not yet executed
   */
  public Optional<TransactionLocation> getTxLocation(HashCode messageHash) {
    MapIndex<HashCode, TransactionLocation> txLocations = getTxLocations();
    TransactionLocation transactionLocation = txLocations.get(messageHash);
    return Optional.ofNullable(transactionLocation);
  }

  /**
   * Returns a map that stores a block object for every block hash.
   */
  public MapIndex<HashCode, Block> getBlocks() {
    return schema.getBlocks();
  }

  /**
   * Returns the block at the given height.
   *
   * @param height the height of the block; must be non-negative and less than or equal to
   *     the current {@linkplain #getHeight() blockchain height}
   * @return a block at the height
   * @throws IndexOutOfBoundsException if the height is not valid
   */
  public Block getBlock(long height) {
    checkHeight(height);

    ListIndex<HashCode> blockHashes = getBlockHashes();
    HashCode blockHash = blockHashes.get(height);
    MapIndex<HashCode, Block> blocks = getBlocks();
    return blocks.get(blockHash);
  }

  private void checkHeight(long height) {
    long blockchainHeight = getHeight();
    if (height < 0 || height > blockchainHeight) {
      throw new IndexOutOfBoundsException("Block height (" + height + ") is out of range [0, "
          + blockchainHeight + "]");
    }
  }

  /**
   * Returns a block object for given block hash.
   *
   * @return a corresponding block, or {@code Optional.empty()} if there is no block with given
   *         block hash
   */
  public Optional<Block> findBlock(HashCode blockHash) {
    MapIndex<HashCode, Block> blocks = getBlocks();
    Block block = blocks.get(blockHash);
    return Optional.ofNullable(block);
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
