package com.exonum.binding.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class ByteBufferAllocator {

  /**
   * Allocates a byte buffer of the given size, and sets its order to little-endian.
   *
   * @param size size of the buffer in bytes
   *
   * @return a new byte buffer of the given size
   */
  static ByteBuffer allocateBuffer(int size) {
    return ByteBuffer.allocate(size)
        .order(ByteOrder.LITTLE_ENDIAN);
  }

  private ByteBufferAllocator() {}
}
