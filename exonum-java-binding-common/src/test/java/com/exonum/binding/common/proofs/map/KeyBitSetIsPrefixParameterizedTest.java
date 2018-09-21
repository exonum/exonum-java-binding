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

package com.exonum.binding.common.proofs.map;

import static com.exonum.binding.test.Bytes.bytes;
import static com.exonum.binding.test.TestParameters.parameters;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
@Disabled
public class KeyBitSetIsPrefixParameterizedTest {

  @Parameter(0)
  public byte[] pathBytes;

  @Parameter(1)
  public int pathLength;

  @Parameter(2)
  public byte[] otherPathBytes;

  @Parameter(3)
  public int otherPathLength;

  @Parameter(4)
  public boolean prefixOf;

  @Parameter(5)
  public String description;

  private KeyBitSet path;

  private KeyBitSet other;

  @BeforeEach
  void setUp() {
    path = new KeyBitSet(pathBytes, pathLength);
    other = new KeyBitSet(otherPathBytes, otherPathLength);
  }

  @Test
  void isPrefixOfOther() {
    assertThat(path.isPrefixOf(other), equalTo(prefixOf));
  }

  @Test
  void isPrefixOfSelf() {
    assertTrue(path.isPrefixOf(path));
  }

  @Test
  void isPrefixOfClone() {
    KeyBitSet clone = new KeyBitSet(pathBytes, pathLength);
    assertTrue(path.isPrefixOf(clone));
  }

  @Parameters(name = "{index} = {5}")
  static Collection<Object[]> testData() {
    return Arrays.asList(
        // "A <- B" reads "A is a prefix of B"
        // "!P" reads "not P"
        parameters(bytes(), 0, bytes(), 0, true, "[] <- []"),
        parameters(bytes(), 0, bytes(), 2, true, "[] <- [00]"),
        parameters(bytes(), 0, bytes(0xE), 4, true, "[] <- [1110]"),

        parameters(bytes(), 2, bytes(), 2, true, "[00] <- [00]"),
        parameters(bytes(), 2, bytes(), 3, true, "[00] <- [000]"),
        parameters(bytes(), 2, bytes(0b100), 3, true, "[00] <- [100]"),
        parameters(bytes(), 2, bytes(0x10), 5, true, "[00] <- [1 0000]"),
        parameters(bytes(), 2, bytes(), 1, false, "![00] <- [0]"),
        parameters(bytes(), 2, bytes(0x1), 1, false, "![00] <- [01]"),
        parameters(bytes(), 2, bytes(0x2), 2, false, "![00] <- [10]"),
        parameters(bytes(), 2, bytes(0x3), 2, false, "![00] <- [11]"),

        parameters(bytes(0x0F), 4, bytes(0x0F), 4, true, "[1111] <- [1111]"),
        parameters(bytes(0x0F), 4, bytes(0x0F), 5, true, "[1111] <- [0 1111]"),
        parameters(bytes(0x0F), 4, bytes(0x1F), 5, true, "[1111] <- [1 1111]"),
        parameters(bytes(0x0F), 4, bytes(0x1F), 8, true, "[1111] <- [0001 1111]"),
        parameters(bytes(0x0F), 4, bytes(0x8F), 8, true, "[1111] <- [1000 1111]"),
        parameters(bytes(0x0F), 4, bytes(0x7), 3, false, "![1111] <- [111]"),
        parameters(bytes(0x0F), 4, bytes(0x7), 4, false, "![1111] <- [0111]"),
        parameters(bytes(0x0F), 4, bytes(0x8), 4, false, "![1111] <- [1000]"),
        parameters(bytes(0x0F), 4, bytes(0x10), 5, false, "![1111] <- [10000]"),

        parameters(bytes(0x0F), 5, bytes(0x0F), 5, true, "[0 1111] <- [0 1111]"),
        parameters(bytes(0x0F), 5, bytes(0x0F), 6, true, "[0 1111] <- [00 1111]"),
        parameters(bytes(0x0F), 5, bytes(0x2F), 6, true, "[0 1111] <- [10 1111]"),
        parameters(bytes(0x0F), 5, bytes(0x8F), 8, true, "[0 1111] <- [1000 1111]"),
        parameters(bytes(0x0F), 5, bytes(0x0F), 4, false, "![0 1111] <- [1111]"),
        parameters(bytes(0x0F), 5, bytes(0x0E), 5, false, "![0 1111] <- [0 1110]"),
        parameters(bytes(0x0F), 5, bytes(0x1F), 5, false, "![0 1111] <- [1 1111]"),
        parameters(bytes(0x0F), 5, bytes(0x0B), 5, false, "![0 1111] <- [0 1011]"),

        parameters(bytes(0x00, 0xFF), 2 * Byte.SIZE, bytes(0x00, 0xFF, 0x11), 3 * Byte.SIZE, true,
            "[FF 00] <- [11 FF 00]"),
        parameters(bytes(0x00, 0xFF), 2 * Byte.SIZE, bytes(0x01, 0xFF, 0x11), 3 * Byte.SIZE, false,
            "![FF 00] <- [11 FF 01]")
    );
  }
}
