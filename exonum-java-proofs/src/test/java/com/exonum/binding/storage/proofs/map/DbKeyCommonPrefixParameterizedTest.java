package com.exonum.binding.storage.proofs.map;

import static com.exonum.binding.storage.proofs.map.DbKeyTestUtils.branchKeyFromPrefix;
import static com.exonum.binding.storage.proofs.map.DbKeyTestUtils.leafKeyFromPrefix;
import static com.exonum.binding.test.TestParameters.parameters;
import static org.hamcrest.CoreMatchers.equalTo;
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
  public void commonPrefixTest() {
    DbKey actualCommonPrefixKey = firstKey.commonPrefix(secondKey);
    assertThat(actualCommonPrefixKey, equalTo(expectedResultKey));
  }

  @Parameters(name = "{index} = {3}")
  public static Collection<Object[]> testData() {
    return Arrays.asList(
        // "A | B -> C" reads "C is a common prefix of A and B"
        // # Not a prefix:
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
            branchKeyFromPrefix("11"),
            branchKeyFromPrefix("111"),
            branchKeyFromPrefix("11"),
            "[11] | [111] -> [11]"),
        parameters(
            branchKeyFromPrefix("10"),
            branchKeyFromPrefix("100"),
            branchKeyFromPrefix("10"),
            "[10] | [100] -> [10]"),
        parameters(
            branchKeyFromPrefix("100"),
            branchKeyFromPrefix("10"),
            branchKeyFromPrefix("10"),
            "[100] | [10] -> [10]"),
        parameters(
            branchKeyFromPrefix("0000"),
            branchKeyFromPrefix("00"),
            branchKeyFromPrefix("00"),
            "[0000] | [00] -> [00]"),
        parameters(
            branchKeyFromPrefix("0"),
            branchKeyFromPrefix("000"),
            branchKeyFromPrefix("0"),
            "[0] | [000] -> [0]"),
        // ## Equal keys:
        parameters(
            branchKeyFromPrefix("00"),
            branchKeyFromPrefix("00"),
            branchKeyFromPrefix("00"),
            "[00] | [00] -> [00]"),
        parameters(
            branchKeyFromPrefix("11"),
            branchKeyFromPrefix("11"),
            branchKeyFromPrefix("11"),
            "[11] | [11] -> [11]"),
        // ## Leaf keys:
        parameters(
            leafKeyFromPrefix("11"),
            leafKeyFromPrefix("11"),
            // In practice two leaves shouldn't be equal
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
