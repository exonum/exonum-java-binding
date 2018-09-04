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

package com.exonum.binding.storage.proofs.map;

import static com.exonum.binding.storage.proofs.map.DbKeyTestUtils.branchKeyFromPrefix;
import static com.exonum.binding.storage.proofs.map.DbKeyTestUtils.leafKeyFromPrefix;
import static com.exonum.binding.test.TestParameters.parameters;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DbKeyCommonPrefixParameterizedTest {

  @Parameter(0)
  public DbKey firstKey;

  @Parameter(1)
  public DbKey secondKey;

  @Parameter(2)
  public DbKey expectedResultKey;

  @Parameter(3)
  public String description;

  @Test
  public void commonPrefix() {
    DbKey actualCommonPrefixKey = firstKey.commonPrefix(secondKey);
    assertThat(actualCommonPrefixKey, equalTo(expectedResultKey));
  }

  @Test
  public void commonPrefixCommutative() {
    DbKey actualCommonPrefixKey = secondKey.commonPrefix(firstKey);
    assertThat(actualCommonPrefixKey, equalTo(expectedResultKey));
  }

  @Test
  public void commonPrefixOfSelf() {
    DbKey commonPrefix = firstKey.commonPrefix(firstKey);
    assertThat(commonPrefix, sameInstance(firstKey));
  }

  @Test
  public void commonPrefixOfEqualKey() {
    DbKey firstKeyClone = DbKey.fromBytes(firstKey.getRawDbKey());
    DbKey commonPrefix = firstKey.commonPrefix(firstKeyClone);
    assertThat(commonPrefix, equalTo(firstKey));
  }

  @Parameters(name = "{index} = {3}")
  public static Collection<Object[]> testData() {
    return Arrays.asList(
        // "A | B -> C" reads "C is a common prefix of A and B"
        // # Not a prefix:
        parameters(
            branchKeyFromPrefix("0"),
            branchKeyFromPrefix("1"),
            branchKeyFromPrefix(""),
            "[0] | [1] -> []"),
        parameters(
            branchKeyFromPrefix("01"),
            branchKeyFromPrefix("10"),
            branchKeyFromPrefix(""),
            "[01] | [10] -> []"),
        // # Prefixes of various length:
        parameters(
            branchKeyFromPrefix("1010"),
            branchKeyFromPrefix("10111"),
            branchKeyFromPrefix("101"),
            "[1010] | [10111] -> [101]"),
        parameters(
            branchKeyFromPrefix("1101"),
            branchKeyFromPrefix("11011"),
            branchKeyFromPrefix("1101"),
            "[1101] | [11011] -> [1101]"),
        parameters(
            branchKeyFromPrefix("11101"),
            branchKeyFromPrefix("11111"),
            branchKeyFromPrefix("111"),
            "[11101] | [11111] -> [111]"),
        parameters(
            branchKeyFromPrefix("11111"),
            branchKeyFromPrefix("10111"),
            branchKeyFromPrefix("1"),
            "[11111] | [11101] -> [1]"),
        // ## Multi-byte keys
        parameters(
            branchKeyFromPrefix("1111 1111 | 10_11"),
            branchKeyFromPrefix("1111 1111 | 10_00"),
            branchKeyFromPrefix("1111 1111 | 10"),
            "[1111 1111 | 10_11] | [1111 1111 | 10_00] -> [1111 1111 | 10]"),
        // ## One is full prefix of another:
        parameters(
            branchKeyFromPrefix(""),
            branchKeyFromPrefix("1"),
            branchKeyFromPrefix(""),
            "[] | [1] -> []"),
        parameters(
            branchKeyFromPrefix(""),
            branchKeyFromPrefix("0"),
            branchKeyFromPrefix(""),
            "[] | [0] -> []"),
        parameters(
            branchKeyFromPrefix("1"),
            branchKeyFromPrefix("11"),
            branchKeyFromPrefix("1"),
            "[1] | [11] -> [1]"),
        parameters(
            branchKeyFromPrefix("10"),
            branchKeyFromPrefix("100"),
            branchKeyFromPrefix("10"),
            "[10] | [100] -> [10]"),
        parameters(
            branchKeyFromPrefix("0"),
            branchKeyFromPrefix("00"),
            branchKeyFromPrefix("0"),
            "[0] | [00] -> [0]"),
        parameters(
            branchKeyFromPrefix("00"),
            branchKeyFromPrefix("000"),
            branchKeyFromPrefix("00"),
            "[00] | [000] -> [00]"),
        // ## Leaf keys:
        parameters(
            leafKeyFromPrefix("11"),
            leafKeyFromPrefix("11"),
            // In practice no two equal leaves shall appear in the same proof tree
            leafKeyFromPrefix("11"),
            "[11] | [11] -> [11]"),
        parameters(
            leafKeyFromPrefix("1111 1111 | 10_11"),
            leafKeyFromPrefix("1111 1111 | 10"),
            branchKeyFromPrefix("1111 1111 | 10"),
            "[1111 1111 | 10_11] | [1111 1111 | 10] -> [1111 1111 | 10]")
    );
  }
}
