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
