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
import java.util.stream.LongStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class Uint64SerializerTest {

  private Serializer<Long> serializer = Uint64Serializer.INSTANCE;

  @ParameterizedTest
  @MethodSource("values")
  void roundTrip(Long value) {
    roundTripTest(value, serializer);
  }

  @ParameterizedTest
  @MethodSource("invalidValues")
  void deserializeInvalidValue(byte[] value) {
    invalidBytesValueTest(value, serializer);
  }

  private static LongStream values() {
    return LongStream.range(0, 64)
        .map(value -> 1L << value)
        .flatMap(value -> LongStream.of(-value, value - 1, value, value + 1));
  }

  private static List<byte[]> invalidValues() {
    return ImmutableList.of(
        Bytes.bytes(),
        Bytes.bytes(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
    );
  }

}
