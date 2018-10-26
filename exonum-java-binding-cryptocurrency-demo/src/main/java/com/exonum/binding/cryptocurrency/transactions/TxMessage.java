package com.exonum.binding.cryptocurrency.transactions;

import com.exonum.binding.transaction.Transaction;

public final class TxMessage<BodyT extends Transaction> {

  public final short service_id;
  public final short message_id;
  public final BodyT body;

  public TxMessage(short service_id, short message_id, BodyT body) {
    this.service_id = service_id;
    this.message_id = message_id;
    this.body = body;
  }

}
