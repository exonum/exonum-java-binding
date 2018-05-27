package com.exonum.binding.storage.proofs.map;

import static com.exonum.binding.test.Bytes.bytes;
import static com.exonum.binding.test.Bytes.createPrefixed;
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
  public void commonPrefixTest() {
    int comparisonResult = firstKey.compareTo(secondKey);
    assertThat(comparisonResult > 0, equalTo(expectedResult));
  }

  @Parameters(name = "{index} = {3}")
  public static Collection<Object[]> testData() {
    return Arrays.asList(
        // "A | B -> C" reads "C is a common prefix of A and B"
        parameters(
            DbKey.newBranchKey(createUserKey(0b1100), 4),
            DbKey.newBranchKey(createUserKey(0b0), 1),
            true,
            "[1100] > [0]"),
        parameters(
            DbKey.newBranchKey(createUserKey(0b1100), 4),
            DbKey.newBranchKey(createUserKey(0b001101), 6),
            false,
            "[1100] < [001101]"),
        parameters(
            DbKey.newBranchKey(createUserKey(0b001101), 6),
            DbKey.newBranchKey(createUserKey(0b00110101), 8),
            true,
            "[001101] < [00110101]"),
        parameters(
            DbKey.newLeafKey(createUserKey(0b101)),
            DbKey.newLeafKey(createUserKey(0b110)),
            true,
            "[101] < [110]"));
  }

  /** Creates a 32-byte long user key with the given byte prefix. */
  private static byte[] createUserKey(int... bytePrefix) {
    return createPrefixed(bytes(bytePrefix), DbKey.KEY_SIZE);
  }
}
