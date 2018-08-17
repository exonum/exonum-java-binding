/* 
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.storage.proofs.map;

import static com.exonum.binding.storage.proofs.map.DbKeyTestUtils.branchDbKey;
import static com.exonum.binding.test.Bytes.bytes;
import static com.google.common.base.Preconditions.checkArgument;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import com.exonum.binding.storage.proofs.map.DbKey.Type;
import com.google.common.primitives.UnsignedBytes;
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
  public void throwsIfNull() {
    expectedException.expect(NullPointerException.class);
    DbKey.fromBytes(null);
  }

  @Test
  public void throwsIfInvalidNodeTypeCode() {
    byte[] rawDbKey = createDbKey(2, bytes("a"), 8);

    expectedException.expect(IllegalArgumentException.class);
    DbKey dbKey = DbKey.fromBytes(rawDbKey);
  }

  @Test
  public void throwsIfLeafHasInvalidSize() {
    int invalidNumBits = 10;
    byte[] rawDbKey = createDbKey(Type.LEAF.code, bytes("a"), invalidNumBits);

    expectedException.expect(IllegalArgumentException.class);
    DbKey dbKey = DbKey.fromBytes(rawDbKey);
  }

  @Test
  public void throwsIfBranchKeySliceHasBitsAfterSignificantPart0Bits() {
    if (MapProofValidator.PERFORM_TREE_CORRECTNESS_CHECKS) {
      int numSignificantBits = 0;
      byte[] rawDbKey = createDbKey(Type.BRANCH.code, bytes(0x01), numSignificantBits);

      expectedException.expect(IllegalArgumentException.class);
      DbKey dbKey = DbKey.fromBytes(rawDbKey);
    }
  }

  @Test
  public void throwsIfBranchKeySliceHasBitsAfterSignificantPart1Bit() {
    if (MapProofValidator.PERFORM_TREE_CORRECTNESS_CHECKS) {
      int numSignificantBits = 1;
      byte[] rawDbKey = createDbKey(Type.BRANCH.code, bytes(0x03), numSignificantBits);

      expectedException.expect(IllegalArgumentException.class);
      DbKey dbKey = DbKey.fromBytes(rawDbKey);
    }
  }

  @Test
  public void throwsIfBranchKeySliceHasBitsAfterSignificantPart7Bit() {
    if (MapProofValidator.PERFORM_TREE_CORRECTNESS_CHECKS) {
      int numSignificantBits = 7;
      byte[] rawDbKey = createDbKey(Type.BRANCH.code, bytes(0x80), numSignificantBits);

      expectedException.expect(IllegalArgumentException.class);
      DbKey dbKey = DbKey.fromBytes(rawDbKey);
    }
  }

  @Test
  public void throwsIfBranchKeySliceHasBitsAfterSignificantPart8Bit() {
    if (MapProofValidator.PERFORM_TREE_CORRECTNESS_CHECKS) {
      int numSignificantBits = 8;
      byte[] rawDbKey = createDbKey(Type.BRANCH.code, bytes(0xFF, 0x01), numSignificantBits);

      expectedException.expect(IllegalArgumentException.class);
      DbKey dbKey = DbKey.fromBytes(rawDbKey);
    }
  }

  @Test
  public void throwsIfBranchKeySliceHasBitsAfterSignificantPart12Bit() {
    if (MapProofValidator.PERFORM_TREE_CORRECTNESS_CHECKS) {
      int numSignificantBits = 12;
      byte[] rawDbKey = createDbKey(Type.BRANCH.code, bytes(0xFF, 0x1F), numSignificantBits);

      expectedException.expect(IllegalArgumentException.class);
      DbKey dbKey = DbKey.fromBytes(rawDbKey);
    }
  }

  @Test
  public void throwsIfBranchKeySliceHasBitsAfterSignificantPart16Bit() {
    if (MapProofValidator.PERFORM_TREE_CORRECTNESS_CHECKS) {
      int numSignificantBits = 16;
      byte[] rawDbKey = createDbKey(Type.BRANCH.code, bytes(0xFF, 0xFF, 0x01), numSignificantBits);

      expectedException.expect(IllegalArgumentException.class);
      DbKey dbKey = DbKey.fromBytes(rawDbKey);
    }
  }

  @Test
  public void getNodeType_BranchNode() {
    byte[] rawDbKey = createDbKey(Type.BRANCH.code, bytes("a"), 8);
    DbKey dbKey = DbKey.fromBytes(rawDbKey);

    assertThat(dbKey.getNodeType(), equalTo(Type.BRANCH));
  }

  @Test
  public void getNodeType_LeafNode() {
    byte[] rawDbKey = createDbKey(Type.LEAF.code, bytes("a"), 0);
    DbKey dbKey = DbKey.fromBytes(rawDbKey);

    assertThat(dbKey.getNodeType(), equalTo(Type.LEAF));
  }

  @Test
  public void getKeySlice_RootBranch() {
    byte[] keyPrefix = bytes();
    byte[] rawDbKey = createDbKey(Type.BRANCH.code, keyPrefix, keyPrefix.length * Byte.SIZE);
    DbKey dbKey = DbKey.fromBytes(rawDbKey);

    assertThat(dbKey.getKeySlice(), keySliceStartsWith(keyPrefix));
  }

  @Test
  public void getKeySlice_IntermediateBranch() {
    byte[] keyPrefix = bytes("abc");
    byte[] rawDbKey = createDbKey(Type.BRANCH.code, keyPrefix, keyPrefix.length * Byte.SIZE);
    DbKey dbKey = DbKey.fromBytes(rawDbKey);

    assertThat(dbKey.getKeySlice(), keySliceStartsWith(keyPrefix));
  }

  @Test
  public void getKeySlice_Leaf() {
    byte[] keyPrefix = bytes("abc");
    byte[] rawDbKey = createDbKey(Type.LEAF.code, keyPrefix, 0);
    DbKey dbKey = DbKey.fromBytes(rawDbKey);

    assertThat(dbKey.getKeySlice(), keySliceStartsWith(keyPrefix));
  }

  @Test
  public void getNumSignificantBits_RootBranch() {
    int numSignificantBits = 0;
    byte[] rawDbKey = createDbKey(Type.BRANCH.code, bytes(), numSignificantBits);
    DbKey dbKey = DbKey.fromBytes(rawDbKey);

    assertThat(dbKey.getNumSignificantBits(), equalTo(numSignificantBits));
  }

  @Test
  public void getNumSignificantBits_IntermediateBranch() {
    int numSignificantBits = 16;
    byte[] rawDbKey = createDbKey(Type.BRANCH.code, bytes("ab"), numSignificantBits);
    DbKey dbKey = DbKey.fromBytes(rawDbKey);

    assertThat(dbKey.getNumSignificantBits(), equalTo(numSignificantBits));
  }

  @Test
  public void getNumSignificantBits_IntermediateBranch9Bits() {
    int numSignificantBits = 9;
    byte[] rawDbKey = createDbKey(Type.BRANCH.code, bytes(0xFF, 0x01), numSignificantBits);
    DbKey dbKey = DbKey.fromBytes(rawDbKey);

    assertThat(dbKey.getNumSignificantBits(), equalTo(numSignificantBits));
  }

  @Test
  public void getNumSignificantBits_IntermediateBranch12Bits() {
    int numSignificantBits = 12;
    byte[] rawDbKey = createDbKey(Type.BRANCH.code, bytes(0xFF, 0x0F), numSignificantBits);
    DbKey dbKey = DbKey.fromBytes(rawDbKey);

    assertThat(dbKey.getNumSignificantBits(), equalTo(numSignificantBits));
  }

  @Test
  public void getNumSignificantBits_IntermediateBranch15Bits() {
    int numSignificantBits = 15;
    byte[] rawDbKey = createDbKey(Type.BRANCH.code, bytes(0xFF, 0x7F), numSignificantBits);
    DbKey dbKey = DbKey.fromBytes(rawDbKey);

    assertThat(dbKey.getNumSignificantBits(), equalTo(numSignificantBits));
  }

  @Test
  public void getNumSignificantBits_Leaf() {
    byte[] rawDbKey = createDbKey(Type.LEAF.code, bytes(0xFF), 0);
    DbKey dbKey = DbKey.fromBytes(rawDbKey);

    assertThat(dbKey.getNumSignificantBits(), equalTo(DbKey.KEY_SIZE_BITS));
  }

  @Test
  public void numSignificantBitsLowerThanZeroShouldThrow() {
    int numSignificantBits = -1;

    expectedException.expect(IllegalArgumentException.class);
    DbKey dbKey = branchDbKey(bytes(0xFF, 0xFF, 0x01), numSignificantBits);
  }

  @Test
  public void numSignificantBitsEqualToKeySizeBitsInBranchShouldThrow() {
    int numSignificantBits = DbKey.KEY_SIZE_BITS;

    expectedException.expect(IllegalArgumentException.class);
    DbKey dbKey = branchDbKey(bytes(0xFF, 0xFF, 0x01), numSignificantBits);
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
  public void testKeyBits_Leaf() {
    byte[] keyPrefix = bytes("abc");
    byte[] rawDbKey = createDbKey(Type.LEAF.code, keyPrefix, 0);
    DbKey dbKey = DbKey.fromBytes(rawDbKey);

    KeyBitSet expectedKeyBits = new KeyBitSet(keyPrefix, DbKey.KEY_SIZE_BITS);
    KeyBitSet keyBits = dbKey.keyBits();

    assertThat(keyBits, equalTo(expectedKeyBits));
  }

  @Test
  public void testKeyBits_RootBranch() {
    byte[] keyPrefix = bytes();
    int numSignificantBits = 0;
    byte[] rawDbKey = createDbKey(Type.BRANCH.code, keyPrefix, numSignificantBits);
    DbKey dbKey = DbKey.fromBytes(rawDbKey);

    KeyBitSet expectedKeyBits = new KeyBitSet(keyPrefix, numSignificantBits);
    KeyBitSet keyBits = dbKey.keyBits();

    assertThat(keyBits, equalTo(expectedKeyBits));
  }

  @Test
  public void testKeyBits_IntermediateBranch() {
    byte[] keyPrefix = bytes("abc");
    int numSignificantBits = keyPrefix.length * Byte.SIZE;
    byte[] rawDbKey = createDbKey(Type.BRANCH.code, keyPrefix, numSignificantBits);
    DbKey dbKey = DbKey.fromBytes(rawDbKey);

    KeyBitSet expectedKeyBits = new KeyBitSet(keyPrefix, numSignificantBits);
    KeyBitSet keyBits = dbKey.keyBits();

    assertThat(keyBits, equalTo(expectedKeyBits));
  }
}
