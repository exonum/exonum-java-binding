package com.exonum.binding.service.adapters;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.messages.Transaction;
import com.exonum.binding.storage.database.Fork;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

/**
 * An adapter of a user-facing interface {@link Transaction} to an interface with a native code.
 */
@SuppressWarnings({"unused", "WeakerAccess"})  // Methods are called from the native proxy
public class UserTransactionAdapter {

  @VisibleForTesting
  final Transaction transaction;

  @Inject
  public UserTransactionAdapter(Transaction transaction) {
    this.transaction = checkNotNull(transaction, "Transaction must not be null");
  }

  public boolean isValid() {
    return transaction.isValid();
  }

  public void execute(long forkNativeHandle) {
    assert forkNativeHandle != 0L : "Fork handle must not be 0";
    try (Fork view = new Fork(forkNativeHandle, false)) {
      transaction.execute(view);
    }
  }

  public String info() {
    return transaction.info();
  }
}
