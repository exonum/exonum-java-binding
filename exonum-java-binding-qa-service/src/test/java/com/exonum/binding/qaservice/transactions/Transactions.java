package com.exonum.binding.qaservice.transactions;

import com.exonum.binding.messages.Message;
import com.exonum.binding.qaservice.QaService;
import java.nio.ByteBuffer;

final class Transactions {

  /** A template of QA service transaction. */
  static final Message QA_TX_MESSAGE_TEMPLATE = new Message.Builder()
      .setNetworkId((byte) 0)
      .setVersion((byte) 0)
      .setServiceId(QaService.ID)
      .setMessageType(Short.MAX_VALUE)
      .setBody(ByteBuffer.allocate(0))
      .setSignature(ByteBuffer.allocate(Message.SIGNATURE_SIZE))
      .buildPartial();

  private Transactions() {
    throw new AssertionError("Non-instantiable");
  }
}
