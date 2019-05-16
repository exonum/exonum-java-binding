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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.blockchain.Block.Builder;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.HashFunction;
import com.exonum.binding.common.hash.Hashing;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BlockTest {

  @Test
  void isEmpty() {
    Block emptyBlock = aBlock()
        .numTransactions(0)
        .build();

    assertTrue(emptyBlock.isEmpty());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, Integer.MAX_VALUE})
  void nonEmptyBlock(int numTransactions) {
    Block nonEmptyBlock = aBlock()
        .numTransactions(numTransactions)
        .build();

    assertFalse(nonEmptyBlock.isEmpty());
  }

  @Test
  void verifyEquals() {
    EqualsVerifier.forClass(AutoValue_Block.class)
        // Constructor ensures that we have no nulls
        .suppress(Warning.NULL_FIELDS)
        // We use 4 bytes from proper SHA-256 with good distribution
        .suppress(Warning.STRICT_HASHCODE)
        .withPrefabValues(HashCode.class, HashCode.fromInt(1), HashCode.fromInt(2))
        .verify();
  }

  private static Builder aBlock() {
    HashFunction hashFunction = Hashing.sha256();
    long blockHeight = 1;
    return Block.builder()
        .proposerId(0)
        .height(blockHeight)
        .numTransactions(0)
        .blockHash(hashFunction.hashLong(blockHeight))
        .previousBlockHash(hashFunction.hashLong(blockHeight - 1))
        .txRootHash(hashFunction.hashString("transactions at" + blockHeight, UTF_8))
        .stateHash(hashFunction.hashString("state hash at " + blockHeight, UTF_8));
  }
}
