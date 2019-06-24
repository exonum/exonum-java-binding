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

package com.exonum.binding.core.blockchain;

import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.core.blockchain.serialization.CoreTypeAdapterFactory;
import com.exonum.binding.core.service.Schema;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

/**
 * Exonum block header data structure.
 *
 * <p>A block is essentially a list of transactions, which is a result of the consensus algorithm
 * (thus authenticated by the supermajority of validators) and is applied atomically to the
 * blockchain state.
 *
 * <p>This structure only contains the amount of transactions and the transactions root hash as well
 * as other information, but not the transactions themselves.
 *
 * @see Blockchain
 */
@AutoValue
public abstract class Block {

  /**
   * Returns the SHA-256 hash of this block.
   */
  public abstract HashCode getBlockHash();

  /**
   * Identifier of the leader node which has proposed the block.
   */
  public abstract int getProposerId();

  /**
   * Returns the height of this block which is a distance between the last block and the "genesis"
   * block. Genesis block has 0 height. Therefore, the blockchain height is equal to
   * the number of blocks plus one.
   *
   * <p>The height also identifies each block in the blockchain.
   */
  public abstract long getHeight();

  /**
   * Returns true if this block is empty:
   * contains no {@linkplain #getNumTransactions() transactions}.
   */
  public final boolean isEmpty() {
    return getNumTransactions() == 0;
  }

  /**
   * Number of transactions in this block.
   */
  public abstract int getNumTransactions();

  /**
   * Hash link to the previous block in the blockchain.
   */
  public abstract HashCode getPreviousBlockHash();

  /**
   * Root hash of the Merkle tree of transactions in this block.
   * These transactions can be accessed with {@link Blockchain#getBlockTransactions(Block)}.
   */
  public abstract HashCode getTxRootHash();

  /**
   * Hash of the blockchain state after applying transactions in the block.
   *
   * @see Schema#getStateHashes()
   */
  public abstract HashCode getStateHash();

  @Override
  public int hashCode() {
    // Use just the first 4 bytes of the SHA-256 hash of the binary object representation,
    // as they will have close to uniform distribution.
    // AutoValue will still use all fields in #equals.
    return getBlockHash().hashCode();
  }

  /**
   * Provides a Gson type adapter for this class.
   *
   * @see CoreTypeAdapterFactory
   */
  public static TypeAdapter<Block> typeAdapter(Gson gson) {
    return new AutoValue_Block.GsonTypeAdapter(gson);
  }

  /**
   * Creates a new block builder.
   */
  public static Builder builder() {
    return new AutoValue_Block.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    /**
     * Sets the hash of the block binary representation.
     */
    public abstract Builder blockHash(HashCode hash);

    /**
     * Sets the identifier of the leader node which has proposed the block.
     */
    public abstract Builder proposerId(int proposerId);

    /**
     * Sets the block height, which is the distance between the block and the genesis block,
     * which has zero height. Must be non-negative.
     */
    public abstract Builder height(long height);

    /**
     * Sets the number of transactions in this block. Must be non-negative.
     */
    public abstract Builder numTransactions(int numTransactions);

    /**
     * Sets the hash of the previous block in the hash chain. The previous block is the block
     * with the height, that is equal to this minus one. Genesis block has a previous hash
     * of zeroes.
     */
    public abstract Builder previousBlockHash(HashCode previousBlockHash);

    /**
     * Sets the Merkle root hash of the collection holding all transactions in this block.
     * This collection and transactions can be accessed with
     * {@link Blockchain#getBlockTransactions(Block)}.
     */
    public abstract Builder txRootHash(HashCode txRootHash);

    // TODO: Expand on that when it has meaningful applications: ECR-2756
    /**
     * Sets the blockchain state hash at the moment this block was committed. The blockchain
     * state hash reflects the state of each service in the database.
     *
     * @see Schema#getStateHashes()
     */
    public abstract Builder stateHash(HashCode blockchainStateHash);

    abstract Block autoBuild();

    /**
     * Creates a new block with the set arguments.
     *
     * @throws IllegalStateException if some of the arguments were not set or aren't valid
     */
    public Block build() {
      Block block = autoBuild();
      checkState(block.getHeight() >= 0, "Height is negative: %s", block.getHeight());
      checkState(block.getNumTransactions() >= 0,
          "numTransaction is negative: %s", block.getNumTransactions());

      return block;
    }
  }
}
