package com.exonum.binding.crypto;

/**
 * Utils for crypto system.
 */
class CryptoUtils {

  /**
   * Check that {@code data} byte array has specified {@code size}.
   * @throws NullPointerException if it is {@code null}
   */
  static boolean hasLength(byte[] data, int size) {
    return data.length == size;
  }

  /**
   * Check that returned status is a success status.
   */
  static boolean checkReturnValueSuccess(int status) {
    return status == 0;
  }
}
