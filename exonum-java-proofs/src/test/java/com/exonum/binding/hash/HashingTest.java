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

package com.exonum.binding.hash;

import static com.exonum.binding.test.Bytes.bytes;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.primitives.UnsignedBytes;
import java.nio.ByteBuffer;
import org.junit.Test;

public class HashingTest {

  private static final String ZERO_HASH_HEX =
      "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

  private static final HashCode ZERO_HASH_CODE = HashCode.fromString(ZERO_HASH_HEX);

  @Test
  public void getHashOfEmptyArray() throws Exception {
    HashFunction f = Hashing.defaultHashFunction();
    assertThat(f.hashBytes(bytes()), equalTo(ZERO_HASH_CODE));
  }

  @Test
  public void getHashOfEmptyByteBuffer() throws Exception {
    HashFunction f = Hashing.defaultHashFunction();
    assertThat(f.hashBytes(ByteBuffer.allocate(0)), equalTo(ZERO_HASH_CODE));
  }

  @Test
  public void toStringAllHexNumbersLower() throws Exception {
    for (byte b = 0; b <= 0xF; b++) {
      String expected = "0" + UnsignedBytes.toString(b, 16);
      assertThat(Hashing.toHexString(bytes(b)), equalTo(expected));
    }
  }

  @Test
  public void toStringAllHexNumbersUpper() throws Exception {
    for (int i = 1; i <= 0xF; i++) {
      byte b = (byte) (i << 4);
      String expected = UnsignedBytes.toString(b, 16);
      assertThat(Hashing.toHexString(bytes(b)), equalTo(expected));
    }
  }
}
