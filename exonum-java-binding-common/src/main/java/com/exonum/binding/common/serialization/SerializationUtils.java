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
import static java.util.Arrays.copyOfRange;

import java.util.Arrays;

final class SerializationUtils {

  /**
   * Performs check that bytes array has correct length.
   *
   * @param array bytes array
   * @param length expected length
   * @throws IllegalArgumentException thrown if length is incorrect
   */
  static void checkLength(byte[] array, int length) {
    checkArgument(array.length == length,
        "Expected an array of size %s, but was %s", length, array.length);
  }

  /**
   * Performs check that the array has no bytes after the position.
   *
   * @throws IllegalArgumentException thrown if any bytes in the tail exist
   */
  static void checkNoTailLeft(byte[] array, int pos) {
    checkArgument(pos == array.length, "Expected no tail left in the array, but was %s",
        Arrays.toString(copyOfRange(array, pos, array.length)));
  }

  private SerializationUtils() {
  }
}
