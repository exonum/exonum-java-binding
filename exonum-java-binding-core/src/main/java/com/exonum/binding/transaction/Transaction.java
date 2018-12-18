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

package com.exonum.binding.transaction;

/**
 * An Exonum transaction.
 *
 * @see <a href="https://exonum.com/doc/architecture/transactions">Exonum Transactions</a>
 * @see <a href="https://exonum.com/doc/architecture/services">Exonum Services</a>
 */
@FunctionalInterface
public interface Transaction {

  /**
   * Execute the transaction, possibly modifying the blockchain state.
   *
   * @param context a transaction execution context, which allows to access transaction-related
   *     data.
   * @throws TransactionExecutionException if the transaction cannot be executed normally
   *     and has to be rolled back. The transaction will be committed as failed (status "error"),
   *     the error code with the optional description will be saved into the storage. The client
   *     can request the error code to know the reason of the failure.
   * @throws RuntimeException if an unexpected error occurs. A correct transaction implementation
   *     must not throw such exceptions. The transaction will be committed as failed
   *     (status "panic").
   */
  void execute(TransactionContext context) throws TransactionExecutionException;

}
