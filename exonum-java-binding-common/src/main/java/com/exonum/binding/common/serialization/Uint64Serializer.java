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

import static com.exonum.binding.common.serialization.SerializationUtils.checkLength;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.copyOf;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

enum Uint64Serializer implements Serializer<Long> {
  INSTANCE;

  private static final int VARINT64_MAX_BYTES = 10;

  @Override
  public byte[] toBytes(Long value) {
    ByteBuffer buffer = ByteBuffer.allocate(VARINT64_MAX_BYTES).order(ByteOrder.LITTLE_ENDIAN);
    long val = value; // un-box the value
    try {
      while (true) {
        if ((val & ~0x7FL) == 0) {
          buffer.put((byte) val);
          return copyOf(buffer.array(), buffer.position());
        } else {
          buffer.put((byte) (((int) val & 0x7F) | 0x80));
          val >>>= 7;
        }
      }
    } catch (BufferOverflowException e) {
      throw new AssertionError("Couldn't serialize value " + val, e);
    }
  }

  @Override
  public Long fromBytes(byte[] serializedValue) {
    checkArgument(serializedValue.length > 0, "Expected not empty array");
    checkArgument(serializedValue.length <= VARINT64_MAX_BYTES,
        "Expected an array of size less than %s, but was %s",
        VARINT64_MAX_BYTES, serializedValue.length);

    try {
      int pos = 0;

      long x;
      int y;
      if ((y = serializedValue[pos++]) >= 0) {
        checkLength(serializedValue, pos);
        return (long) y;
      } else if ((y ^= (serializedValue[pos++] << 7)) < 0) {
        x = y ^ (~0 << 7);
      } else if ((y ^= (serializedValue[pos++] << 14)) >= 0) {
        x = y ^ ((~0 << 7) ^ (~0 << 14));
      } else if ((y ^= (serializedValue[pos++] << 21)) < 0) {
        x = y ^ ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21));
      } else if ((x = y ^ ((long) serializedValue[pos++] << 28)) >= 0L) {
        x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28);
      } else if ((x ^= ((long) serializedValue[pos++] << 35)) < 0L) {
        x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35);
      } else if ((x ^= ((long) serializedValue[pos++] << 42)) >= 0L) {
        x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42);
      } else if ((x ^= ((long) serializedValue[pos++] << 49)) < 0L) {
        x ^= (~0L << 7)
            ^ (~0L << 14)
            ^ (~0L << 21)
            ^ (~0L << 28)
            ^ (~0L << 35)
            ^ (~0L << 42)
            ^ (~0L << 49);
      } else {
        x ^= ((long) serializedValue[pos++] << 56);
        x ^= (~0L << 7)
            ^ (~0L << 14)
            ^ (~0L << 21)
            ^ (~0L << 28)
            ^ (~0L << 35)
            ^ (~0L << 42)
            ^ (~0L << 49)
            ^ (~0L << 56);
        if (x < 0L && serializedValue[pos++] < 0L) {
          throw new IllegalArgumentException(
              "Serialized value has wrong format " + Arrays.toString(serializedValue));
        }
      }
      checkLength(serializedValue, pos);
      return x;
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException(
          "Serialized value has wrong format " + Arrays.toString(serializedValue), e);
    }
  }

}
