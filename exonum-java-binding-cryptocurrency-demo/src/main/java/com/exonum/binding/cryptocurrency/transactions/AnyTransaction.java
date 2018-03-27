package com.exonum.binding.cryptocurrency.transactions;

import com.exonum.binding.cryptocurrency.CryptocurrencyService;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Parameters of any transaction. Simplifies converting them from binary to JSON.
 */
public class AnyTransaction<BodyT> {
  private final short service_id;
  private final short message_id;
  private final BodyT body;

  AnyTransaction(short message_id, BodyT body) {
    this(CryptocurrencyService.ID, message_id, body);
  }

  public AnyTransaction(short service_id, short message_id, BodyT body) {
    this.service_id = service_id;
    this.message_id = message_id;
    this.body = body;
  }

  public short getService_id() {
    return service_id;
  }

  public short getMessage_id() {
    return message_id;
  }

  public BodyT getBody() {
    return body;
  }
}
