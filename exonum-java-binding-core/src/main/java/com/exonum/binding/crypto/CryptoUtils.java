package com.exonum.binding.crypto;

/**
 * Utils for crypto system.
 */
class CryptoUtils {

  /**
   * Check that {@code data} byte array has specified {@code size}.
   */
  static void checkLength(byte[] data, int size) {
    if (data == null || data.length != size) {
      throw new IllegalArgumentException("Byte array has invalid size");
    }
  }

  /**
   * Check that returned status is a success status.
   */
  static boolean checkReturnValueSuccess(int status) {
    return status == 0;
  }
}
