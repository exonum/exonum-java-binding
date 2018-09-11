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

package com.exonum.binding.service;

import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.transaction.Transaction;
import java.util.function.Function;

/**
 * An Exonum node context. Allows to add transactions to Exonum network
 * and get a snapshot of the database state.
 */
// todo: a better name?
public interface Node {

  /**
   * Submits a transaction into Exonum network. This node does <em>not</em> execute
   * the transaction immediately, but verifies it and, if it is valid,
   * broadcasts it to all the nodes in the network. Then each node adds the transaction to a
   * <a href="https://exonum.com/doc/advanced/consensus/specification/#pool-of-unconfirmed-transactions">pool of unconfirmed transactions</a>.
   * The transaction is executed later asynchronously.
   *
   * @param transaction a transaction to send
   * @throws InvalidTransactionException if the transaction is not valid
   * @throws InternalServerError if this node failed to process the transaction
   * @throws NullPointerException if the transaction is null
   */
  void submitTransaction(Transaction transaction)
      throws InvalidTransactionException, InternalServerError;

  /**
   * Performs a given function with a snapshot of the current database state.
   *
   * @param snapshotFunction a function to execute
   * @param <ResultT> a type the function returns
   * @return the result of applying the given function to the database state
   */
  <ResultT> ResultT withSnapshot(Function<Snapshot, ResultT> snapshotFunction);

  /**
   * Returns the public key of this node.
   *
   * @throws IllegalStateException if the node proxy is closed
   */
  byte[] getPublicKey();
}
