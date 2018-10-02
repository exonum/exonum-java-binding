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

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.Hashing;
import com.google.common.collect.Streams;
import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class HashCodeSerializerTest {

  @ParameterizedTest
  @MethodSource("testHashes")
  void roundTrip(HashCode hashCode) {
    roundTripTest(hashCode, HashCodeSerializer.INSTANCE);
  }

  @Test
  void deserializeInvalidValue() {
    byte[] invalidValue = {};
    invalidBytesValueTest(invalidValue, HashCodeSerializer.INSTANCE);
  }

  private static Stream<HashCode> testHashes() {
    // Hash codes of zeros of various length
    Stream<HashCode> zeroHashCodes = IntStream.of(1, 2, 16, 32)
        .mapToObj(byte[]::new)
        .map(HashCode::fromBytes);

    // Non-zero 32-byte SHA-256 hash codes
    Stream<HashCode> sha256HashCodes = Stream.of(
        "",
        "a",
        "hello"
    ).map(s -> Hashing.sha256().hashString(s, StandardCharsets.UTF_8));
    return Streams.concat(zeroHashCodes, sha256HashCodes);
  }

}
