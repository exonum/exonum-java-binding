/*
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.service.adapters;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.messages.Transaction;
import com.exonum.binding.messages.TransactionExecutionException;
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

  private final ViewFactory viewFactory;

  public UserTransactionAdapter(Transaction transaction, ViewFactory viewFactory) {
    this.transaction = checkNotNull(transaction, "Transaction must not be null");
    this.viewFactory = checkNotNull(viewFactory, "viewFactory");
  }

  public boolean isValid() {
    try {
      return transaction.isValid();
    } catch (Throwable e) {
      logUnexpectedException(e);
      throw e;
    }
  }

  public void execute(long forkNativeHandle) throws TransactionExecutionException {
    try {
      assert forkNativeHandle != 0L : "Fork handle must not be 0";

      try (Cleaner cleaner = new Cleaner("Transaction#execute")) {
        Fork view = viewFactory.createFork(forkNativeHandle, cleaner);
        transaction.execute(view);
      }

    } catch (TransactionExecutionException e) {
      logger.info("Transaction {} failed:", transaction, e);
      throw e;
    } catch (CloseFailuresException e) {
      logger.error("Failed to close some resources during transaction {} execution:",
          transaction, e);
      throw new RuntimeException(e);
    } catch (Throwable e) {
      logUnexpectedException(e);
      throw e;
    }
  }

  public String info() {
    try {
      return transaction.info();
    } catch (Throwable e) {
      logUnexpectedException(e);
      throw e;
    }
  }

  private void logUnexpectedException(Throwable e) {
    logger.error("Unexpected exception:", e);
  }
}
