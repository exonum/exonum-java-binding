package com.exonum.binding.qaservice.transactions;

import static com.exonum.binding.qaservice.transactions.TransactionPreconditions.checkMessageSize;
import static com.exonum.binding.qaservice.transactions.TransactionPreconditions.checkTransaction;

import com.exonum.binding.messages.AbstractTransaction;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.storage.database.Fork;
import com.google.common.base.MoreObjects;

/**
 * An invalid transaction always returning false in {@link #isValid()}.
 */
final class InvalidTx extends AbstractTransaction {

  private static final short ID = QaTransaction.INVALID.id;
  private static final int BODY_SIZE = 0;

  InvalidTx(BinaryMessage message) {
    super(checkTransaction(message, ID));
    checkMessageSize(message, BODY_SIZE);
  }

  @Override
  public boolean isValid() {
    return false;
  }

  @Override
  public void execute(Fork view) {
    throw new AssertionError("Must never be executed by the framework: " + this);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("message", message)
        .toString();
  }
}
