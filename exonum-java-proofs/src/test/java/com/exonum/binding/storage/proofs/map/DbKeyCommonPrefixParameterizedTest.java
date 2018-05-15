package com.exonum.binding.storage.proofs.map;

import static com.exonum.binding.test.Bytes.bytes;
import static com.exonum.binding.test.Bytes.createPrefixed;
import static com.exonum.binding.test.TestParameters.parameters;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import com.exonum.binding.storage.proofs.map.DbKey.Type;
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
    assertThat(actualCommonPrefixKey.getKeySlice(), equalTo(expectedResultKey.getKeySlice()));
  }

  @Parameters(name = "{index} = {3}")
  public static Collection<Object[]> testData() {
    return Arrays.asList(
        // "A | B -> C" reads "C is a common prefix of A and B"
        parameters(
            new DbKey(Type.BRANCH, createPrefixed(bytes(0b01), DbKey.KEY_SIZE), DbKey.KEY_SIZE),
            new DbKey(Type.BRANCH, createPrefixed(bytes(0b10), DbKey.KEY_SIZE), DbKey.KEY_SIZE),
            new DbKey(Type.BRANCH, createPrefixed(bytes(), DbKey.KEY_SIZE), DbKey.KEY_SIZE),
            "[01] | [10] -> []"),
        parameters(
            new DbKey(Type.LEAF, createPrefixed(bytes(0b1011), DbKey.KEY_SIZE), DbKey.KEY_SIZE_BITS),
            new DbKey(Type.LEAF, createPrefixed(bytes(0b1111), DbKey.KEY_SIZE), DbKey.KEY_SIZE_BITS),
            new DbKey(Type.BRANCH, createPrefixed(bytes(0b11), DbKey.KEY_SIZE), DbKey.KEY_SIZE),
            "[1011] | [1111] -> [11]"),
        parameters(
            new DbKey(Type.LEAF, createPrefixed(bytes(0b00), DbKey.KEY_SIZE), DbKey.KEY_SIZE_BITS),
            new DbKey(Type.LEAF, createPrefixed(bytes(0b0000), DbKey.KEY_SIZE), DbKey.KEY_SIZE_BITS),
            new DbKey(Type.BRANCH, createPrefixed(bytes(0b0), DbKey.KEY_SIZE), DbKey.KEY_SIZE),
            "[00] | [0000] -> [0]"),
        parameters(
            new DbKey(Type.BRANCH, createPrefixed(bytes(0b0101), DbKey.KEY_SIZE), DbKey.KEY_SIZE),
            new DbKey(Type.BRANCH, createPrefixed(bytes(0b11101), DbKey.KEY_SIZE), DbKey.KEY_SIZE),
            new DbKey(Type.BRANCH, createPrefixed(bytes(0b00101), DbKey.KEY_SIZE), DbKey.KEY_SIZE),
            "[0101] | [11101] -> [101]"));
  }
}
