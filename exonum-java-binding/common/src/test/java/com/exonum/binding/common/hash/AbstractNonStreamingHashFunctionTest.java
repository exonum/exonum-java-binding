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

import com.exonum.binding.common.hash.HashTestUtils.RandomHasherAction;
import com.exonum.binding.test.CiOnly;
import com.google.common.collect.ImmutableList;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Tests for AbstractNonStreamingHashFunction.
 */
@CiOnly // The SUT and tests are imported and rarely, if ever, change (see 4725ab9e)
class AbstractNonStreamingHashFunctionTest {
  /**
   * Constructs two trivial HashFunctions (output := input), one streaming and one non-streaming,
   * and checks that their results are identical, no matter which newHasher version we used.
   */
  @Test
  void testExhaustive() {
    List<Hasher> hashers =
        ImmutableList.of(
            new StreamingVersion().newHasher(),
            new StreamingVersion().newHasher(52),
            new NonStreamingVersion().newHasher(),
            new NonStreamingVersion().newHasher(123));
    Random random = new Random(0);
    for (int i = 0; i < 200; i++) {
      RandomHasherAction.pickAtRandom(random).performAction(random, hashers);
    }
    HashCode[] codes = new HashCode[hashers.size()];
    for (int i = 0; i < hashers.size(); i++) {
      codes[i] = hashers.get(i).hash();
    }
    for (int i = 1; i < codes.length; i++) {
      assertEquals(codes[i - 1], codes[i]);
    }
  }

  @Test
  void testPutStringWithLowSurrogate() {
    // we pad because the dummy hash function we use to test this, merely copies the input into
    // the output, so the input must be at least 32 bits, since the output has to be that long
    assertPutString(new char[]{'p', HashTestUtils.randomLowSurrogate(new Random())});
  }

  @Test
  void testPutStringWithHighSurrogate() {
    // we pad because the dummy hash function we use to test this, merely copies the input into
    // the output, so the input must be at least 32 bits, since the output has to be that long
    assertPutString(new char[]{'p', HashTestUtils.randomHighSurrogate(new Random())});
  }

  @Test
  void testPutStringWithLowHighSurrogate() {
    assertPutString(
        new char[]{
            HashTestUtils.randomLowSurrogate(new Random()),
            HashTestUtils.randomHighSurrogate(new Random())
        });
  }

  @Test
  void testPutStringWithHighLowSurrogate() {
    assertPutString(
        new char[]{
            HashTestUtils.randomHighSurrogate(new Random()),
            HashTestUtils.randomLowSurrogate(new Random())
        });
  }

  private static void assertPutString(char[] chars) {
    Hasher h1 = new NonStreamingVersion().newHasher();
    Hasher h2 = new NonStreamingVersion().newHasher();
    String s = new String(chars);
    // this is the correct implementation of the spec
    for (int i = 0; i < s.length(); i++) {
      h1.putChar(s.charAt(i));
    }
    h2.putUnencodedChars(s);
    assertEquals(h1.hash(), h2.hash());
  }

  static class StreamingVersion extends AbstractHashFunction {
    @Override
    public int bits() {
      return 32;
    }

    @Override
    public Hasher newHasher() {
      return new AbstractStreamingHasher(4, 4) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        @Override
        protected HashCode makeHash() {
          return HashCode.fromBytes(out.toByteArray());
        }

        @Override
        protected void process(ByteBuffer bb) {
          while (bb.hasRemaining()) {
            out.write(bb.get());
          }
        }

        @Override
        protected void processRemaining(ByteBuffer bb) {
          while (bb.hasRemaining()) {
            out.write(bb.get());
          }
        }
      };
    }
  }

  static class NonStreamingVersion extends AbstractNonStreamingHashFunction {
    @Override
    public int bits() {
      return 32;
    }

    @Override
    public HashCode hashBytes(byte[] input, int off, int len) {
      return HashCode.fromBytes(Arrays.copyOfRange(input, off, off + len));
    }
  }
}
