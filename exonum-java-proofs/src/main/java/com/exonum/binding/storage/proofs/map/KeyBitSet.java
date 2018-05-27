package com.exonum.binding.storage.proofs.map;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.BitSet;
import java.util.Objects;

/**
 * A key bit set.
 */
public final class KeyBitSet {

  private final BitSet keyBits;

  /**
   * A length of this key in bits, i.e., the number of significant bits.
   *
   * <p>NOT the same as {@code keyBits.length()}, which is the most significant set bit.</p>
   */
  private final int length;

  /**
   * Creates a new bit set.
   *
   * @param key key bytes
   * @param length a length in bits, i.e., the number of significant bits in key array
   */
  public KeyBitSet(byte[] key, int length) {
    checkArgument(length >= 0, "length (%s) must be non-negative", length);
    keyBits = BitSet.valueOf(key);
    this.length = length;
  }

  public int getLength() {
    return length;
  }

  public BitSet getKeyBits() {
    return (BitSet) keyBits.clone();
  }

  /**
   * Checks if this key bit set is equal to the specified object.
   * @param o an object to compare against
   * @return true if this key bit set is equal to the specified object
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    KeyBitSet keyBitSet = (KeyBitSet) o;
    return length == keyBitSet.length
        && Objects.equals(keyBits, keyBitSet.keyBits);
  }

  @Override
  public int hashCode() {
    return Objects.hash(keyBits, length);
  }

  @SuppressWarnings("ReferenceEquality")
  boolean isPrefixOf(KeyBitSet other) {
    if (other == this) {
      return true;
    }
    if (other.length < this.length) {
      return false;
    }
    BitSet thisBits = (BitSet) this.keyBits.clone();
    thisBits.xor(other.keyBits);
    int firstSetBitIndex = thisBits.nextSetBit(0);
    return (firstSetBitIndex >= this.length) || (firstSetBitIndex == -1);
  }

  @Override
  public String toString() {
    return "KeyBitSet{"
        + "keyBits=" + keyBits
        + ", length=" + length
        + '}';
  }
}
