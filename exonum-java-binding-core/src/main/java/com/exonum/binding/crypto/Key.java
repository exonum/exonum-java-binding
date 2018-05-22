package com.exonum.binding.crypto;

public interface Key {

  /**
   * Returns the value of this key as a byte array.
   */
  byte[] toBytes();

  /**
   * Returns a mutable view of the underlying bytes for the given key.
   */
  byte[] toBytesNoCopy();
}
