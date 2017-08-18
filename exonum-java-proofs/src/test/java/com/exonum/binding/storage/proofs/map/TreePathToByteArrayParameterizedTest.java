package com.exonum.binding.storage.proofs.map;

import static com.exonum.binding.test.Bytes.bytes;
import static com.exonum.binding.test.TestParameters.parameters;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TreePathToByteArrayParameterizedTest {

  @Parameter(0)
  public byte[] pathBytes;

  @Parameter(1)
  public int pathLength;

  @Parameter(2)
  public String description;

  private TreePath path;

  @Before
  public void setUp() throws Exception {
    path = new TreePath(BitSet.valueOf(pathBytes), pathLength);
  }

  @Test
  public void toByteArray() throws Exception {
    assertThat(path.toByteArray(), equalTo(pathBytes));
  }

  @Parameters(name = "{index} = {2}")
  public static Collection<Object[]> testData() {
    return Arrays.asList(
        // "A <- B" reads "A is a prefix of B"
        // "!P" reads "not P"
        parameters(bytes(), 0, "[]"),

        parameters(bytes(), 2, "[00]"),
        parameters(bytes(0x0F), 4, "[1111]"),
        parameters(bytes(0x0F), 5, "[1111 0]"),

        parameters(bytes(0x00, 0xFF), 2 * Byte.SIZE, "[00 FF]"),
        parameters(bytes(0x00, 0xFF, 0xEA, 0x01), 4 * Byte.SIZE, "[00 FF EA 01]")
    );
  }
}
