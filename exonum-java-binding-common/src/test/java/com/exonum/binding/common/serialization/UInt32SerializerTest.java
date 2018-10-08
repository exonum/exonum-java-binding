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

import static com.exonum.binding.common.serialization.StandardSerializersTest.invalidBytesValueTest;
import static com.exonum.binding.common.serialization.StandardSerializersTest.roundTripTest;

import com.exonum.binding.test.Bytes;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class UInt32SerializerTest {

  private Serializer<Integer> serializer = UInt32Serializer.INSTANCE;

  @ParameterizedTest
  @MethodSource("values")
  void roundTrip(Integer value) {
    roundTripTest(value, serializer);
  }

  @ParameterizedTest
  @MethodSource("invalidVarInts")
  void deserializeInvalidValue(byte[] value) {
    invalidBytesValueTest(value, serializer);
  }

  private static IntStream values() {
    return IntStream.range(0, 32)
        .map(value -> 1 << value)
        .flatMap(value -> IntStream.of(-value, value - 1, value, value + 1));
  }

  private static List<byte[]> invalidVarInts() {
    return ImmutableList.of(
        Bytes.bytes(),
        // # MSB is set in the last byte, but there is the end of serialized value:
        Bytes.bytes(0x80),
        Bytes.bytes(0x8F),
        Bytes.bytes(0xFF),
        Bytes.bytes(0x80, 0x81),
        Bytes.bytes(0x80, 0x81, 0x82),
        Bytes.bytes(0x80, 0x81, 0x82, 0x83),
        Bytes.bytes(0x80, 0x81, 0x82, 0x83, 0x84),
        Bytes.bytes(0x80, 0x81, 0x82, 0x83, 0x84, 0x85),

        // # Correct first bytes, but unexpected "tail":
        Bytes.bytes(0x01, 0x02), // A single byte varint + tail.
        Bytes.bytes(0x01, 0x82), // A single byte varint + tail.
        Bytes.bytes(0x01, 0x02, 0x03, 0x04, 0x85), // A single byte varint + 4-byte tail.
        Bytes.bytes(0x80, 0x81, 0x02, 0x83, 0x84), // A valid 3 byte varint + 2-byte tail
        Bytes.bytes(0x80, 0x81, 0x82, 0x03, 0x84), // A valid 4 byte varint, 5th byte is invalid
        Bytes.bytes(0x80, 0x81, 0x82, 0x83, 0x04, 0x01), // Valid 5 byte varint, 6th byte is invalid
        // Exceeding the maximum length (valid 6 & 10-byte varints)
        Bytes.bytes(0x81, 0x82, 0x83, 0x84, 0x85, 0x06),
        Bytes.bytes(0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89, 0x01)
    );
  }

}
