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

package com.exonum.binding.common.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.crypto.CryptoFunction;
import com.exonum.binding.common.crypto.CryptoFunctions;
import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.test.Bytes;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class TransactionMessageTest {

  private static final CryptoFunction CRYPTO = CryptoFunctions.ed25519();
  private static final KeyPair KEYS = CRYPTO.generateKeyPair();

  @Test
  void noProperlyFilledMessagesTest() {
    short serviceId = 1;
    short transactionId = 1;
    byte[] payload = Bytes.bytes();

    assertThrows(NullPointerException.class,
        () -> TransactionMessage.builder()
            .serviceId(serviceId)
            .sign(KEYS, CRYPTO));
    assertThrows(NullPointerException.class,
        () -> TransactionMessage.builder()
            .serviceId(serviceId)
            .transactionId(transactionId)
            .sign(KEYS, CRYPTO));
    assertThrows(NullPointerException.class,
        () -> TransactionMessage.builder()
            .transactionId(transactionId)
            .sign(KEYS, CRYPTO));
    assertThrows(NullPointerException.class,
        () -> TransactionMessage.builder()
            .serviceId(serviceId)
            .payload(payload)
            .sign(KEYS, CRYPTO));
    assertThrows(NullPointerException.class,
        () -> TransactionMessage.builder()
            .transactionId(transactionId)
            .payload(payload)
            .sign(KEYS, CRYPTO));
    assertThrows(NullPointerException.class,
        () -> TransactionMessage.builder()
            .payload(payload)
            .sign(KEYS, CRYPTO));
  }


  @ParameterizedTest
  @MethodSource("source")
  void roundTripTest(TransactionMessage message) {
    byte[] bytes = message.toBytes();
    TransactionMessage actualMessage = TransactionMessage.fromBytes(bytes);
    assertThat(actualMessage).isEqualTo(message);
  }

  private static List<TransactionMessage> source() {
    return ImmutableList.of(
        TransactionMessage.builder()
            .serviceId((short) 1)
            .transactionId((short) 1)
            .payload(Bytes.bytes())
            .sign(KEYS, CRYPTO),
        TransactionMessage.builder()
            .serviceId(Short.MIN_VALUE)
            .transactionId(Short.MAX_VALUE)
            .payload(Bytes.bytes())
            .sign(KEYS, CRYPTO)
    );
  }

}
