package com.exonum.binding.cryptocurrency.transactions;

import com.exonum.binding.cryptocurrency.CryptocurrencyService;
import com.exonum.binding.messages.Message;

import java.nio.ByteBuffer;

final class Transactions {

  static final Message TX_MESSAGE_TEMPLATE = new Message.Builder()
          .setNetworkId((byte) 0)
          .setVersion((byte) 0)
          .setServiceId(CryptocurrencyService.ID)
          .setMessageType(Short.MAX_VALUE)
          .setBody(ByteBuffer.allocate(0))
          .setSignature(ByteBuffer.allocate(Message.SIGNATURE_SIZE))
          .buildPartial();

  private Transactions() {
    throw new AssertionError("Non-instantiable");
  }
}
