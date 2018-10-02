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

import static com.exonum.binding.common.serialization.StandardSerializersTest.roundTripTest;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class NoOpSerializerTest {

  @ParameterizedTest
  @MethodSource("testSource")
  void roundTrip(byte[] value) {
    roundTripTest(value, NoOpSerializer.INSTANCE);
  }

  private static Stream<byte[]> testSource() {
    return Stream.of(
        new byte[]{},
        new byte[]{-1},
        new byte[]{-1, 0},
        new byte[]{-1, 0, 1},
        new byte[]{Byte.MIN_VALUE, Byte.MAX_VALUE}
    );
  }

}
