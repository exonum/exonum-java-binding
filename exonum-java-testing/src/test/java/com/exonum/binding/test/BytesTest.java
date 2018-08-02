/*
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.test;

import static com.exonum.binding.test.Bytes.bytes;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.primitives.UnsignedBytes;
import org.junit.jupiter.api.Test;

class BytesTest {

  @Test
  void fromHex() {
    assertThat(Bytes.fromHex("abcd01"), equalTo(new byte[] {UnsignedBytes.checkedCast(0xAB),
        UnsignedBytes.checkedCast(0xCD), 0x01}));
  }

  @Test
  void toHexStringAllHexNumbersLower() {
    for (byte b = 0; b <= 0xF; b++) {
      String expected = "0" + UnsignedBytes.toString(b, 16);
      assertThat(Bytes.toHexString(bytes(b)), equalTo(expected));
    }
  }

  @Test
  void toHexStringAllHexNumbersUpper() {
    for (int i = 1; i <= 0xF; i++) {
      byte b = (byte) (i << 4);
      String expected = UnsignedBytes.toString(b, 16);
      assertThat(Bytes.toHexString(bytes(b)), equalTo(expected));
    }
  }
}
