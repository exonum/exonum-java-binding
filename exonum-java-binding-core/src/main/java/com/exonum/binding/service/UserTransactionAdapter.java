package com.exonum.binding.service;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.messages.Transaction;
import com.exonum.binding.storage.database.Fork;
import com.google.common.annotations.VisibleForTesting;

/**
 * An adapter of a user-facing interface {@link Transaction} to an interface with a native code.
 */
class UserTransactionAdapter {

  @VisibleForTesting
  final Transaction transaction;

  UserTransactionAdapter(Transaction transaction) {
    this.transaction = checkNotNull(transaction, "Transaction must not be null");
  }

  @SuppressWarnings("unused")  // called from the native proxy
  boolean isValid() {
    return transaction.isValid();
  }


  @SuppressWarnings("unused")  // called from the native proxy
  void execute(long forkNativeHandle) {
    assert forkNativeHandle != 0L : "Fork handle must not be 0";
    try (Fork view = new Fork(forkNativeHandle, false)) {
      transaction.execute(view);
    }
  }

  @SuppressWarnings("unused")  // called from the native proxy
  String info() {
    return transaction.info();
  }
}
