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

import static com.exonum.binding.common.proofs.map.DbKeyComparisonParameterizedTest.ComparisonResult.EQUAL;
import static com.exonum.binding.common.proofs.map.DbKeyComparisonParameterizedTest.ComparisonResult.GREATER;
import static com.exonum.binding.common.proofs.map.DbKeyComparisonParameterizedTest.ComparisonResult.LESS;
import static com.exonum.binding.common.proofs.map.DbKeyTestUtils.branchKeyFromPrefix;
import static com.exonum.binding.common.proofs.map.DbKeyTestUtils.leafKeyFromPrefix;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DbKeyComparisonParameterizedTest {

  @ParameterizedTest(name = "[{index}] => {3}")
  @MethodSource("testData")
  void dbKeyCompareTest(DbKey firstKey, DbKey secondKey, ComparisonResult expectedResult,
      String description) {
    int comparisonResult = firstKey.compareTo(secondKey);

    assertThat(ComparisonResult.fromInt(comparisonResult), equalTo(expectedResult));
  }

  @ParameterizedTest
  @MethodSource("testData")
  void dbKeyComparatorSymmetric(DbKey firstKey, DbKey secondKey, ComparisonResult firstToSecond,
      String description) {
    int comparisonResult = secondKey.compareTo(firstKey);

    ComparisonResult expectedResult = firstToSecond.opposite();
    assertThat(ComparisonResult.fromInt(comparisonResult), equalTo(expectedResult));
  }

  private static List<Arguments> testData() {
    return Arrays.asList(
        // "A > B" reads "Key A is bigger than key B"
        Arguments.of(
            branchKeyFromPrefix("1100"),
            branchKeyFromPrefix("0"),
            GREATER,
            "[1100] > [0]"),
        Arguments.of(
            branchKeyFromPrefix("1100"),
            branchKeyFromPrefix("001101"),
            GREATER,
            "[1100] > [001101]"),
        Arguments.of(
            branchKeyFromPrefix("001101"),
            branchKeyFromPrefix("00110101"),
            LESS,
            "[001101] < [00110101]"),
        Arguments.of(
            branchKeyFromPrefix("101"),
            branchKeyFromPrefix("110"),
            LESS,
            "[101] < [110]"),
        Arguments.of(
            branchKeyFromPrefix("1"),
            branchKeyFromPrefix("1"),
            EQUAL,
            "[1] = [1] (branches)"),
        Arguments.of(
            leafKeyFromPrefix("0"),
            leafKeyFromPrefix("0"),
            EQUAL,
            "[0] = [0] (leaves)")
    );
  }

  enum ComparisonResult {
    GREATER,
    LESS,
    EQUAL;

    static ComparisonResult fromInt(int compareCode) {
      if (compareCode < 0) {
        return LESS;
      } else if (0 < compareCode) {
        return GREATER;
      } else {
        return EQUAL;
      }
    }

    /**
     * Returns the opposite comparison result — the one you would obtain if you swap
     * the arguments positions.
     */
    ComparisonResult opposite() {
      switch (this) {
        case GREATER: return LESS;
        case LESS: return GREATER;
        case EQUAL: return EQUAL;
        default: throw new AssertionError("unreachable");
      }
    }
  }
}
