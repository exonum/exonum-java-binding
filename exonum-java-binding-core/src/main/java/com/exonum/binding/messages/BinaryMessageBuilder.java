/* 
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.messages;

import static com.exonum.binding.messages.Message.BODY_OFFSET;
import static com.exonum.binding.messages.Message.MESSAGE_TYPE_OFFSET;
import static com.exonum.binding.messages.Message.NET_ID_OFFSET;
import static com.exonum.binding.messages.Message.PAYLOAD_LENGTH_OFFSET;
import static com.exonum.binding.messages.Message.SERVICE_ID_OFFSET;
import static com.exonum.binding.messages.Message.VERSION_OFFSET;
import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.IntFunction;

/**
 * A builder of binary Exonum messages.
 */
public final class BinaryMessageBuilder {

  private static final IntFunction<ByteBuffer> messageBufferAllocator = ByteBuffer::allocate;

  private final Message message;

  /**
   * Creates a binary message from a given message.
   *
   * <p>If the given message is already a binary message, will return it.
   *
   * @param message a message to convert into a binary message
   * @return a binary message
   * @throws NullPointerException if the message is null
   */
  public static BinaryMessage toBinary(Message message) {
    if (message instanceof MessageReader) {
      return (MessageReader) message;
    } else {
      return new BinaryMessageBuilder(message).build();
    }
  }

  BinaryMessageBuilder(Message message) {
    this.message = checkNotNull(message);
  }

  BinaryMessage build() {
    ByteBuffer buffer = allocateBuffer();
    putHeader(buffer);
    putBody(buffer);
    putSignature(buffer);
    buffer.flip();
    return MessageReader.wrap(buffer);
  }

  private ByteBuffer allocateBuffer() {
    int size = message.size();
    return messageBufferAllocator.apply(size)
        .order(ByteOrder.LITTLE_ENDIAN);
  }

  private void putHeader(ByteBuffer buffer) {
    buffer.put(NET_ID_OFFSET, message.getNetworkId())
        .put(VERSION_OFFSET, message.getVersion())
        .putShort(SERVICE_ID_OFFSET, message.getServiceId())
        .putShort(MESSAGE_TYPE_OFFSET, message.getMessageType())
        .putInt(PAYLOAD_LENGTH_OFFSET, message.size());
  }

  private void putBody(ByteBuffer buffer) {
    buffer.position(BODY_OFFSET);
    buffer.put(message.getBody());
  }

  private void putSignature(ByteBuffer buffer) {
    buffer.position(message.signatureOffset());
    buffer.put(message.getSignature());
  }
}
