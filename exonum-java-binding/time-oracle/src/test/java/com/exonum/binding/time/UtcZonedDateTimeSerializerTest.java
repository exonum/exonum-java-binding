/*
 * Copyright 2019 The Exonum Team
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

package com.exonum.binding.time;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.test.Bytes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

class UtcZonedDateTimeSerializerTest {

  private final Serializer<ZonedDateTime> SERIALIZER = UtcZonedDateTimeSerializer.INSTANCE;

  @ParameterizedTest
  @MethodSource("testSource")
  void roundTrip(ZonedDateTime key) {
    byte[] bytes = SERIALIZER.toBytes(key);
    ZonedDateTime actual = SERIALIZER.fromBytes(bytes);

    assertThat(actual, equalTo(key));
  }

  @Test
  void deserializeInvalidValue() {
    byte[] invalidValue = Bytes.bytes();
    assertThrows(IllegalArgumentException.class, () -> SERIALIZER.fromBytes(invalidValue));
  }

  @Test
  void serializeNonUtcValue() {
    ZonedDateTime value = ZonedDateTime.now(ZoneId.of("Europe/Amsterdam"));
    assertThrows(IllegalArgumentException.class, () -> SERIALIZER.toBytes(value));
  }

  private static Stream<ZonedDateTime> testSource() {
    return Stream.of(
        ZonedDateTime.now(ZoneOffset.UTC),
        ZonedDateTime.of(2000, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC));
  }
}
