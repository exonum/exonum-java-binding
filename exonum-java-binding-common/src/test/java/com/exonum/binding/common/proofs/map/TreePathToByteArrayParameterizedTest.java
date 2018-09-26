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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TreePathToByteArrayParameterizedTest {

  @ParameterizedTest(name = "{index} => pathBytes={0}, pathLength={1}, description={2}")
  @MethodSource("testData")
  void toByteArray(byte[] pathBytes, int pathLength, String description) {
    TreePath path = new TreePath(BitSet.valueOf(pathBytes), pathLength);
    assertThat(path.toByteArray(), equalTo(pathBytes));
  }

  private static List<Arguments> testData() {
    // "A <- B" reads "A is a prefix of B"
    // "!P" reads "not P"
    return Arrays.asList(
        Arguments.of(bytes(), 0, "[]"),
        Arguments.of(bytes(), 2, "[00]"),
        Arguments.of(bytes(0x0F), 4, "[1111]"),
        Arguments.of(bytes(0x0F), 5, "[1111 0]"),
        Arguments.of(bytes(0x00, 0xFF), 2 * Byte.SIZE, "[00 FF]"),
        Arguments.of(bytes(0x00, 0xFF, 0xEA, 0x01), 4 * Byte.SIZE, "[00 FF EA 01]")
    );
  }
}
