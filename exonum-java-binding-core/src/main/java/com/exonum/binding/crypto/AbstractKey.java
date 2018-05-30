package com.exonum.binding.crypto;

import static org.abstractj.kalium.encoders.Encoder.HEX;

import com.google.common.base.Objects;
import java.util.Arrays;

/**
 * Represent either a private or public key in a digital signature system.
 */
public abstract class AbstractKey {

  private final byte[] rawKey;

  AbstractKey(byte[] rawKey) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AbstractKey that = (AbstractKey) o;
    return Arrays.equals(rawKey, that.rawKey);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode((Object) rawKey);
  }

  @Override
  public String toString() {
    return HEX.encode(rawKey);
  }
}
