package com.exonum.binding.qaservice.transactions;

import com.exonum.binding.messages.Transaction;
import com.google.common.primitives.Shorts;


/**
 * All known QA service transactions.
 *
 * @implNote Keep in sync with {@link QaTransactionConverter#TRANSACTION_FACTORIES}.
 */
public enum QaTransaction {
  // Well-behaved transactions.
  CREATE_COUNTER(0, CreateCounterTx.class),
  INCREMENT_COUNTER(1, IncrementCounterTx.class),

  // Badly-behaved transactions, do some crazy things.
  INVALID(10, InvalidTx.class),
  INVALID_THROWING(11, InvalidThrowingTx.class),
  VALID_THROWING(12, ValidThrowingTx.class);

  private final short id;
  private final Class<? extends Transaction> transactionClass;

  QaTransaction(int id, Class<? extends Transaction> txClass) {
    this.id = Shorts.checkedCast(id);
    this.transactionClass = txClass;
  }

  /** Returns the unique id of this transaction. */
  public short id() {
    return id;
  }

  /** Returns the class implementing this transaction. */
  public Class<? extends Transaction> transactionClass() {
    return transactionClass;
  }
}
