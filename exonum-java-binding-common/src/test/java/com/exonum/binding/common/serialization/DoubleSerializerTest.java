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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DoubleSerializerTest {

  @ParameterizedTest
  @ValueSource(doubles = {Double.MIN_VALUE, -1D, 0D, 1.5D, Double.MAX_VALUE,
      Double.MIN_NORMAL, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
      Double.MIN_EXPONENT, Double.MAX_EXPONENT})
  void roundTrip(Double value) {
    roundTripTest(value, DoubleSerializer.INSTANCE);
  }

  @Test
  void deserializeInvalidValue() {
    byte[] invalidValue = "invalid".getBytes();
    invalidBytesValueTest(invalidValue, DoubleSerializer.INSTANCE);
  }

}
