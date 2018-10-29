package com.exonum.binding.cryptocurrency.transactions;

import com.exonum.binding.transaction.Transaction;

public final class TxMessage<BodyT extends Transaction> {

  private final short serviceId;
  private final short messageId;
  private final BodyT body;

  TxMessage(short serviceId, short messageId, BodyT body) {
    this.serviceId = serviceId;
    this.messageId = messageId;
    this.body = body;
  }

  public short getServiceId() {
    return serviceId;
  }

  public short getMessageId() {
    return messageId;
  }

  public BodyT getBody() {
    return body;
  }

}
