package com.exonum.binding.messages;

import com.exonum.binding.hash.Hashes;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
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

  // todo: fromBuffer/wrap(ByteBuffer)?

  /**
   * Returns the whole binary message.
   */
  // todo: consider renaming, for this class *is* a message.
  //   - ¿Message#getBuffer
  //   - ¿Message#getMessageBuffer
  //   - ¿Message#getMessagePacket
  ByteBuffer getMessage();

  /**
   * Returns the SHA-256 hash of this message.
   */
  default HashCode hash() {
    HashFunction hashFunction = Hashes.defaultHashFunction();
    return hashFunction.hashBytes(getMessage());
  }
}
