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

class StringSerializerTest {

  private Serializer<String> serializer = StringSerializer.INSTANCE;

  @ParameterizedTest
  @ValueSource(strings = {
      "",
      "a",
      "δ", // A two-byte character
      "\uD83E\uDD37", // A four-byte character: a shrug emoji
      "ab",
      "cat",
      "Iñtërnâtiônàlizætiøn"})
  void roundTrip(String value) {
    roundTripTest(value, serializer);
  }

  @Test
  void deserializeInvalidValue() {
    byte[] invalidValue = {-1};
    invalidBytesValueTest(invalidValue, serializer);
  }

}
