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

import static com.exonum.binding.common.message.TransactionMessage.AUTHOR_PUBLIC_KEY_SIZE;
import static com.exonum.binding.common.message.TransactionMessage.MIN_MESSAGE_SIZE;
import static com.exonum.binding.common.message.TransactionMessage.SIGNATURE_SIZE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.exonum.binding.common.crypto.CryptoFunction;
import com.exonum.binding.common.crypto.CryptoFunctions;
import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.test.Bytes;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

class TransactionMessageBuilderTest {

  @Test
  void messageBuilderTest() {
    short serviceId = 1;
    short transactionId = 2;
    byte[] payload = Bytes.randomBytes(100);
    byte[] publicKey = Bytes.randomBytes(AUTHOR_PUBLIC_KEY_SIZE);
    KeyPair keys = KeyPair.createKeyPair(Bytes.bytes(0x00), publicKey);
    byte[] signature = Bytes.randomBytes(SIGNATURE_SIZE);
    CryptoFunction cryptoFunction = Mockito.mock(CryptoFunction.class);
    when(cryptoFunction.signMessage(any(), eq(keys.getPrivateKey()))).thenReturn(signature);

    TransactionMessage message = TransactionMessage.builder()
        .serviceId(serviceId)
        .transactionId(transactionId)
        .payload(payload)
        .sign(keys, cryptoFunction);

    assertThat(message.getServiceId(), is(serviceId));
    assertThat(message.getTransactionId(), is(transactionId));
    assertThat(message.getPayload(), is(payload));
    assertThat(message.getAuthor(), is(keys.getPublicKey()));
    assertThat(message.getSignature(), is(signature));
    int expectedSize = MIN_MESSAGE_SIZE + payload.length;
    assertThat(message.size(), is(expectedSize));
  }

  @Test
  void invalidKeyLengthTest() {
    KeyPair keys = KeyPair
        .createKeyPair(Bytes.bytes(0x00, 0x01), Bytes.bytes(0x00, 0x01, 0x02, 0x03));

    assertThrows(IllegalArgumentException.class,
        () -> TransactionMessage.builder()
            .serviceId((short) 0)
            .transactionId((short) 1)
            .payload(Bytes.bytes())
            .sign(keys, CryptoFunctions.ed25519())
    );
  }

  @ParameterizedTest
  @MethodSource("notProperlyFilledMessagesSource")
  void noProperlyFilledMessagesTest(Executable message) {
    assertThrows(IllegalStateException.class, message);
  }

  private static List<Executable> notProperlyFilledMessagesSource() {
    short serviceId = 1;
    short transactionId = 1;
    byte[] payload = Bytes.bytes();
    CryptoFunction crypto = CryptoFunctions.ed25519();
    KeyPair keyPair = crypto.generateKeyPair();

    return ImmutableList.of(
        () -> TransactionMessage.builder()
            .serviceId(serviceId)
            .sign(keyPair, crypto),
        () -> TransactionMessage.builder()
            .serviceId(serviceId)
            .transactionId(transactionId)
            .sign(keyPair, crypto),
        () -> TransactionMessage.builder()
            .transactionId(transactionId)
            .sign(keyPair, crypto),
        () -> TransactionMessage.builder()
            .serviceId(serviceId)
            .payload(payload)
            .sign(keyPair, crypto),
        () -> TransactionMessage.builder()
            .transactionId(transactionId)
            .payload(payload)
            .sign(keyPair, crypto),
        () -> TransactionMessage.builder()
            .payload(payload)
            .sign(keyPair, crypto)
    );
  }

}
