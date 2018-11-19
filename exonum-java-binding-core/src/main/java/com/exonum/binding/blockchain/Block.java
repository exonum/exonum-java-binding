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
import com.google.common.base.Objects;

/**
 * Exonum block header data structure.
 *
 * A block is essentially a list of transactions, which is a result of the consensus algorithm
 * (thus authenticated by the supermajority of validators) and is applied atomically to the
 * blockchain state.
 *
 * This structure only contains the amount of transactions and the transactions root hash as well as
 * other information, but not the transactions themselves.
 */
class Block {

  private short proposerId;
  private long height;
  private int numTransactions;
  private HashCode previousBlockHash;
  private HashCode txRootHash;
  private HashCode stateHash;

  Block(short proposerId, long height, int numTransactions, HashCode previousBlockHash,
      HashCode txRootHash, HashCode stateHash) {
    this.proposerId = proposerId;
    this.height = height;
    this.numTransactions = numTransactions;
    this.previousBlockHash = previousBlockHash;
    this.txRootHash = txRootHash;
    this.stateHash = stateHash;
  }

  /**
   * Identifier of the leader node which has proposed the block.
   */
  public short getProposerId() {
    return proposerId;
  }

  /**
   * Height of the block, which is also the number of this particular block in the blockchain.
   */
  public long getHeight() {
    return height;
  }

  /**
   * Number of transactions in this block.
   */
  public int getNumTransactions() {
    return numTransactions;
  }

  /**
   * Hash link to the previous block in the blockchain.
   */
  public HashCode getPreviousBlockHash() {
    return previousBlockHash;
  }

  /**
   * Root hash of the Merkle tree of transactions in this block.
   */
  public HashCode getTxRootHash() {
    return txRootHash;
  }

  /**
   * Hash of the blockchain state after applying transactions in the block.
   */
  public HashCode getStateHash() {
    return stateHash;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Block block = (Block) o;
    return proposerId == block.proposerId &&
        height == block.height &&
        numTransactions == block.numTransactions &&
        Objects.equal(previousBlockHash, block.previousBlockHash) &&
        Objects.equal(txRootHash, block.txRootHash) &&
        Objects.equal(stateHash, block.stateHash);
  }

  @Override
  public int hashCode() {
    return Objects
        .hashCode(proposerId, height, numTransactions, previousBlockHash, txRootHash, stateHash);
  }

  @Override
  public String toString() {
    return "Block{" +
        "proposerId=" + proposerId +
        ", height=" + height +
        ", numTransactions=" + numTransactions +
        ", previousBlockHash=" + previousBlockHash +
        ", txRootHash=" + txRootHash +
        ", stateHash=" + stateHash +
        '}';
  }

}
