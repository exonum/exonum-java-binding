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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Tests for the MessageDigestHashFunction.
 *
 * @author Kurt Alfred Kluever
 */
public class MessageDigestHashFunctionTest {
  private static final ImmutableSet<String> INPUTS = ImmutableSet.of("", "Z", "foobar");

  // From "How Provider Implementations Are Requested and Supplied" from
  // http://docs.oracle.com/javase/6/docs/technotes/guides/security/crypto/CryptoSpec.html
  //  - Some providers may choose to also include alias names.
  //  - For example, the "SHA-256" algorithm might be referred to as "SHA256".
  //  - The algorithm name is not case-sensitive.
  private static final ImmutableMap<String, HashFunction> ALGORITHMS =
      new ImmutableMap.Builder<String, HashFunction>()
          .put("SHA-256", Hashing.sha256())
          .put("SHA-384", Hashing.sha384())
          .put("SHA-512", Hashing.sha512())
          .build();

  @Test
  void testHashing() {
    for (String stringToTest : INPUTS) {
      for (String algorithmToTest : ALGORITHMS.keySet()) {
        assertMessageDigestHashing(HashTestUtils.ascii(stringToTest), algorithmToTest);
      }
    }
  }

  @Test
  void testPutAfterHash() {
    Hasher sha256 = Hashing.sha256().newHasher();

    assertEquals(
        "d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592",
        sha256.putString("The quick brown fox jumps over the lazy dog", Charsets.UTF_8)
            .hash()
            .toString());
    assertThrows(IllegalStateException.class, () -> sha256.putInt(42));
  }

  @Test
  void testHashTwice() {
    Hasher sha256 = Hashing.sha256().newHasher();

    assertEquals(
        "d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592",
        sha256.putString("The quick brown fox jumps over the lazy dog", Charsets.UTF_8)
            .hash()
            .toString());
    assertThrows(IllegalStateException.class, () -> sha256.hash());
  }

  @Test
  void testToString() {
    assertEquals("Hashing.sha256()", Hashing.sha256().toString());
    assertEquals("Hashing.sha512()", Hashing.sha512().toString());
  }

  private static void assertMessageDigestHashing(byte[] input, String algorithmName) {
    try {
      MessageDigest digest = MessageDigest.getInstance(algorithmName);
      assertEquals(
          HashCode.fromBytes(digest.digest(input)), ALGORITHMS.get(algorithmName).hashBytes(input));
      for (int bytes = 4; bytes <= digest.getDigestLength(); bytes++) {
        assertEquals(
            HashCode.fromBytes(Arrays.copyOf(digest.digest(input), bytes)),
            new MessageDigestHashFunction(algorithmName, bytes, algorithmName).hashBytes(input));
      }
      assertThrows(IllegalArgumentException.class, () -> {
        int maxSize = digest.getDigestLength();
        new MessageDigestHashFunction(algorithmName, maxSize + 1, algorithmName);
      });
    } catch (NoSuchAlgorithmException nsae) {
      throw new AssertionError(nsae);
    }
  }
}
