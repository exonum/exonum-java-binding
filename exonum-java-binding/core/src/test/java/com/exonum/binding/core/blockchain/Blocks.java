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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.HashFunction;
import com.exonum.binding.common.hash.Hashing;
import com.exonum.binding.core.blockchain.Block.Builder;
import com.exonum.binding.core.blockchain.serialization.BlockSerializer;
import com.google.common.collect.ImmutableMap;

public final class Blocks {

  /**
   * Creates a fully initialized builder of some block with some default values.
   */
  static Builder aBlock() {
    HashFunction hashFunction = Hashing.sha256();
    long blockHeight = 1;
    return Block.builder()
        .proposerId(0)
        .height(blockHeight)
        .numTransactions(0)
        .blockHash(hashFunction.hashLong(blockHeight))
        .previousBlockHash(hashFunction.hashLong(blockHeight - 1))
        .txRootHash(hashFunction.hashString("transactions at" + blockHeight, UTF_8))
        .stateHash(hashFunction.hashString("state hash at " + blockHeight, UTF_8))
        .additionalHeaders(ImmutableMap.of())
        .errorHash(HashCode.fromString("ab"));
  }

  /**
   * Returns a new block that has its hash set up to the proper value — SHA-256 hash
   * of its binary representation.
   *
   * @param block a block to process
   */
  public static Block withProperHash(Block block) {
    byte[] blockBytes = BlockSerializer.INSTANCE.toBytes(block);
    HashCode actualBlockHash = Hashing.sha256()
        .hashBytes(blockBytes);
    return Block.builder()
        .proposerId(block.getProposerId())
        .height(block.getHeight())
        .numTransactions(block.getNumTransactions())
        .blockHash(actualBlockHash)
        .previousBlockHash(block.getPreviousBlockHash())
        .txRootHash(block.getTxRootHash())
        .stateHash(block.getStateHash())
        .errorHash(block.getErrorHash())
        .additionalHeaders(block.getAdditionalHeaders())
        .build();
  }

  private Blocks() {}
}
