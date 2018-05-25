package com.exonum.binding.crypto;

import static org.abstractj.kalium.encoders.Encoder.HEX;

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
  public String toString() {
    return HEX.encode(rawKey);
  }
}
