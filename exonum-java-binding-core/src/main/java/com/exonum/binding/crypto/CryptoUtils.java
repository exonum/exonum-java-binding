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

package com.exonum.binding.crypto;

import com.goterl.lazycode.lazysodium.LazySodium;

/**
 * Utils for crypto system.
 */
class CryptoUtils {

  /**
   * Check that {@code data} byte array has specified {@code size}.
   *
   * @throws NullPointerException if it is {@code null}
   */
  static boolean hasLength(byte[] data, int size) {
    return data.length == size;
  }

  /**
   * Converts hexadecimal to bytes.
   *
   * @param hex hexadecimal string
   * @return bytes array
   */
  static byte[] hexToByteArray(String hex) {
    return LazySodium.toBin(hex);
  }

  /**
   * Converts bytes to hexadecimal.
   *
   * @param bin bytes array
   * @return hexadecimal string
   */
  static String byteArrayToHex(byte[] bin) {
    return LazySodium.toHex(bin);
  }

  /**
   * Merges two byte arrays.
   *
   * @param first  byte array
   * @param second byte array
   * @return combined byte array
   */
  static byte[] merge(byte[] first, byte[] second) {
    byte[] combined = new byte[first.length + second.length];
    System.arraycopy(first, 0, combined, 0, first.length);
    System.arraycopy(second, 0, combined, first.length, second.length);
    return combined;
  }

  private CryptoUtils() {
  }
}
