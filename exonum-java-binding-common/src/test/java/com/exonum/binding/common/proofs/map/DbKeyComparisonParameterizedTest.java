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
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DbKeyComparisonParameterizedTest {

  @ParameterizedTest(name = "{index} => description={3}")
  @MethodSource("testData")
  void dbKeyCompareTest(DbKey firstKey, DbKey secondKey, boolean expectedResult,
      String description) {
    int comparisonResult = firstKey.compareTo(secondKey);

    assertThat(comparisonResult > 0, equalTo(expectedResult));
  }

  private static List<Arguments> testData() {
    return Arrays.asList(
        // "A > B" reads "Key A is bigger than key B"
        Arguments.of(
            branchKeyFromPrefix("1100"),
            branchKeyFromPrefix("0"),
            true,
            "[1100] > [0]"),
        Arguments.of(
            branchKeyFromPrefix("1100"),
            branchKeyFromPrefix("001101"),
            true,
            "[1100] > [001101]"),
        Arguments.of(
            branchKeyFromPrefix("001101"),
            branchKeyFromPrefix("00110101"),
            false,
            "[001101] < [00110101]"),
        Arguments.of(
            branchKeyFromPrefix("101"),
            branchKeyFromPrefix("110"),
            false,
            "[101] < [110]")
    );
  }
}
