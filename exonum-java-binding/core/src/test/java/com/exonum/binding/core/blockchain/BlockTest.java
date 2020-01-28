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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.common.hash.HashCode;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BlockTest {

  @Test
  void isEmpty() {
    Block emptyBlock = Blocks.aBlock()
        .numTransactions(0)
        .build();

    assertTrue(emptyBlock.isEmpty());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, Integer.MAX_VALUE})
  void nonEmptyBlock(int numTransactions) {
    Block nonEmptyBlock = Blocks.aBlock()
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
}
