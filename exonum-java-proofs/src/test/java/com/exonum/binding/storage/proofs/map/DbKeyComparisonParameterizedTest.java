package com.exonum.binding.storage.proofs.map;

import static com.exonum.binding.storage.proofs.map.DbKeyTestUtils.branchKeyFromPrefix;
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
  public void dbKeyCompareTest() {
    int comparisonResult = firstKey.compareTo(secondKey);

    assertThat(comparisonResult > 0, equalTo(expectedResult));
  }

  @Parameters(name = "{index} = {3}")
  public static Collection<Object[]> testData() {
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
