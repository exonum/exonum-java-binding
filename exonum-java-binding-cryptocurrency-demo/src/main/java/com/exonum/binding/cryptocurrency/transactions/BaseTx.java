package com.exonum.binding.cryptocurrency.transactions;

/**
 * Base transaction with common fields, used by all transaction types.
 */
public class BaseTx {
  private final short service_id;
  private final short message_id;

  public BaseTx(short service_id, short message_id) {
    this.service_id = service_id;
    this.message_id = message_id;
  }

  public short getService_id() {
    return service_id;
  }

  public short getMessage_id() {
    return message_id;
  }
}
