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
 */

package com.exonum.binding.common.message;

import static com.exonum.binding.common.message.ByteBufferAllocator.allocateBuffer;
import static com.exonum.binding.common.message.Message.BODY_OFFSET;
import static com.exonum.binding.common.message.Message.MESSAGE_TYPE_OFFSET;
import static com.exonum.binding.common.message.Message.NET_ID_OFFSET;
import static com.exonum.binding.common.message.Message.PAYLOAD_LENGTH_OFFSET;
import static com.exonum.binding.common.message.Message.SERVICE_ID_OFFSET;
import static com.exonum.binding.common.message.Message.SIGNATURE_SIZE;
import static com.exonum.binding.common.message.Message.VERSION_OFFSET;
import static com.exonum.binding.common.message.MessageReader.MIN_MESSAGE_BUFFER_SIZE;
import static com.exonum.binding.test.Bytes.bytes;
import static com.exonum.binding.test.Bytes.createPrefixed;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

class MessageReaderTest {


  @Test
  void wrapThrowsIfTooSmall() {
    ByteBuffer buf = allocateBuffer(2);


    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
        () -> MessageReader.wrap(buf));
    assertEquals("The buffer size (2) is less than the minimal possible "
        + "message size (74)", thrown.getMessage());
  }

  @Test
  void wrapThrowsIfTooSmall2() {
    ByteBuffer buf = allocateBuffer(MIN_MESSAGE_BUFFER_SIZE - 1);


    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
        () -> MessageReader.wrap(buf));
    assertEquals("The buffer size (73) is less than the minimal possible "
        + "message size (74)", thrown.getMessage());
  }

  @Test
  void wrapThrowsIfTooSmallWithSetMessageSize() {
    ByteBuffer buf = allocateMessageBuffer(MIN_MESSAGE_BUFFER_SIZE - 1);


    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
        () -> MessageReader.wrap(buf));
    assertEquals("The buffer size (73) is less than the minimal possible "
        + "message size (74)", thrown.getMessage());
  }

  @Test
  void wrapsMinimalMessage() {
    ByteBuffer buf = allocateMessageBuffer(MIN_MESSAGE_BUFFER_SIZE);

    MessageReader m = MessageReader.wrap(buf);

    assertThat(m.getBody().remaining(), equalTo(0));
  }

  @Test
  void wrapsWhenLimitNotEqualCapacity() {
    ByteBuffer buf = allocateBuffer(2 * MIN_MESSAGE_BUFFER_SIZE)
        .putInt(PAYLOAD_LENGTH_OFFSET, MIN_MESSAGE_BUFFER_SIZE);
    buf.limit(MIN_MESSAGE_BUFFER_SIZE);

    MessageReader m = MessageReader.wrap(buf);

    assertThat(m.getBody().remaining(), equalTo(0));
  }

  @Test
  void wrapThrowsIfMessageSizeFieldGreaterThanActual() {
    int bufferSize = MIN_MESSAGE_BUFFER_SIZE;
    ByteBuffer buf = allocateBuffer(bufferSize);
    int messageSize = 2048;
    buf.putInt(PAYLOAD_LENGTH_OFFSET, messageSize);


    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
        () -> MessageReader.wrap(buf));
    assertEquals("The size of the buffer (" + bufferSize
        + ") does not match the expected size specified in the message header (" + messageSize
        + ")", thrown.getMessage());
  }

  @Test
  void wrapThrowsIfMessageSizeFieldLessThanActual() {
    int bufferSize = 2 * MIN_MESSAGE_BUFFER_SIZE;
    ByteBuffer buf = allocateBuffer(bufferSize);
    int messageSize = bufferSize - 1;
    buf.putInt(PAYLOAD_LENGTH_OFFSET, messageSize);


    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
        () -> MessageReader.wrap(buf));
    assertEquals("The size of the buffer (" + bufferSize
        + ") does not match the expected size specified in the message header (" + messageSize
        + ")", thrown.getMessage());
  }

  @Test
  void getNetworkId() {
    byte netId = 0x01;
    ByteBuffer buf = allocateMessageBuffer(MIN_MESSAGE_BUFFER_SIZE)
        .put(NET_ID_OFFSET, netId);

    MessageReader m = MessageReader.wrap(buf);

    assertThat(m.getNetworkId(), equalTo(netId));
  }

  @Test
  void getVersion() {
    byte version = 0x02;
    ByteBuffer buf = allocateMessageBuffer(MIN_MESSAGE_BUFFER_SIZE)
        .put(VERSION_OFFSET, version);

    MessageReader m = MessageReader.wrap(buf);

    assertThat(m.getVersion(), equalTo(version));
  }

  @Test
  void getServiceId() {
    short serviceId = 0x0BCD;
    ByteBuffer buf = allocateMessageBuffer(MIN_MESSAGE_BUFFER_SIZE)
        .putShort(SERVICE_ID_OFFSET, serviceId);

    MessageReader m = MessageReader.wrap(buf);

    assertThat(m.getServiceId(), equalTo(serviceId));
  }

  @Test
  void getMessageType() {
    short messageType = 0x0BCD;
    ByteBuffer buf = allocateMessageBuffer(MIN_MESSAGE_BUFFER_SIZE)
        .putShort(MESSAGE_TYPE_OFFSET, messageType);

    MessageReader m = MessageReader.wrap(buf);

    assertThat(m.getMessageType(), equalTo(messageType));
  }

  @Test
  void getBody_Empty() {
    ByteBuffer buf = allocateMessageBuffer(MIN_MESSAGE_BUFFER_SIZE);
    boolean directBuffer = buf.isDirect();

    MessageReader m = MessageReader.wrap(buf);

    ByteBuffer body = m.getBody();
    assertThat(body.isDirect(), equalTo(directBuffer));
    assertThat(body.remaining(), equalTo(0));
  }

  @Test
  void getBody_4Bytes() {
    int bodySize = Integer.BYTES;
    int bodyValue = 0x12345678;
    ByteBuffer buf = allocateMessageBuffer(MIN_MESSAGE_BUFFER_SIZE + bodySize)
        .putInt(BODY_OFFSET, bodyValue);

    MessageReader m = MessageReader.wrap(buf);

    ByteBuffer body = m.getBody()
        .order(ByteOrder.LITTLE_ENDIAN);
    assertThat(body.remaining(), equalTo(bodySize));
    assertThat(body.getInt(), equalTo(bodyValue));
  }

  @Test
  void getSignature() {
    byte[] signature = createPrefixed(bytes("Signature bytes"), SIGNATURE_SIZE);
    ByteBuffer buf = allocateMessageBuffer(MIN_MESSAGE_BUFFER_SIZE);
    buf.position(MIN_MESSAGE_BUFFER_SIZE - SIGNATURE_SIZE);
    buf.put(signature);
    buf.flip();

    MessageReader m = MessageReader.wrap(buf);

    assertThat(m.getSignature(), equalTo(signature));
  }

  @Test
  void getMessage() {
    ByteBuffer buf = allocateMessageBuffer(MIN_MESSAGE_BUFFER_SIZE)
        .put(NET_ID_OFFSET, (byte) 0x02)
        .put(VERSION_OFFSET, (byte) 0x01)
        .putShort(MESSAGE_TYPE_OFFSET, (short) 0x0ABC);

    MessageReader m = MessageReader.wrap(buf);

    ByteBuffer binaryMessage = m.getSignedMessage();
    assertThat(binaryMessage, equalTo(buf));
  }

  @Test
  void size() {
    int bufferSize = MIN_MESSAGE_BUFFER_SIZE;
    ByteBuffer buf = allocateMessageBuffer(bufferSize);

    MessageReader m = MessageReader.wrap(buf);

    assertThat(m.size(), equalTo(bufferSize));
  }

  /**
   * Allocates a byte buffer of the given size and sets its "payload_length" field.
   */
  private static ByteBuffer allocateMessageBuffer(int bufferSize) {
    return allocateBuffer(bufferSize)
        .putInt(PAYLOAD_LENGTH_OFFSET, bufferSize);
  }
}
