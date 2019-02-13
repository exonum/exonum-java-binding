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

package com.exonum.binding.blockchain.serialization;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import com.exonum.binding.blockchain.Block;
import com.exonum.binding.blockchain.Blocks;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.Serializer;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class BlockSerializerTest {

  private static final Serializer<Block> BLOCK_SERIALIZER = BlockSerializer.INSTANCE;

  @ParameterizedTest
  @MethodSource("testSource")
  void roundTrip(Block expected) {
    byte[] bytes = BLOCK_SERIALIZER.toBytes(expected);
    Block actual = BLOCK_SERIALIZER.fromBytes(bytes);

    assertThat(actual, equalTo(expected));
  }

  private static Stream<Block> testSource() {
    Block block1 = Block.builder()
        .proposerId(0)
        .height(1)
        .numTransactions(2)
        .blockHash(HashCode.fromString("ab"))
        .previousBlockHash(HashCode.fromString("bc"))
        .txRootHash(HashCode.fromString("cd"))
        .stateHash(HashCode.fromString("ab"))
        .build();
    Block block2 = Block.builder()
        .proposerId(Integer.MAX_VALUE)
        .height(Long.MAX_VALUE)
        .numTransactions(Integer.MAX_VALUE)
        .blockHash(HashCode.fromString("ab"))
        .previousBlockHash(HashCode.fromString("bc"))
        .txRootHash(HashCode.fromString("cd"))
        .stateHash(HashCode.fromString("ab"))
        .build();

    return Stream.of(block1, block2)
        .map(Blocks::withProperHash);
  }
}
