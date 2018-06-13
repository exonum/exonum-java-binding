package com.exonum.binding.crypto;

import static com.google.common.base.Preconditions.checkArgument;
import static org.abstractj.kalium.encoders.Encoder.HEX;

import java.util.Arrays;

/**
 * Represent either a private or public key in a digital signature system.
 */
public abstract class AbstractKey {

  private final byte[] rawKey;

  AbstractKey(byte[] rawKey) {
    checkArgument(rawKey.length > 0, "Key must not be empty");
    this.rawKey = rawKey;
  }

  /**
   * Returns the value of this key as a byte array.
   */
  public byte[] toBytes() {
    return rawKey.clone();
  }

  /**
   * Returns a mutable view of the underlying bytes for the given key.
   */
  byte[] toBytesNoCopy() {
    return rawKey;
  }

  /**
   * Returns the length of this key.
   */
  public int size() {
    return rawKey.length;
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }
    if (this.getClass() == o.getClass()) {
      AbstractKey that = (AbstractKey) o;
      return Arrays.equals(rawKey, that.rawKey);
    }
    return false;
  }

  @Override
  public final int hashCode() {
    return Arrays.hashCode(rawKey);
  }

  @Override
  public String toString() {
    return HEX.encode(rawKey);
  }
}
