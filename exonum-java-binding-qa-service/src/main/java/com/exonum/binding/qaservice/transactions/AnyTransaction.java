package com.exonum.binding.qaservice.transactions;

import com.exonum.binding.qaservice.PromoteToCore;
import com.exonum.binding.qaservice.QaService;

/**
 * Parameters of any transaction. Simplifies converting them from binary to JSON.
 *
 * @param <BodyT> type of the transaction message body (aka payload)
 */
@PromoteToCore("A similar class might be universally useful if we need to serialize transaction "
    + "data into JSON in the Exonum standard format.")
class AnyTransaction<BodyT> {
  final short service_id;
  final short message_id;
  final BodyT body;

  AnyTransaction(short message_id,
                 BodyT body) {
    this(QaService.ID, message_id, body);
  }

  private AnyTransaction(short service_id,
                         short message_id,
                         BodyT body) {
    this.service_id = service_id;
    this.message_id = message_id;
    this.body = body;
  }
}
