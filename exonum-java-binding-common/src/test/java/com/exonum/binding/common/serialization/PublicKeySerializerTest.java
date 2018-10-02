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

import static com.exonum.binding.common.crypto.CryptoFunctions.ed25519;
import static com.exonum.binding.common.serialization.StandardSerializersTest.invalidBytesValueTest;
import static com.exonum.binding.common.serialization.StandardSerializersTest.roundTripTest;

import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.crypto.PublicKey;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class PublicKeySerializerTest {

  @ParameterizedTest
  @MethodSource("testSource")
  void roundTrip(PublicKey key) {
    roundTripTest(key, PublicKeySerializer.INSTANCE);
  }

  @Test
  void deserializeInvalidValue() {
    byte[] invalidValue = {};
    invalidBytesValueTest(invalidValue, PublicKeySerializer.INSTANCE);
  }

  private static Stream<PublicKey> testSource() {
    return Stream.of(
        ed25519().generateKeyPair(),
        ed25519().generateKeyPair(),
        ed25519().generateKeyPair()
    ).map(KeyPair::getPublicKey);
  }

}
