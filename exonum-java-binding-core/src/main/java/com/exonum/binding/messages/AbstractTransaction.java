package com.exonum.binding.messages;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An abstract Exonum transaction. It includes a reference to a binary Exonum message
 * ({@link AbstractTransaction#message}, representing this transaction.
 */
public abstract class AbstractTransaction implements Transaction {

  /**
   * A binary Exonum message, representing this transaction.
   */
  protected final BinaryMessage message;

  protected AbstractTransaction(BinaryMessage message) {
    this.message = checkNotNull(message);
  }

  @Override
  public BinaryMessage getMessage() {
    return message;
  }
}
