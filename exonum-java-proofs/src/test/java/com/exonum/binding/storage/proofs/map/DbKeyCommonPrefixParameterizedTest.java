package com.exonum.binding.storage.proofs.map;

import static com.exonum.binding.test.Bytes.bytes;
import static com.exonum.binding.test.Bytes.createPrefixed;
import static com.exonum.binding.test.TestParameters.parameters;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.BitSet;
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
        parameters(
            branchKeyFromPrefix("01"),
            branchKeyFromPrefix("10"),
            branchKeyFromPrefix(""),
            "[01] | [10] -> []"),
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
            "[11111] | [11101] -> [1]"));
  }

  /**
   * Returns a new branch db key with the given prefix. The number of significant bits
   * is equal to the number of bits in the string (excluding whitespaces and delimiters).
   *
   * @param prefix a key prefix — from the least significant bit to the most significant,
   *               i.e., "00 01" is 8, "10 00" is 1.
   *               May contain spaces, underscores or bars (e.g., "00 01|01 11" and "11_10"
   *               are valid strings).
   */
  private static DbKey branchKeyFromPrefix(String prefix) {
    // Replace spaces that may be used to separate groups of binary digits
    prefix = prefix.replaceAll("[ _|]", "");
    // Check the string is correct
    assert prefix.matches("[01]*");
    assert prefix.length() <= DbKey.KEY_SIZE_BITS;

    BitSet keyPrefixBits = new BitSet(prefix.length());
    for (int i = 0; i < prefix.length(); i++) {
      char bit = prefix.charAt(i);
      if (bit == '1') {
        keyPrefixBits.set(i);
      }
    }

    byte[] fullKeySlice = createPrefixed(keyPrefixBits.toByteArray(), DbKey.KEY_SIZE);
    return DbKey.newBranchKey(fullKeySlice, prefix.length());
  }
}
