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
 */

package com.exonum.binding.blockchain;

import com.exonum.binding.common.hash.HashCode;
import com.google.auto.value.AutoValue;

/**
 * Exonum block header data structure.
 *
 * <p>A block is essentially a list of transactions, which is a result of the consensus algorithm
 * (thus authenticated by the supermajority of validators) and is applied atomically to the
 * blockchain state.
 *
 * <p>This structure only contains the amount of transactions and the transactions root hash as well
 * as other information, but not the transactions themselves.
 */
@AutoValue
public abstract class Block {

  // TODO: implement a builder for this class - ECR-2734
  public static Block valueOf(
      int proposerId,
      long height,
      int numTransactions,
      HashCode previousBlockHash,
      HashCode txRootHash,
      HashCode stateHash,
      HashCode blockHash) {
    return new AutoValue_Block(
        proposerId, height, numTransactions, previousBlockHash, txRootHash, stateHash, blockHash);
  }

  /**
   * Identifier of the leader node which has proposed the block.
   */
  public abstract int getProposerId();

  /**
   * Height of the block, which also identifies the number of this particular block in the
   * blockchain starting from 0 ("genesis" block).
   */
  public abstract long getHeight();

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
   * These transactions can be accesed with {@link Blockchain#getBlockTransactions(Block)}.
   */
  public abstract HashCode getTxRootHash();

  /**
   * Hash of the blockchain state after applying transactions in the block.
   */
  public abstract HashCode getStateHash();

  /**
   * Returns the SHA-256 hash of this block.
   */
  public abstract HashCode getBlockHash();

  @Override
  public int hashCode() {
    // Use just the first 4 bytes of the SHA-256 hash of the binary object representation,
    // as they will have close to uniform distribution.
    // AutoValue will still use all fields in #equals.
    return getBlockHash().hashCode();
  }
}
