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

import static com.exonum.binding.common.proofs.map.DbKeyTestUtils.branchKeyFromPrefix;
import static com.exonum.binding.common.proofs.map.DbKeyTestUtils.leafKeyFromPrefix;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DbKeyCommonPrefixParameterizedTest {

  @ParameterizedTest(name = "{index} => description={3}")
  @MethodSource("testData")
  void commonPrefix(DbKey firstKey, DbKey secondKey, DbKey expectedResultKey, String description) {
    DbKey actualCommonPrefixKey = firstKey.commonPrefix(secondKey);
    assertThat(actualCommonPrefixKey, equalTo(expectedResultKey));
  }

  @ParameterizedTest(name = "{index} => description={3}")
  @MethodSource("testData")
  void commonPrefixCommutative(DbKey firstKey, DbKey secondKey, DbKey expectedResultKey,
      String description) {
    DbKey actualCommonPrefixKey = secondKey.commonPrefix(firstKey);
    assertThat(actualCommonPrefixKey, equalTo(expectedResultKey));
  }

  @ParameterizedTest(name = "{index} => description={3}")
  @MethodSource("testData")
  void commonPrefixOfSelf(DbKey firstKey, DbKey secondKey, DbKey expectedResultKey,
      String description) {
    DbKey commonPrefix = firstKey.commonPrefix(firstKey);
    assertThat(commonPrefix, sameInstance(firstKey));
  }

  @ParameterizedTest(name = "{index} => description={3}")
  @MethodSource("testData")
  void commonPrefixOfEqualKey(DbKey firstKey, DbKey secondKey, DbKey expectedResultKey,
      String description) {
    DbKey firstKeyClone = DbKey.fromBytes(firstKey.getRawDbKey());
    DbKey commonPrefix = firstKey.commonPrefix(firstKeyClone);
    assertThat(commonPrefix, equalTo(firstKey));
  }

  private static Stream<Arguments> testData() {
    // "A <- B" reads "A is a prefix of B"
    // "!P" reads "not P"
    return Stream.of(
        // "A | B -> C" reads "C is a common prefix of A and B"
        // # Not a prefix:
        Arguments.of(
            branchKeyFromPrefix("0"),
            branchKeyFromPrefix("1"),
            branchKeyFromPrefix(""),
            "[0] | [1] -> []"),
        Arguments.of(
            branchKeyFromPrefix("01"),
            branchKeyFromPrefix("10"),
            branchKeyFromPrefix(""),
            "[01] | [10] -> []"),
        // # Prefixes of various length:
        Arguments.of(
            branchKeyFromPrefix("1010"),
            branchKeyFromPrefix("10111"),
            branchKeyFromPrefix("101"),
            "[1010] | [10111] -> [101]"),
        Arguments.of(
            branchKeyFromPrefix("1101"),
            branchKeyFromPrefix("11011"),
            branchKeyFromPrefix("1101"),
            "[1101] | [11011] -> [1101]"),
        Arguments.of(
            branchKeyFromPrefix("11101"),
            branchKeyFromPrefix("11111"),
            branchKeyFromPrefix("111"),
            "[11101] | [11111] -> [111]"),
        Arguments.of(
            branchKeyFromPrefix("11111"),
            branchKeyFromPrefix("10111"),
            branchKeyFromPrefix("1"),
            "[11111] | [11101] -> [1]"),
        // ## Multi-byte keys
        Arguments.of(
            branchKeyFromPrefix("1111 1111 | 10_11"),
            branchKeyFromPrefix("1111 1111 | 10_00"),
            branchKeyFromPrefix("1111 1111 | 10"),
            "[1111 1111 | 10_11] | [1111 1111 | 10_00] -> [1111 1111 | 10]"),
        // ## One is full prefix of another:
        Arguments.of(
            branchKeyFromPrefix(""),
            branchKeyFromPrefix("1"),
            branchKeyFromPrefix(""),
            "[] | [1] -> []"),
        Arguments.of(
            branchKeyFromPrefix(""),
            branchKeyFromPrefix("0"),
            branchKeyFromPrefix(""),
            "[] | [0] -> []"),
        Arguments.of(
            branchKeyFromPrefix("1"),
            branchKeyFromPrefix("11"),
            branchKeyFromPrefix("1"),
            "[1] | [11] -> [1]"),
        Arguments.of(
            branchKeyFromPrefix("10"),
            branchKeyFromPrefix("100"),
            branchKeyFromPrefix("10"),
            "[10] | [100] -> [10]"),
        Arguments.of(
            branchKeyFromPrefix("0"),
            branchKeyFromPrefix("00"),
            branchKeyFromPrefix("0"),
            "[0] | [00] -> [0]"),
        Arguments.of(
            branchKeyFromPrefix("00"),
            branchKeyFromPrefix("000"),
            branchKeyFromPrefix("00"),
            "[00] | [000] -> [00]"),
        // ## Leaf keys:
        Arguments.of(
            leafKeyFromPrefix("11"),
            leafKeyFromPrefix("11"),
            // In practice no two equal leaves shall appear in the same proof tree
            leafKeyFromPrefix("11"),
            "[11] | [11] -> [11]"),
        Arguments.of(
            leafKeyFromPrefix("1111 1111 | 10_11"),
            leafKeyFromPrefix("1111 1111 | 10"),
            branchKeyFromPrefix("1111 1111 | 10"),
            "[1111 1111 | 10_11] | [1111 1111 | 10] -> [1111 1111 | 10]")
    );
  }
}
