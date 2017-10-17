package com.exonum.binding.messages;

import com.exonum.binding.hash.Hashes;
import java.nio.ByteBuffer;

/**
 * A binary Exonum message.
 */
public interface BinaryMessage extends Message {

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
  default byte[] hash() {
    return Hashes.getHashOf(getMessage());
  }
}
