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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Objects;
import com.google.common.primitives.UnsignedBytes;
import java.util.Arrays;
import java.util.BitSet;

/**
 * A MapProof database key.
 *
 * <p>It includes:
 * <ul>
 *   <li>Node type: a branch or a leaf</li>
 *   <li>The key to which the value is mapped for leaf nodes;
 *       the common prefix of keys in the left and right sub-trees</li>
 *   <li>The size of the common prefix in branch nodes.</li>
 * </ul>
 */
public final class DbKey implements Comparable<DbKey> {

  /**
   * Type of the node in a Merkle-Patricia tree.
   */
  public enum Type {
    BRANCH(0),
    LEAF(1);

    /**
     * Branch type code, used as the first byte in a raw database key.
     */
    public final byte code;

    Type(int code) {
      this.code = (byte) code;
    }

    private static Type from(byte code) {
      if (code == BRANCH.code) {
        return BRANCH;
      } else if (code == LEAF.code) {
        return LEAF;
      } else {
        throw new IllegalArgumentException("Invalid node code: " + code);
      }
    }
  }

  /**
   * Size of the user key in bytes.
   */
  public static final int KEY_SIZE = 32;

  /**
   * Size of the user key in bits.
   */
  public static final int KEY_SIZE_BITS = KEY_SIZE * Byte.SIZE;

  /**
   * Size of the database key in bytes.
   */
  public static final int DB_KEY_SIZE = KEY_SIZE + 2;

  private final byte[] rawDbKey;

  private final Type nodeType;

  private final byte[] keySlice;

  private final int numSignificantBits;

  /**
   * Creates a database key.
   *
   * @param rawDbKey a raw database key, as used in the underlying storage
   * @throws NullPointerException if the key is null
   * @throws IllegalArgumentException if the specified database key is not valid
   */
  public static DbKey fromBytes(byte[] rawDbKey) {
    return new DbKey(rawDbKey);
  }

  private DbKey(byte[] rawDbKey) {
    checkArgument(rawDbKey.length == DB_KEY_SIZE,
        "Database key has illegal size: %s", rawDbKey.length);
    this.rawDbKey = rawDbKey.clone();  // TODO: when you copy, and when not?
    nodeType = Type.from(rawDbKey[0]);
    keySlice = new byte[KEY_SIZE];  // TODO: lazy copy?
    System.arraycopy(rawDbKey, 1, keySlice, 0, KEY_SIZE);
    int numSignificantBits = Byte.toUnsignedInt(rawDbKey[DB_KEY_SIZE - 1]);
    switch (nodeType) {
      case BRANCH:
        checkArgument(0 <= numSignificantBits && numSignificantBits < KEY_SIZE_BITS,
            "Invalid end index: %s", numSignificantBits);
        checkBranchKeySlice(keySlice, numSignificantBits);
        this.numSignificantBits = numSignificantBits;
        break;
      case LEAF:
        checkArgument(numSignificantBits == 0,
            "Invalid last byte: %s, must be zero" + numSignificantBits);
        this.numSignificantBits = KEY_SIZE_BITS;
        break;
      default:
        throw new AssertionError("Unreachable");
    }
  }

  private DbKey(DbKey.Type nodeType, byte[] keySlice, int numSignificantBits) {
    this.nodeType = nodeType;
    this.keySlice = keySlice;
    this.numSignificantBits = numSignificantBits;
    this.rawDbKey = new byte[DB_KEY_SIZE];
    rawDbKey[0] = nodeType.code;
    rawDbKey[DB_KEY_SIZE - 1] = (numSignificantBits == KEY_SIZE_BITS) ? 0
        : UnsignedBytes.checkedCast(numSignificantBits);
    System.arraycopy(keySlice, 0, rawDbKey, 1, KEY_SIZE);
  }

  /**
   * Given key as a byte array, returns new leaf DbKey.
   */
  public static DbKey newLeafKey(byte[] keySlice) {
    checkArgument(keySlice.length == KEY_SIZE);
    return new DbKey(Type.LEAF, keySlice, KEY_SIZE_BITS);
  }

  /**
   * Creates a new branch database key.
   * @param keySlice key as a byte array, must be 32-byte long
   * @param numSignificantBits the number of significant bits in the key (= the prefix size)
   * @throws IllegalArgumentException if key has invalid length or contains set bits
   *     after the prefix; if numSignificantBits is not in range [0, 255]
   */
  public static DbKey newBranchKey(byte[] keySlice, int numSignificantBits) {
    checkArgument(keySlice.length == KEY_SIZE);
    checkArgument(0 <= numSignificantBits && numSignificantBits < KEY_SIZE_BITS);
    checkBranchKeySlice(keySlice, numSignificantBits);
    return new DbKey(Type.BRANCH, keySlice, numSignificantBits);
  }

  private static void checkBranchKeySlice(byte[] keySlice, int numSignificantBits) {
    BitSet keyBits = BitSet.valueOf(keySlice);
    checkArgument(keyBits.length() <= numSignificantBits,
        "Branch key slice contains set bits after its numSignificantBits (%s): "
            + "length=%s, keyBits=%s", numSignificantBits, keyBits.length(), keyBits);
  }

  /**
   * Returns the type of the Merkle-Patricia tree node corresponding to this database key.
   */
  public Type getNodeType() {
    return nodeType;
  }

  /**
   * Returns the key slice. It's size is equal to {@link #KEY_SIZE}, but the number of significant
   * <em>bits</em> is equal to the {@link #getNumSignificantBits}.
   */
  public byte[] getKeySlice() {
    return keySlice;
  }

  /**
   * Returns the number of significant bits in the key slice.
   */
  public int getNumSignificantBits() {
    return numSignificantBits;
  }

  public byte[] getRawDbKey() {
    return rawDbKey;
  }

  /**
   * Returns a key as a bit set.
   */
  public KeyBitSet keyBits() {
    return new KeyBitSet(keySlice, numSignificantBits);
  }

  /**
   * Returns new branch DbKey (unless common prefix of two equals DbKeys is requested, in which
   * case this DbKey itself is returned), which is a common prefix of this and another DbKey.
   */
  public DbKey commonPrefix(DbKey other) {
    if (other.equals(this)) {
      return this;
    }
    BitSet thisBits = this.keyBits().getKeyBits();
    thisBits.xor(other.keyBits().getKeyBits());
    int firstSetBitIndex = thisBits.nextSetBit(0);

    int minPrefixSize = Math.min(this.numSignificantBits, other.numSignificantBits);

    // firstSetBitIndex equals -1 when either both keys are equal or one is a prefix of another with
    // trailing zeros in their prefixes
    if (firstSetBitIndex == -1) {
      return newBranchKey(this.keySlice, minPrefixSize);
    }
    int commonPrefixSize = Math.min(firstSetBitIndex, minPrefixSize);
    byte[] resultingByteArray = this.keyBits().getKeyBits().get(0, firstSetBitIndex).toByteArray();
    byte[] newArray = new byte[DbKey.KEY_SIZE];
    System.arraycopy(resultingByteArray, 0, newArray, 0, resultingByteArray.length);
    return newBranchKey(newArray, commonPrefixSize);
  }

  /**
   * Returns true if this {@code DbKey} is a prefix of that {@code DbKey}.
   */
  public boolean isPrefixOf(DbKey other) {
    return this.keyBits().isPrefixOf(other.keyBits());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DbKey dbKey = (DbKey) o;
    boolean fullRawKeysEqual = Arrays.equals(rawDbKey, dbKey.rawDbKey);
    if (fullRawKeysEqual) {
      assert numSignificantBits == dbKey.numSignificantBits
          && Arrays.equals(keySlice, dbKey.keySlice)
          && nodeType == dbKey.nodeType;
    }
    return fullRawKeysEqual;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(rawDbKey, nodeType, keySlice, numSignificantBits);
  }

  /**
   * The following algorithm is used for comparison:
   * Try to find a first bit index at which this key is greater than the other key (i.e., a bit of
   * this key is 1 and the corresponding bit of the other key is 0), and vice versa. The smaller of
   * these indexes indicates the greater key.
   * If there is no such bit, then lengths of these keys are compared and the key with greater
   * length is considered a greater key.
   */
  @Override
  public int compareTo(DbKey other) {
    if (other.equals(this)) {
      return 0;
    }
    int commonPartSize = Math.min(this.numSignificantBits, other.numSignificantBits);
    BitSet thisBits = this.keyBits().getKeyBits();
    BitSet otherBits = other.keyBits().getKeyBits();
    for (int i = 0; i < commonPartSize; i++) {
      if (thisBits.get(i) ^ otherBits.get(i)) {
        if (thisBits.get(i)) {
          return 1;
        } else {
          return -1;
        }
      }
    }
    return Integer.compare(this.numSignificantBits, other.numSignificantBits);
  }
}
