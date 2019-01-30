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

import com.exonum.binding.blockchain.Blockchain;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.transaction.RawTransaction;
import java.util.function.Function;

/**
 * An Exonum node context. Allows to add transactions to Exonum network
 * and get a snapshot of the database state.
 */
public interface Node {

  /**
   * Creates a transaction from the given parameters, signs it with
   * the {@linkplain #getPublicKey() node service key}, and then submits it into Exonum network.
   * This node does <em>not</em> execute the transaction immediately, but broadcasts it to all
   * the nodes in the network. Then each node verifies the transaction and, if it is correct, adds it to
   * the <a href="https://exonum.com/doc/advanced/consensus/specification/#pool-of-unconfirmed-transactions">pool of unconfirmed transactions</a>.
   * The transaction is executed later asynchronously.
   *
   * <p>Incorrect transactions (e.g., the payload of which cannot be deserialized by the target service, or which
   * have unknown message id) are rejected by the network.
   *
   * <p/><em>Be aware that each node has its own service key pair, therefore
   * invocations of this method on different nodes will produce different transactions.</em>
   *
   * @param rawTransaction transaction parameters to include in transaction message
   * @return hash of the transaction message created by the framework
   * @throws InternalServerError if this node failed to process the transaction
   * @throws NullPointerException if the transaction is null
   * @see Blockchain#getTxMessages()
   */
  HashCode submitTransaction(RawTransaction rawTransaction)
      throws InternalServerError;

  /**
   * Performs a given function with a snapshot of the current database state.
   *
   * @param snapshotFunction a function to execute
   * @param <ResultT> a type the function returns
   * @return the result of applying the given function to the database state
   */
  <ResultT> ResultT withSnapshot(Function<Snapshot, ResultT> snapshotFunction);

  /**
   * Returns the service public key of this node. The corresponding private key is used
   * for signing transactions in {@link #submitTransaction(RawTransaction)}.
   *
   * <p>This key is stored under "service_public_key" key in the node configuration file.
   *
   * @throws IllegalStateException if the node proxy is closed
   */
  PublicKey getPublicKey();
}
