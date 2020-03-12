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

import static com.exonum.binding.common.crypto.CryptoFunctions.Ed25519.PUBLIC_KEY_BYTES;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.exonum.binding.common.crypto.CryptoFunction;
import com.exonum.binding.common.crypto.CryptoFunctions;
import com.exonum.binding.common.crypto.CryptoFunctions.Ed25519;
import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.message.TransactionMessage.Builder;
import com.exonum.binding.test.Bytes;
import com.google.common.collect.ImmutableList;
import java.nio.ByteBuffer;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class TransactionMessageBuilderTest {

  private static final int SERVICE_ID = 1;
  private static final int TRANSACTION_ID = 2;
  private static final CryptoFunction CRYPTO = CryptoFunctions.ed25519();

  @Test
  void messageBuilderTest() {
    byte[] payload = Bytes.randomBytes(100);
    byte[] publicKey = Bytes.randomBytes(PUBLIC_KEY_BYTES);
    KeyPair keys = KeyPair.newInstance(Bytes.bytes(0x00), publicKey);
    byte[] signature = Bytes.randomBytes(Ed25519.SIGNATURE_BYTES);
    CryptoFunction cryptoFunction = mock(CryptoFunction.class);
    when(cryptoFunction.signMessage(any(), eq(keys.getPrivateKey()))).thenReturn(signature);

    TransactionMessage message = TransactionMessage.builder()
        .serviceId(SERVICE_ID)
        .transactionId(TRANSACTION_ID)
        .payload(payload)
        .signedWith(keys, cryptoFunction)
        .build();

    // Check the message has correct attributes
    assertThat(message.getServiceId(), is(SERVICE_ID));
    assertThat(message.getTransactionId(), is(TRANSACTION_ID));
    assertThat(message.getPayload().toByteArray(), is(payload));
    assertThat(message.getAuthor(), is(keys.getPublicKey()));
    assertThat(message.getSignature(), is(signature));
  }

  @Test
  void invalidKeyLengthTest() {
    Builder messageBuilder = TransactionMessage.builder()
        .serviceId(SERVICE_ID)
        .transactionId(TRANSACTION_ID)
        .payload(Bytes.bytes());

    byte[] publicKeyBytes = Bytes.bytes(0x00, 0x01, 0x02, 0x03);
    KeyPair keys = KeyPair.newInstance(Bytes.bytes(0x00, 0x01), publicKeyBytes);

    Exception e = assertThrows(IllegalArgumentException.class,
        () -> messageBuilder.sign(keys));

    assertThat(e.getMessage(), allOf(
        containsString(String.valueOf(publicKeyBytes.length)),
        containsString(String.valueOf(PUBLIC_KEY_BYTES)),
        containsString(String.valueOf(keys.getPublicKey()))));
  }

  @Test
  void payloadFromByteBufferWithCustomPositionAndLimitTest() {
    int position = 3;
    int capacity = 10;
    byte[] payload = Bytes.bytes(0x00, 0x01);
    ByteBuffer payloadBuffer = ByteBuffer.allocate(capacity);
    payloadBuffer.position(position);
    payloadBuffer.limit(position + payload.length);
    payloadBuffer.put(payload);
    payloadBuffer.position(position);

    TransactionMessage message = TransactionMessage.builder()
        .serviceId(SERVICE_ID)
        .transactionId(TRANSACTION_ID)
        .payload(payloadBuffer)
        .sign(CRYPTO.generateKeyPair());

    assertThat(message.getPayload().toByteArray(), is(payload));
  }

  @ParameterizedTest
  @MethodSource("notProperlyFilledMessagesSource")
  void notProperlyFilledMessagesTest(Executable message) {
    assertThrows(IllegalStateException.class, message);
  }

  private static List<Executable> notProperlyFilledMessagesSource() {
    byte[] payload = Bytes.bytes();
    KeyPair keyPair = CRYPTO.generateKeyPair();

    return ImmutableList.of(
        () -> TransactionMessage.builder()
            .serviceId(SERVICE_ID)
            .signedWith(keyPair)
            .build(),
        () -> TransactionMessage.builder()
            .serviceId(SERVICE_ID)
            .transactionId(TRANSACTION_ID)
            .signedWith(keyPair)
            .build(),
        () -> TransactionMessage.builder()
            .transactionId(TRANSACTION_ID)
            .signedWith(keyPair)
            .build(),
        () -> TransactionMessage.builder()
            .serviceId(SERVICE_ID)
            .payload(payload)
            .signedWith(keyPair)
            .build(),
        () -> TransactionMessage.builder()
            .transactionId(TRANSACTION_ID)
            .payload(payload)
            .signedWith(keyPair)
            .build(),
        () -> TransactionMessage.builder()
            .payload(payload)
            .signedWith(keyPair)
            .build()
    );
  }

}
