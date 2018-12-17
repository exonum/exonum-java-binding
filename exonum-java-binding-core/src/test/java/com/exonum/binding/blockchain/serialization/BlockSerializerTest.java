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
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.Hashing;
import com.exonum.binding.common.serialization.Serializer;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class BlockSerializerTest {

  private static final Serializer<Block> serializer = BlockSerializer.INSTANCE;

  @ParameterizedTest
  @MethodSource("testSource")
  void roundTrip(Block expected) {
    byte[] bytes = serializer.toBytes(expected);
    Block actual = serializer.fromBytes(bytes);

    assertThat(actual, equalTo(expected));
  }

  private static Stream<Block> testSource() {
    Block block1 = Block.valueOf(
        1,
        1,
        1,
        HashCode.fromString("ab"),
        HashCode.fromString("bc"),
        HashCode.fromString("cd"),
        HashCode.fromString("ab"));
    Block block2 = Block.valueOf(
        Integer.MAX_VALUE,
        Long.MAX_VALUE,
        Integer.MAX_VALUE,
        HashCode.fromString("a0a0a0a0a0"),
        HashCode.fromString("a0a0a0a0a0"),
        HashCode.fromString("a0a0a0a0a0"),
        HashCode.fromString("a0a0a0a0a0"));
    return Stream.of(
        Block.valueOf(
            block1.getProposerId(),
            block1.getHeight(),
            block1.getNumTransactions(),
            block1.getPreviousBlockHash(),
            block1.getTxRootHash(),
            block1.getStateHash(),
            Hashing.sha256().hashBytes(serializer.toBytes(block1))),
        Block.valueOf(
            block2.getProposerId(),
            block2.getHeight(),
            block2.getNumTransactions(),
            block2.getPreviousBlockHash(),
            block2.getTxRootHash(),
            block2.getStateHash(),
            Hashing.sha256().hashBytes(serializer.toBytes(block2))));
  }

}
