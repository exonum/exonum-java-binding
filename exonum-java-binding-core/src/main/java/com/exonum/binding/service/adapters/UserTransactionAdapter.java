package com.exonum.binding.service.adapters;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.messages.Transaction;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.proxy.ProxyContext;
import com.exonum.binding.storage.database.ForkProxy;
import com.google.common.annotations.VisibleForTesting;

/**
 * An adapter of a user-facing interface {@link Transaction} to an interface with a native code.
 */
@SuppressWarnings({"unused", "WeakerAccess"})  // Methods are called from the native proxy
public class UserTransactionAdapter {

  @VisibleForTesting
  final Transaction transaction;

  public UserTransactionAdapter(Transaction transaction) {
    this.transaction = checkNotNull(transaction, "Transaction must not be null");
  }

  public boolean isValid() {
    return transaction.isValid();
  }

  public void execute(long forkNativeHandle) {
    assert forkNativeHandle != 0L : "ForkProxy handle must not be 0";

    try (ProxyContext context = new ProxyContext()) {
      ForkProxy view = new ForkProxy(forkNativeHandle, false);
      context.add(view);

      transaction.execute(view);
    } catch (CloseFailuresException e) {
      // todo: log properly
      e.printStackTrace();
    }
  }

  public String info() {
    return transaction.info();
  }
}
