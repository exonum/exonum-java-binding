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
 *
 */

package com.exonum.binding.common.serialization;

import static com.google.common.base.Preconditions.checkArgument;

final class SerializationUtils {

  /**
   * Performs check that serialized value has correct length.
   *
   * @param array serialized value in bytes
   * @param length expected length
   * @throws IllegalArgumentException thrown if length is incorrect
   */
  static void checkLength(byte[] array, int length) {
    checkArgument(array.length == length,
        "Expected an array of size %s, but was %s", length, array.length);
  }

  private SerializationUtils() {
  }
}
