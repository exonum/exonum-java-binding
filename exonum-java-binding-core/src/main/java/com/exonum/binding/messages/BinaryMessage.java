package com.exonum.binding.messages;

import java.nio.ByteBuffer;

/**
 * A binary Exonum message.
 */
public interface BinaryMessage extends Message {

  /**
   * Returns the whole binary message.
   */
  ByteBuffer getMessage();
}
