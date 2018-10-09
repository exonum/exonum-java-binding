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
import static java.util.Arrays.copyOf;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

enum Uint32Serializer implements Serializer<Integer> {
  INSTANCE;

  private static final int VARINT32_MAX_BYTES = 5;

  @Override
  public byte[] toBytes(Integer value) {
    ByteBuffer buffer = ByteBuffer.allocate(VARINT32_MAX_BYTES).order(ByteOrder.LITTLE_ENDIAN);
    int val = value; // un-box the value
    try {
      while (true) {
        if ((val & ~0x7F) == 0) {
          buffer.put((byte) val);
          return copyOf(buffer.array(), buffer.position());
        } else {
          buffer.put((byte) ((val & 0x7F) | 0x80));
          val >>>= 7;
        }
      }
    } catch (BufferOverflowException e) {
      throw new AssertionError("Couldn't serialize value " + val, e);
    }
  }

  @Override
  public Integer fromBytes(byte[] serializedValue) {
    checkArgument(serializedValue.length > 0, "Expected not empty array");
    checkArgument(serializedValue.length <= VARINT32_MAX_BYTES,
        "Expected an array of size less than %s, but was %s",
        VARINT32_MAX_BYTES, serializedValue.length);

    fastpath:
    {
      int pos = 0;

      int x;
      if ((x = serializedValue[pos++]) >= 0) {
        return x;
      } else if (pos > 1) {
        break fastpath;
      } else if ((x ^= (serializedValue[pos++] << 7)) < 0) {
        x ^= (~0 << 7);
      } else if ((x ^= (serializedValue[pos++] << 14)) >= 0) {
        x ^= (~0 << 7) ^ (~0 << 14);
      } else if ((x ^= (serializedValue[pos++] << 21)) < 0) {
        x ^= (~0 << 7) ^ (~0 << 14) ^ (~0 << 21);
      } else {
        int y = serializedValue[pos++];
        x ^= y << 28;
        x ^= (~0 << 7) ^ (~0 << 14) ^ (~0 << 21) ^ (~0 << 28);
        if (y < 0
            && serializedValue[pos++] < 0
            && serializedValue[pos++] < 0
            && serializedValue[pos++] < 0
            && serializedValue[pos++] < 0
            && serializedValue[pos++] < 0) {
          break fastpath; // Will throw malformedVarint()
        }
      }
      return x;
    }
    throw new AssertionError("Malformed value: " + Arrays.toString(serializedValue));
  }

}
