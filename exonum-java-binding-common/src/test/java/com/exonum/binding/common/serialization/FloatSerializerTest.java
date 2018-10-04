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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class FloatSerializerTest {

  private Serializer<Float> serializer = FloatSerializer.INSTANCE;

  @ParameterizedTest
  @ValueSource(floats = {
      Float.NaN,
      Float.NEGATIVE_INFINITY,
      Float.MIN_NORMAL,
      Float.MIN_VALUE,
      -1F, 0F, 1.5F,
      Float.MAX_VALUE,
      Float.POSITIVE_INFINITY})
  void roundTrip(Float value) {
    roundTripTest(value, serializer);
  }

  @ParameterizedTest
  @MethodSource("invalidFloats")
  void deserializeInvalidValue(byte[] value) {
    invalidBytesValueTest(value, serializer);
  }

  private static List<byte[]> invalidFloats() {
    return ImmutableList.of(
        Bytes.bytes(),
        Bytes.bytes((byte) 0),
        Bytes.bytes(1, 2, 3),
        Bytes.bytes(1, 2, 3, 4, 5),
        Bytes.bytes(1, 2, 3, 4, 5, 6, 7, 8)
    );
  }
}
