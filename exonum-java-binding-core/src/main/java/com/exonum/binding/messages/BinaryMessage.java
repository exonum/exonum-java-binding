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

package com.exonum.binding.messages;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.HashFunction;
import com.exonum.binding.hash.Hashing;
import java.nio.ByteBuffer;

/**
 * A binary Exonum message.
 */
public interface BinaryMessage extends Message {

  /**
   * Creates a binary message from a byte array.
   *
   * @param messageBytes an array with message bytes
   * @return a binary message
   * @throws IllegalArgumentException if message has invalid size
   */
  static BinaryMessage fromBytes(byte[] messageBytes) {
    ByteBuffer buf = ByteBuffer.wrap(messageBytes);
    return MessageReader.wrap(buf);
  }

  /**
   * Returns a message without signature, i.e., without the last 64 bytes of the binary message.
   */
  default byte[] getMessageNoSignature() {
    ByteBuffer signedMessage = getSignedMessage();
    int fullSize = signedMessage.remaining();
    int messageSize = fullSize - Message.SIGNATURE_SIZE;
    byte[] message = new byte[messageSize];
    signedMessage.get(message);
    return message;
  }

  /**
   * Returns the whole binary message. It includes a message header, body and signature.
   */
  // todo: consider renaming, for this class *is* a message.
  //   - ¿Message#getBuffer
  //   - ¿Message#getMessageBuffer
  //   - ¿Message#getMessagePacket
  ByteBuffer getSignedMessage();

  /**
   * Returns the SHA-256 hash of this message.
   */
  default HashCode hash() {
    HashFunction hashFunction = Hashing.defaultHashFunction();
    return hashFunction.hashBytes(getSignedMessage());
  }
}
