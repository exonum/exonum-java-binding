package com.exonum.binding.storage.proofs.map;

import static com.exonum.binding.test.Bytes.bytes;
import static com.google.common.base.Preconditions.checkArgument;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import com.exonum.binding.storage.proofs.map.DbKey.Type;
import com.google.common.primitives.UnsignedBytes;
import java.util.BitSet;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DbKeyTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void throwsIfInvalidNodeTypeCode() throws Exception {
    byte[] rawDbKey = createDbKey(2, bytes("a"), 8);

    expectedException.expect(IllegalArgumentException.class);
    DbKey dbKey = new DbKey(rawDbKey);
  }

  @Test
  public void throwsIfLeafHasInvalidSize() throws Exception {
    int invalidNumBits = 10;
    byte[] rawDbKey = createDbKey(Type.LEAF.code, bytes("a"), invalidNumBits);

    expectedException.expect(IllegalArgumentException.class);
    DbKey dbKey = new DbKey(rawDbKey);
  }

  @Test
  public void throwsIfBranchKeySliceHasBitsAfterSignificantPart0Bits() throws Exception {
    int numSignificantBits = 0;
    byte[] rawDbKey = createDbKey(Type.BRANCH.code, bytes(0x01), numSignificantBits);

    expectedException.expect(IllegalArgumentException.class);
    DbKey dbKey = new DbKey(rawDbKey);
  }

  @Test
  public void throwsIfBranchKeySliceHasBitsAfterSignificantPart1Bit() throws Exception {
    int numSignificantBits = 1;
    byte[] rawDbKey = createDbKey(Type.BRANCH.code, bytes(0x03), numSignificantBits);

    expectedException.expect(IllegalArgumentException.class);
    DbKey dbKey = new DbKey(rawDbKey);
  }

  @Test
  public void throwsIfBranchKeySliceHasBitsAfterSignificantPart7Bit() throws Exception {
    int numSignificantBits = 7;
    byte[] rawDbKey = createDbKey(Type.BRANCH.code, bytes(0x80), numSignificantBits);

    expectedException.expect(IllegalArgumentException.class);
    DbKey dbKey = new DbKey(rawDbKey);
  }

  @Test
  public void throwsIfBranchKeySliceHasBitsAfterSignificantPart8Bit() throws Exception {
    int numSignificantBits = 8;
    byte[] rawDbKey = createDbKey(Type.BRANCH.code, bytes(0xFF, 0x01), numSignificantBits);

    expectedException.expect(IllegalArgumentException.class);
    DbKey dbKey = new DbKey(rawDbKey);
  }

  @Test
  public void throwsIfBranchKeySliceHasBitsAfterSignificantPart12Bit() throws Exception {
    int numSignificantBits = 12;
    byte[] rawDbKey = createDbKey(Type.BRANCH.code, bytes(0xFF, 0x1F), numSignificantBits);

    expectedException.expect(IllegalArgumentException.class);
    DbKey dbKey = new DbKey(rawDbKey);
  }

  @Test
  public void throwsIfBranchKeySliceHasBitsAfterSignificantPart16Bit() throws Exception {
    int numSignificantBits = 16;
    byte[] rawDbKey = createDbKey(Type.BRANCH.code, bytes(0xFF, 0xFF, 0x01), numSignificantBits);

    expectedException.expect(IllegalArgumentException.class);
    DbKey dbKey = new DbKey(rawDbKey);
  }

  @Test
  public void getNodeType_BranchNode() throws Exception {
    byte[] rawDbKey = createDbKey(Type.BRANCH.code, bytes("a"), 8);
    DbKey dbKey = new DbKey(rawDbKey);

    assertThat(dbKey.getNodeType(), equalTo(Type.BRANCH));
  }

  @Test
  public void getNodeType_LeafNode() throws Exception {
    byte[] rawDbKey = createDbKey(Type.LEAF.code, bytes("a"), 0);
    DbKey dbKey = new DbKey(rawDbKey);

    assertThat(dbKey.getNodeType(), equalTo(Type.LEAF));
  }

  @Test
  public void getKeySlice_RootBranch() throws Exception {
    byte[] keyPrefix = bytes();
    byte[] rawDbKey = createDbKey(Type.BRANCH.code, keyPrefix, keyPrefix.length * Byte.SIZE);
    DbKey dbKey = new DbKey(rawDbKey);

    assertThat(dbKey.getKeySlice(), keySliceStartsWith(keyPrefix));
  }

  @Test
  public void getKeySlice_IntermediateBranch() throws Exception {
    byte[] keyPrefix = bytes("abc");
    byte[] rawDbKey = createDbKey(Type.BRANCH.code, keyPrefix, keyPrefix.length * Byte.SIZE);
    DbKey dbKey = new DbKey(rawDbKey);

    assertThat(dbKey.getKeySlice(), keySliceStartsWith(keyPrefix));
  }

  @Test
  public void getKeySlice_Leaf() throws Exception {
    byte[] keyPrefix = bytes("abc");
    byte[] rawDbKey = createDbKey(Type.LEAF.code, keyPrefix, 0);
    DbKey dbKey = new DbKey(rawDbKey);

    assertThat(dbKey.getKeySlice(), keySliceStartsWith(keyPrefix));
  }

  @Test
  public void getNumSignificantBits_RootBranch() throws Exception {
    int numSignificantBits = 0;
    byte[] rawDbKey = createDbKey(Type.BRANCH.code, bytes(), numSignificantBits);
    DbKey dbKey = new DbKey(rawDbKey);

    assertThat(dbKey.getNumSignificantBits(), equalTo(numSignificantBits));
  }

  @Test
  public void getNumSignificantBits_IntermediateBranch() throws Exception {
    int numSignificantBits = 16;
    byte[] rawDbKey = createDbKey(Type.BRANCH.code, bytes("ab"), numSignificantBits);
    DbKey dbKey = new DbKey(rawDbKey);

    assertThat(dbKey.getNumSignificantBits(), equalTo(numSignificantBits));
  }

  @Test
  public void getNumSignificantBits_IntermediateBranch9Bits() throws Exception {
    int numSignificantBits = 9;
    byte[] rawDbKey = createDbKey(Type.BRANCH.code, bytes(0xFF, 0x01), numSignificantBits);
    DbKey dbKey = new DbKey(rawDbKey);

    assertThat(dbKey.getNumSignificantBits(), equalTo(numSignificantBits));
  }

  @Test
  public void getNumSignificantBits_IntermediateBranch12Bits() throws Exception {
    int numSignificantBits = 12;
    byte[] rawDbKey = createDbKey(Type.BRANCH.code, bytes(0xFF, 0x0F), numSignificantBits);
    DbKey dbKey = new DbKey(rawDbKey);

    assertThat(dbKey.getNumSignificantBits(), equalTo(numSignificantBits));
  }

  @Test
  public void getNumSignificantBits_IntermediateBranch15Bits() throws Exception {
    int numSignificantBits = 15;
    byte[] rawDbKey = createDbKey(Type.BRANCH.code, bytes(0xFF, 0x7F), numSignificantBits);
    DbKey dbKey = new DbKey(rawDbKey);

    assertThat(dbKey.getNumSignificantBits(), equalTo(numSignificantBits));
  }

  @Test
  public void getNumSignificantBits_Leaf() throws Exception {
    byte[] rawDbKey = createDbKey(Type.LEAF.code, bytes(0xFF), 0);
    DbKey dbKey = new DbKey(rawDbKey);

    assertThat(dbKey.getNumSignificantBits(), equalTo(DbKey.KEY_SIZE_BITS));
  }

  /**
   * The method intentionally doesn't check if type & numSignificantBits are valid, so that
   * it may be used to test how DbKey handles invalid inputs.
   */
  private static byte[] createDbKey(int type, byte[] keySlice, int numSignificantBits) {
    byte[] rawDbKey = new byte[DbKey.DB_KEY_SIZE];
    rawDbKey[0] = UnsignedBytes.checkedCast(type);
    System.arraycopy(keySlice, 0, rawDbKey, 1, Math.min(DbKey.KEY_SIZE, keySlice.length));
    rawDbKey[DbKey.DB_KEY_SIZE - 1] = UnsignedBytes.checkedCast(numSignificantBits);
    return rawDbKey;
  }

  private static Matcher<byte[]> keySliceStartsWith(byte[] prefix) {
    checkArgument(prefix.length <= DbKey.KEY_SIZE);
    return new TypeSafeMatcher<byte[]>() {
      @Override
      protected boolean matchesSafely(byte[] key) {
        if (key.length != DbKey.KEY_SIZE) {
          return false;
        }
        int i = 0;
        while (i < prefix.length) {
          if (key[i] != prefix[i]) {
            return false;
          }
          i++;
        }
        while (i < key.length) {
          if (key[i] != 0) {
            return false;
          }
          i++;
        }
        return true;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("key of size ").appendText(String.valueOf(DbKey.KEY_SIZE))
            .appendText("starting with ").appendValueList("[", ", ", "]", prefix);
      }
    };
  }

  @Test
  public void testToPath_Leaf() throws Exception {
    byte[] keyPrefix = bytes("abc");
    byte[] rawDbKey = createDbKey(Type.LEAF.code, keyPrefix, 0);
    DbKey dbKey = new DbKey(rawDbKey);

    TreePath expectedPath = new TreePath(BitSet.valueOf(keyPrefix), DbKey.KEY_SIZE_BITS,
        DbKey.KEY_SIZE_BITS);
    TreePath path = dbKey.toPath();

    assertThat(path, equalTo(expectedPath));
  }

  @Test
  public void testToPath_RootBranch() throws Exception {
    byte[] keyPrefix = bytes();
    int numSignificantBits = 0;
    byte[] rawDbKey = createDbKey(Type.BRANCH.code, keyPrefix, numSignificantBits);
    DbKey dbKey = new DbKey(rawDbKey);

    TreePath expectedPath = new TreePath(BitSet.valueOf(keyPrefix), numSignificantBits,
        DbKey.KEY_SIZE_BITS);
    TreePath path = dbKey.toPath();

    assertThat(path, equalTo(expectedPath));
  }

  @Test
  public void testToPath_IntermediateBranch() throws Exception {
    byte[] keyPrefix = bytes("abc");
    int numSignificantBits = keyPrefix.length * Byte.SIZE;
    byte[] rawDbKey = createDbKey(Type.BRANCH.code, keyPrefix, numSignificantBits);
    DbKey dbKey = new DbKey(rawDbKey);

    TreePath expectedPath = new TreePath(BitSet.valueOf(keyPrefix), numSignificantBits,
        DbKey.KEY_SIZE_BITS);
    TreePath path = dbKey.toPath();

    assertThat(path, equalTo(expectedPath));
  }
}
