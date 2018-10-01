/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

package com.exonum.binding.common.hash;

import static com.exonum.binding.test.Bytes.bytes;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Hashing}.
 *
 * @author Dimitris Andreou
 * @author Kurt Alfred Kluever
 */
class HashingTest {

  private static final String ZERO_HASH_HEX =
      "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

  private static final HashCode ZERO_HASH_CODE = HashCode.fromString(ZERO_HASH_HEX);

  @Test
  void testGetHashOfEmptyArray() {
    HashFunction f = Hashing.defaultHashFunction();
    assertThat(f.hashBytes(bytes()), equalTo(ZERO_HASH_CODE));
  }

  @Test
  void testGetHashOfEmptyByteBuffer() {
    HashFunction f = Hashing.defaultHashFunction();
    assertThat(f.hashBytes(ByteBuffer.allocate(0)), equalTo(ZERO_HASH_CODE));
  }

  @Test
  void testSha256() {
    HashTestUtils.checkAvalanche(Hashing.sha256(), 100, 0.4);
    HashTestUtils.checkNo2BitCharacteristics(Hashing.sha256());
    HashTestUtils.checkNoFunnels(Hashing.sha256());
    HashTestUtils.assertInvariants(Hashing.sha256());
    assertEquals("Hashing.sha256()", Hashing.sha256().toString());
  }

  @Test
  void testSha384() {
    HashTestUtils.checkAvalanche(Hashing.sha384(), 100, 0.4);
    HashTestUtils.checkNo2BitCharacteristics(Hashing.sha384());
    HashTestUtils.checkNoFunnels(Hashing.sha384());
    HashTestUtils.assertInvariants(Hashing.sha384());
    assertEquals("Hashing.sha384()", Hashing.sha384().toString());
  }

  @Test
  void testSha512() {
    HashTestUtils.checkAvalanche(Hashing.sha512(), 100, 0.4);
    HashTestUtils.checkNo2BitCharacteristics(Hashing.sha512());
    HashTestUtils.checkNoFunnels(Hashing.sha512());
    HashTestUtils.assertInvariants(Hashing.sha512());
    assertEquals("Hashing.sha512()", Hashing.sha512().toString());
  }
}
