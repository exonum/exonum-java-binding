package com.exonum.binding.qaservice.transactions;

import com.google.common.primitives.Shorts;


/**
 * All known QA service transactions.
 *
 * @implNote Keep in sync with {@link QaTransactionConverter#TRANSACTION_FACTORIES}.
 */
enum QaTransaction {
  // Well-behaved transactions.
  CREATE_COUNTER(0),
  INCREMENT_COUNTER(1),

  // Badly-behaved transactions, do some crazy things.
  INVALID(10),
  INVALID_THROWING(11),
  VALID_THROWING(12);

  final short id;

  QaTransaction(int id) {
    this.id = Shorts.checkedCast(id);
  }

  short id() {
    return id;
  }
}
