package com.exonum.binding.storage.proofs.map;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.UnsignedBytes;
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
public class DbKey {

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

  @VisibleForTesting  // This constructor exists for tests only.
  DbKey(DbKey.Type nodeType, byte[] keySlice, int numSignificantBits) {
    // TODO: consider extracting the checks below
    checkArgument(keySlice.length == KEY_SIZE);
    checkArgument(0 <= numSignificantBits && numSignificantBits <= KEY_SIZE_BITS);
    switch (nodeType) {
      case LEAF:
        checkArgument(numSignificantBits == KEY_SIZE_BITS);
        break;
      case BRANCH:
        checkBranchKeySlice(keySlice, numSignificantBits);
        break;
      default:
        throw new AssertionError("Unreachable");
    }
    this.nodeType = nodeType;
    this.keySlice = keySlice;
    this.numSignificantBits = numSignificantBits;
    this.rawDbKey = new byte[DB_KEY_SIZE];
    rawDbKey[0] = nodeType.code;
    rawDbKey[DB_KEY_SIZE - 1] = (numSignificantBits == KEY_SIZE_BITS) ? 0
        : UnsignedBytes.checkedCast(numSignificantBits);
    System.arraycopy(keySlice, 0, rawDbKey, 1, KEY_SIZE);
  }

  private void checkBranchKeySlice(byte[] keySlice, int numSignificantBits) {
    if (MapProofValidator.PERFORM_TREE_CORRECTNESS_CHECKS) {
      BitSet keyBits = BitSet.valueOf(keySlice);
      checkArgument(keyBits.length() <= numSignificantBits,
          "Branch key slice contains set bits after its numSignificantBits (%s): "
              + "length=%s, keyBits=%s", numSignificantBits, keyBits.length(), keyBits);
    }
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
}
