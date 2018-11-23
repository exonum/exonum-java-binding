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

package com.exonum.binding.common.serialization;

import static com.exonum.binding.common.serialization.StandardSerializersTest.roundTripTest;

import com.exonum.binding.common.blockchain.Block;
import com.exonum.binding.common.hash.HashCode;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class BlockSerializerTest {
  private Serializer<Block> serializer = BlockSerializer.INSTANCE;

  @ParameterizedTest
  @MethodSource("testSource")
  void roundTrip(Block key) {
    roundTripTest(key, serializer);
  }

  private static Stream<Block> testSource() {
    return Stream.of(
        Block.valueOf(
            (short) 1,
            1,
            1,
            HashCode.fromString("ab"),
            HashCode.fromString("bc"),
            HashCode.fromString("cd")),
        Block.valueOf(
            (short) 1,
            1,
            1,
            null,
            null,
            null));
  }

}
