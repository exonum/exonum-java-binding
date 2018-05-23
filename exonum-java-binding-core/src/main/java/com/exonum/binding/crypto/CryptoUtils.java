package com.exonum.binding.crypto;

/**
 * Utils for crypto system.
 */
class CryptoUtils {

  /**
   * Check that {@code data} byte array has specified {@code size} and is not {@code null}.
   */
  static boolean checkLength(byte[] data, int size) {
    return data != null && data.length == size;
  }

  /**
   * Check that returned status is a success status.
   */
  static boolean checkReturnValueSuccess(int status) {
    return status == 0;
  }
}
