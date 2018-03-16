package com.exonum.binding.qaservice.transactions;

import com.exonum.binding.qaservice.PromoteToCore;
import com.exonum.binding.qaservice.QaService;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

/**
 * Parameters of any transaction. Simplifies converting them from binary to JSON.
 */
@PromoteToCore("A similar class might be universally useful if we need to serialize transaction " +
    "data into JSON in the Exonum standard format.")
class AnyTransaction {
  final short service_id;
  final short message_id;
  // When deserialized, it is mutable, but we don't care.
  final Map<String, Object> body;

  AnyTransaction(short message_id,
                 Map<String, ?> body) {
    this(QaService.ID, message_id, body);
  }

  AnyTransaction(short service_id,
                 short message_id,
                 Map<String, ?> body) {
    this.service_id = service_id;
    this.message_id = message_id;
    this.body = ImmutableMap.copyOf(body);
  }
}
