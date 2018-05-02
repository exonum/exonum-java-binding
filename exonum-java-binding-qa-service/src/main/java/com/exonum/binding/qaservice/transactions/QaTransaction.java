package com.exonum.binding.qaservice.transactions;

import com.google.common.primitives.Shorts;


/**
 * All known QA service transactions.
 *
 * @implNote Keep in sync with {@link QaTransactionConverter#TRANSACTION_FACTORIES}.
 */
public enum QaTransaction {
  // Well-behaved transactions.
  CREATE_COUNTER(0),
  INCREMENT_COUNTER(1),

  // Badly-behaved transactions, do some crazy things.
  INVALID(10),
  INVALID_THROWING(11),
  VALID_THROWING(12);

  private final short id;

  QaTransaction(int id) {
    this.id = Shorts.checkedCast(id);
  }

  /** Returns the unique id of this transaction. */
  public short id() {
    return id;
  }

}
