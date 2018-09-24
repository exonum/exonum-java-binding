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

package com.exonum.binding.common.proofs.map;

import static com.exonum.binding.test.Bytes.bytes;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class KeyBitSetIsPrefixParameterizedTest {

  private KeyBitSet path;

  private KeyBitSet other;

  @ParameterizedTest(name = "{index} => description={5}")
  @MethodSource("testData")
  void isPrefixOfOther(byte[] pathBytes, int pathLength, byte[] otherPathBytes, int otherPathLength,
      boolean prefixOf, String description) {
    init(pathBytes, pathLength, otherPathBytes, otherPathLength);

    assertThat(path.isPrefixOf(other), equalTo(prefixOf));
  }

  @ParameterizedTest(name = "{index} => description={5}")
  @MethodSource("testData")
  void isPrefixOfSelf(byte[] pathBytes, int pathLength, byte[] otherPathBytes, int otherPathLength,
      boolean prefixOf, String description) {
    init(pathBytes, pathLength, otherPathBytes, otherPathLength);

    assertTrue(path.isPrefixOf(path));
  }

  @ParameterizedTest(name = "{index} => description={5}")
  @MethodSource("testData")
  void isPrefixOfClone(byte[] pathBytes, int pathLength, byte[] otherPathBytes, int otherPathLength,
      boolean prefixOf, String description) {
    init(pathBytes, pathLength, otherPathBytes, otherPathLength);
    KeyBitSet clone = new KeyBitSet(pathBytes, pathLength);

    assertTrue(path.isPrefixOf(clone));
  }

  private void init(byte[] pathBytes, int pathLength, byte[] otherPathBytes, int otherPathLength) {
    path = new KeyBitSet(pathBytes, pathLength);
    other = new KeyBitSet(otherPathBytes, otherPathLength);
  }

  private static Stream<Arguments> testData() {
    return Stream.of(
        // "A <- B" reads "A is a prefix of B"
        // "!P" reads "not P"
        Arguments.of(bytes(), 0, bytes(), 0, true, "[] <- []"),
        Arguments.of(bytes(), 0, bytes(), 2, true, "[] <- [00]"),
        Arguments.of(bytes(), 0, bytes(0xE), 4, true, "[] <- [1110]"),

        Arguments.of(bytes(), 2, bytes(), 2, true, "[00] <- [00]"),
        Arguments.of(bytes(), 2, bytes(), 3, true, "[00] <- [000]"),
        Arguments.of(bytes(), 2, bytes(0b100), 3, true, "[00] <- [100]"),
        Arguments.of(bytes(), 2, bytes(0x10), 5, true, "[00] <- [1 0000]"),
        Arguments.of(bytes(), 2, bytes(), 1, false, "![00] <- [0]"),
        Arguments.of(bytes(), 2, bytes(0x1), 1, false, "![00] <- [01]"),
        Arguments.of(bytes(), 2, bytes(0x2), 2, false, "![00] <- [10]"),
        Arguments.of(bytes(), 2, bytes(0x3), 2, false, "![00] <- [11]"),

        Arguments.of(bytes(0x0F), 4, bytes(0x0F), 4, true, "[1111] <- [1111]"),
        Arguments.of(bytes(0x0F), 4, bytes(0x0F), 5, true, "[1111] <- [0 1111]"),
        Arguments.of(bytes(0x0F), 4, bytes(0x1F), 5, true, "[1111] <- [1 1111]"),
        Arguments.of(bytes(0x0F), 4, bytes(0x1F), 8, true, "[1111] <- [0001 1111]"),
        Arguments.of(bytes(0x0F), 4, bytes(0x8F), 8, true, "[1111] <- [1000 1111]"),
        Arguments.of(bytes(0x0F), 4, bytes(0x7), 3, false, "![1111] <- [111]"),
        Arguments.of(bytes(0x0F), 4, bytes(0x7), 4, false, "![1111] <- [0111]"),
        Arguments.of(bytes(0x0F), 4, bytes(0x8), 4, false, "![1111] <- [1000]"),
        Arguments.of(bytes(0x0F), 4, bytes(0x10), 5, false, "![1111] <- [10000]"),

        Arguments.of(bytes(0x0F), 5, bytes(0x0F), 5, true, "[0 1111] <- [0 1111]"),
        Arguments.of(bytes(0x0F), 5, bytes(0x0F), 6, true, "[0 1111] <- [00 1111]"),
        Arguments.of(bytes(0x0F), 5, bytes(0x2F), 6, true, "[0 1111] <- [10 1111]"),
        Arguments.of(bytes(0x0F), 5, bytes(0x8F), 8, true, "[0 1111] <- [1000 1111]"),
        Arguments.of(bytes(0x0F), 5, bytes(0x0F), 4, false, "![0 1111] <- [1111]"),
        Arguments.of(bytes(0x0F), 5, bytes(0x0E), 5, false, "![0 1111] <- [0 1110]"),
        Arguments.of(bytes(0x0F), 5, bytes(0x1F), 5, false, "![0 1111] <- [1 1111]"),
        Arguments.of(bytes(0x0F), 5, bytes(0x0B), 5, false, "![0 1111] <- [0 1011]"),
        Arguments.of(bytes(0x00, 0xFF), 2 * Byte.SIZE, bytes(0x00, 0xFF, 0x11), 3 * Byte.SIZE, true,
            "[FF 00] <- [11 FF 00]"),
        Arguments
            .of(bytes(0x00, 0xFF), 2 * Byte.SIZE, bytes(0x01, 0xFF, 0x11), 3 * Byte.SIZE, false,
                "![FF 00] <- [11 FF 01]")
    );
  }
}
