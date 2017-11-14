package com.exonum.binding.messages;

import static com.exonum.binding.messages.ByteBufferAllocator.allocateBuffer;
import static com.exonum.binding.messages.Message.SIGNATURE_SIZE;

public class TemplateMessage {

  /**
   * An immutable template message, that has all fields of a message set.
   * Use it in tests when you need a binary message with no or a few fields set
   * to particular values.
   */
  public static final Message TEMPLATE_MESSAGE = new Message.Builder()
      .setNetworkId((byte) 1)
      .setVersion((byte) 0)
      .setServiceId((short) 0)
      .setMessageType((short) 1)
      .setBody(allocateBuffer(2))
      .setSignature(allocateBuffer(SIGNATURE_SIZE))
      .buildPartial();

  private TemplateMessage() {}
}
