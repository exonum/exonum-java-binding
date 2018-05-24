package com.exonum.binding.service.adapters;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.messages.Transaction;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.storage.database.Fork;
import com.google.common.annotations.VisibleForTesting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An adapter of a user-facing interface {@link Transaction} to an interface with a native code.
 */
@SuppressWarnings({"unused", "WeakerAccess"})  // Methods are called from the native proxy
public final class UserTransactionAdapter {

  private static final Logger logger = LogManager.getLogger(UserTransactionAdapter.class);

  @VisibleForTesting
  final Transaction transaction;

  public UserTransactionAdapter(Transaction transaction) {
    this.transaction = checkNotNull(transaction, "Transaction must not be null");
  }

  public boolean isValid() {
    try {
      return transaction.isValid();
    } catch (Throwable e) {
      logUserException(e);
      throw e;
    }
  }

  public void execute(long forkNativeHandle) {
    try {
      assert forkNativeHandle != 0L : "Fork handle must not be 0";

      try (Cleaner cleaner = new Cleaner("Transaction#execute")) {
        Fork view = Fork.newInstance(forkNativeHandle, false, cleaner);
        transaction.execute(view);
      }

    } catch (CloseFailuresException e) {
      logger.error(e);
      throw new RuntimeException(e);
    } catch (Throwable e) {
      logUserException(e);
      throw e;
    }
  }

  public String info() {
    try {
      return transaction.info();
    } catch (Throwable e) {
      logUserException(e);
      throw e;
    }
  }

  private void logUserException(Throwable e) {
    logger.error("", e);
  }
}
