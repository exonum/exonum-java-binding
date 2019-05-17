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

import static com.exonum.binding.common.hash.Hashing.sha256;
import static com.exonum.binding.common.message.TransactionMessage.CLS_OFFSET;
import static com.exonum.binding.common.message.TransactionMessage.MIN_MESSAGE_SIZE;
import static com.exonum.binding.common.message.TransactionMessage.TAG_OFFSET;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.crypto.CryptoFunction;
import com.exonum.binding.common.crypto.CryptoFunctions;
import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.test.Bytes;
import com.google.common.collect.ImmutableList;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.stream.Stream;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BinaryTransactionMessageTest {

  private static final CryptoFunction CRYPTO = CryptoFunctions.ed25519();
  private static final KeyPair KEYS = CRYPTO.generateKeyPair();

  @Test
  void equalsTest() {
    EqualsVerifier
        .forClass(BinaryTransactionMessage.class)
        .withIgnoredFields("messageSize")
        .verify();
  }

  @Test
  void immutabilityTest() {
    byte[] mutableInputPayload = Bytes.bytes(0x00, 0x01, 0x02);
    TransactionMessage message = TransactionMessage.builder()
        .serviceId((short) 100)
        .transactionId((short) 200)
        .payload(mutableInputPayload)
        .sign(KEYS, CRYPTO);

    // mutate input parameter
    mutate(mutableInputPayload);
    assertThat(message.getPayload(), not(mutableInputPayload));

    // mutate output parameters
    byte[] mutablePayload = message.getPayload();
    mutate(mutablePayload);
    assertThat(message.getPayload(), not(mutablePayload));

    byte[] mutableSignature = message.getSignature();
    mutate(mutableSignature);
    assertThat(message.getSignature(), not(mutableSignature));

    byte[] mutableMessage = message.toBytes();
    mutate(mutableMessage);
    assertThat(message.toBytes(), not(mutableMessage));
  }

  @Test
  void internalBufferPositionImmutabilityTest() {
    TransactionMessage message1 = TransactionMessage.builder()
        .serviceId((short) 100)
        .transactionId((short) 200)
        .payload(Bytes.bytes(0x00, 0x01, 0x02))
        .sign(KEYS, CRYPTO);
    TransactionMessage message2 = TransactionMessage.builder()
        .serviceId((short) 100)
        .transactionId((short) 200)
        .payload(Bytes.bytes(0x00, 0x01, 0x02))
        .sign(KEYS, CRYPTO);

    assertThat(message1, equalTo(message2));

    message1.getAuthor();
    assertThat(message1, equalTo(message2));
    message1.getPayload();
    assertThat(message1, equalTo(message2));
    message1.getServiceId();
    assertThat(message1, equalTo(message2));
    message1.getSignature();
    assertThat(message1, equalTo(message2));
    message1.getTransactionId();
    assertThat(message1, equalTo(message2));
    message1.hash();
    assertThat(message1, equalTo(message2));
    message1.toBytes();
    assertThat(message1, equalTo(message2));
  }

  @Test
  void invalidMessageSizeTest() {
    int invalidSize = MIN_MESSAGE_SIZE - 1;
    byte[] messageBytes = Bytes.randomBytes(invalidSize);

    Exception e = assertThrows(IllegalArgumentException.class,
        () -> new BinaryTransactionMessage(messageBytes));
    assertThat(e.getMessage(), containsString(Integer.toString(invalidSize)));
  }

  @Test
  void invalidClassTest() {
    byte[] messageBytes = TransactionMessage.builder()
        .serviceId((short) 1)
        .transactionId((short) 2)
        .payload(Bytes.bytes(0x00, 0x01, 0x02))
        .sign(KEYS, CRYPTO)
        .toBytes();

    // Modify the 'class' byte
    byte invalidClass = (byte) (MessageType.TRANSACTION.cls() + 1);
    messageBytes[CLS_OFFSET] = invalidClass;

    Exception e = assertThrows(IllegalArgumentException.class,
        () -> new BinaryTransactionMessage(messageBytes));
    assertThat(e.getMessage(), containsString("Invalid message class: " + invalidClass));
  }

  @Test
  void invalidTagTest() {
    byte[] messageBytes = TransactionMessage.builder()
        .serviceId((short) 1)
        .transactionId((short) 2)
        .payload(Bytes.bytes(0x00, 0x01, 0x02))
        .sign(KEYS, CRYPTO)
        .toBytes();

    // Modify the 'class' byte
    byte invalidTag = (byte) (MessageType.TRANSACTION.tag() + 1);
    messageBytes[TAG_OFFSET] = invalidTag;

    Exception e = assertThrows(IllegalArgumentException.class,
        () -> new BinaryTransactionMessage(messageBytes));
    assertThat(e.getMessage(), containsString("Invalid message tag: " + invalidTag));
  }

  @ParameterizedTest
  @MethodSource("transactionMessageSource")
  void roundTripTest(TransactionMessage message) {
    byte[] bytes = message.toBytes();
    TransactionMessage actualMessage = TransactionMessage.fromBytes(bytes);

    assertThat(actualMessage, is(message));
  }

  @ParameterizedTest
  @MethodSource("transactionMessageSource")
  void hashTest(TransactionMessage message) {
    HashCode hash = sha256().hashBytes(message.toBytes());

    assertThat(message.hash(), is(hash));

    // check that hash doesn't depend on internal state of the message
    // By changing the position within the internal BB
    message.getPayload();
    assertThat(message.hash(), is(hash));
    message.getAuthor();
    assertThat(message.hash(), is(hash));
    message.getSignature();
    assertThat(message.hash(), is(hash));
  }

  @ParameterizedTest
  @MethodSource("byteBufferSource")
  void constructFromBufferTest(TransactionMessage expectedMessage, ByteBuffer buffer) {
    assertThat(new BinaryTransactionMessage(buffer), equalTo(expectedMessage));
  }

  private static List<TransactionMessage> transactionMessageSource() {
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

  private static Stream<Arguments> byteBufferSource() {
    return transactionMessageSource().stream()
        .flatMap(message ->
            Stream.of(
                toLittleEndianBuffer(message),
                toReadOnlyBuffer(message),
                toDirectBuffer(message),
                toByteBufferWithCustomPositions(message)
            )
                .map(buffer -> Arguments.of(message, buffer))
        );
  }

  private static ByteBuffer toLittleEndianBuffer(TransactionMessage message) {
    return ByteBuffer
        .wrap(message.toBytes())
        .order(ByteOrder.LITTLE_ENDIAN);
  }

  private static ByteBuffer toReadOnlyBuffer(TransactionMessage message) {
    return ByteBuffer
        .wrap(message.toBytes())
        .asReadOnlyBuffer();
  }

  private static ByteBuffer toDirectBuffer(TransactionMessage message) {
    byte[] bytes = message.toBytes();
    ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
    buffer.put(bytes);
    buffer.position(0);
    return buffer;
  }

  private static ByteBuffer toByteBufferWithCustomPositions(TransactionMessage message) {
    byte[] bytes = message.toBytes();
    int position = bytes.length;
    int limit = bytes.length * 2;
    int capacity = bytes.length * 3;
    ByteBuffer buffer = ByteBuffer.allocate(capacity);
    buffer.position(position);
    buffer.limit(limit);
    buffer.put(bytes);
    buffer.position(position);
    return buffer;
  }

  private static void mutate(byte[] array) {
    for (int i = 0; i < array.length; i++) {
      array[i] = (byte) ~array[i];
    }
  }

}
