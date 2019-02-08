/*
 * Copyright 2019 The Exonum Team
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

package com.exonum.binding.common.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CryptoUtilsTest {

  @Test
  void hasLength() {
    byte[] bytes = new byte[10];

    assertTrue(CryptoUtils.hasLength(bytes, 10));
  }

  @Test
  void hexToByteArray() {
    byte[] bytes = CryptoUtils.hexToByteArray("abcd");

    assertEquals(2, bytes.length);
    assertEquals(-85, bytes[0]);
    assertEquals(-51, bytes[1]);
  }

  @Test
  void byteArrayToHex() {
    String hex = CryptoUtils.byteArrayToHex(new byte[]{-85, -51});

    assertEquals("abcd", hex);
  }

}
