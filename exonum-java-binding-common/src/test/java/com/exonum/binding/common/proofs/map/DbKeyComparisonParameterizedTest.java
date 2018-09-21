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
import static com.exonum.binding.test.TestParameters.parameters;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
@Disabled
public class DbKeyComparisonParameterizedTest {

  @Parameter(0)
  public DbKey firstKey;

  @Parameter(1)
  public DbKey secondKey;

  @Parameter(2)
  public boolean expectedResult;

  @Parameter(3)
  public String description;

  @Test
  void dbKeyCompareTest() {
    int comparisonResult = firstKey.compareTo(secondKey);

    assertThat(comparisonResult > 0, equalTo(expectedResult));
  }

  @Parameters(name = "{index} = {3}")
  static Collection<Object[]> testData() {
    return Arrays.asList(
        // "A > B" reads "Key A is bigger than key B"
        parameters(
            branchKeyFromPrefix("1100"),
            branchKeyFromPrefix("0"),
            true,
            "[1100] > [0]"),
        parameters(
            branchKeyFromPrefix("1100"),
            branchKeyFromPrefix("001101"),
            true,
            "[1100] > [001101]"),
        parameters(
            branchKeyFromPrefix("001101"),
            branchKeyFromPrefix("00110101"),
            false,
            "[001101] < [00110101]"),
        parameters(
            branchKeyFromPrefix("101"),
            branchKeyFromPrefix("110"),
            false,
            "[101] < [110]"));
  }
}
