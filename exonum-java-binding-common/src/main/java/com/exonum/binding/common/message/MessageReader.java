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

import static com.google.common.base.Preconditions.checkArgument;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A reader of binary Exonum messages.
 *
 * <p>See <a href=https://exonum.com/doc/version/latest/architecture/serialization/#message-serialization>the definition of the message format</a>.
 */
public final class MessageReader implements BinaryMessage {

  /** Assuming zero-length body. */
  static final int MIN_MESSAGE_BUFFER_SIZE = HEADER_SIZE + SIGNATURE_SIZE;

  private final ByteBuffer message;

  /**
   * Creates a MessageReader from the given byte buffer.
   *
   * @param buffer a byte buffer to read. Its position must be zero,
   *               and the limit must be set to the size of the message
   * @return a message reader of this byte buffer
   * @throws IllegalArgumentException if the message has invalid size
   */
  public static MessageReader wrap(ByteBuffer buffer) {
    MessageReader reader = new MessageReader(buffer);

    int bufferSize = buffer.limit();
    checkArgument(MIN_MESSAGE_BUFFER_SIZE <= bufferSize, 
        "The buffer size (%s) is less than the minimal possible message size (%s)",
        bufferSize, MIN_MESSAGE_BUFFER_SIZE);
    // Check the 'payload_length' field of the message matches the actual buffer size.
    int expectedSize = reader.size();
    checkArgument(bufferSize == expectedSize,
        "The size of the buffer (%s) does not match the expected size "
            + "specified in the message header (%s)", bufferSize, expectedSize);
    return reader;
  }

  private MessageReader(ByteBuffer buf) {
    this.message = buf.duplicate()
        .order(ByteOrder.LITTLE_ENDIAN);
  }

  @Override
  public byte getNetworkId() {
    return message.get(NET_ID_OFFSET);
  }

  @Override
  public byte getVersion() {
    return message.get(VERSION_OFFSET);
  }

  @Override
  public short getServiceId() {
    return message.getShort(SERVICE_ID_OFFSET);
  }

  @Override
  public short getMessageType() {
    return message.getShort(MESSAGE_TYPE_OFFSET);
  }

  /**
   * Returns the body of the message as a view in the underlying byte buffer.
   *
   * <p>The returned byte buffer shares the content of the underlying byte buffer of this message.
   * Its position is zero, limit is set to the position after-the-last element of the body.
   * The buffer is direct iff the underlying buffer is direct, and it is read-only iff
   * the underlying buffer is read-only.
   */
  @Override
  public ByteBuffer getBody() {
    message.position(BODY_OFFSET);
    ByteBuffer body = message.slice();
    body.limit(bodySize());
    return body;
  }

  private int bodySize() {
    return size() - HEADER_SIZE - SIGNATURE_SIZE;
  }

  /**
   * {@inheritDoc}
   *
   * <p>The returned byte buffer shares the content of the underlying byte buffer of this message,
   * and is read-only.
   */
  @Override
  public byte[] getSignature() {
    message.position(signatureOffset());
    byte[] signature = new byte[Message.SIGNATURE_SIZE];
    message.get(signature);
    return signature;
  }

  @Override
  public int size() {
    return message.getInt(PAYLOAD_LENGTH_OFFSET);
  }

  /**
   * {@inheritDoc}
   *
   * <p>The returned byte buffer shares the content of the underlying byte buffer of this message,
   * and is read-only.
   */
  @Override
  public ByteBuffer getSignedMessage() {
    message.position(0);
    return message.duplicate();
  }
}
