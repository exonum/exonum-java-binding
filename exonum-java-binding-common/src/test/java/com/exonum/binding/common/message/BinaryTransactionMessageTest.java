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

import static com.exonum.binding.common.message.TransactionMessage.MIN_MESSAGE_SIZE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.crypto.CryptoFunction;
import com.exonum.binding.common.crypto.CryptoFunctions;
import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.test.Bytes;
import com.google.common.collect.ImmutableList;
import java.util.List;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class BinaryTransactionMessageTest {

  private static final CryptoFunction CRYPTO = CryptoFunctions.ed25519();
  private static final KeyPair KEYS = CRYPTO.generateKeyPair();

  @Test
  void equalsTest() {
    EqualsVerifier
        .forClass(BinaryTransactionMessage.class)
        .verify();
  }

  @Test
  void immutabilityTest() {
    byte[] mutableIn = Bytes.bytes(0x00, 0x01, 0x02);
    TransactionMessage message = TransactionMessage.builder()
        .serviceId((short) 100)
        .transactionId((short) 200)
        .payload(mutableIn)
        .sign(KEYS, CRYPTO);

    // mutate input parameter
    mutate(mutableIn);
    assertThat(message.getPayload(), not(mutableIn));

    // mutate output parameters
    byte[] mutablePayload = message.getPayload();
    mutate(mutablePayload);
    assertThat(message.getPayload(), not(mutablePayload));

    byte[] mutableAuthor = message.getAuthor().toBytes();
    mutate(mutableAuthor);
    assertThat(message.getAuthor().toBytes(), not(mutableAuthor));

    byte[] mutableSignature = message.getSignature();
    mutate(mutableSignature);
    assertThat(message.getSignature(), not(mutableSignature));

    byte[] mutableHash = message.hash().asBytes();
    mutate(mutableHash);
    assertThat(message.hash().asBytes(), not(mutableHash));

    byte[] mutableMessage = message.toBytes();
    mutate(mutableMessage);
    assertThat(message.toBytes(), not(mutableMessage));
  }

  @Test
  void invalidBytesArrayTest() {
    byte[] messageBytes = Bytes.randomBytes(MIN_MESSAGE_SIZE - 1);

    assertThrows(IllegalArgumentException.class, () -> new BinaryTransactionMessage(messageBytes));
  }

  @ParameterizedTest
  @MethodSource("source")
  void roundTripTest(TransactionMessage message) {
    byte[] bytes = message.toBytes();
    TransactionMessage actualMessage = TransactionMessage.fromBytes(bytes);

    assertThat(actualMessage, is(message));
  }

  private static List<TransactionMessage> source() {
    return ImmutableList.of(
        TransactionMessage.builder()
            .serviceId((short) 0)
            .transactionId((short) 1)
            .payload(Bytes.bytes())
            .sign(KEYS, CRYPTO),
        TransactionMessage.builder()
            .serviceId(Short.MIN_VALUE)
            .transactionId(Short.MAX_VALUE)
            .payload(Bytes.bytes("test content"))
            .sign(KEYS, CRYPTO)
    );
  }

  private static void mutate(byte[] array) {
    for (int i = 0; i < array.length; i++) {
      array[i] = (byte) ~array[i];
    }
  }

}
