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

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.storage.indices.ProofListIndexProxy;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.transaction.Transaction;
import io.vertx.ext.web.Router;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * An Exonum service.
 *
 * <p>You shall usually subclass an {@link AbstractService} which implements some of the methods
 * declared in this interface.
 */
public interface Service {

  /**
   * Returns the id of the service.
   */
  short getId();

  /**
   * Returns the name of the service.
   */
  String getName();

  /**
   * Initializes the service. This method is called once when a genesis block is created
   * and is supposed to
   * <ul>
   *   <li>(a) initialize the database schema of this service, and</li>
   *   <li>(b) provide an initial <a href="https://exonum.com/doc/version/0.10/architecture/services/#global-configuration">global configuration</a>
   * of the service.</li>
   * </ul>
   *
   * <p>The service configuration parameters must be provided as a JSON string.
   * It is recorded in a table of global configuration parameters of each service deployed
   * in the network.
   *
   * @param fork a database fork to apply changes to. Not valid after this method returns
   * @return a global configuration of the service, or {@code Optional.empty()} if the service
   *         does not have any configuration parameters.
   * @see <a href="https://exonum.com/doc/version/0.10/architecture/services/#genesis-block-handler">Genesis block handler</a>
   */
  default Optional<String> initialize(Fork fork) {
    return Optional.empty();
  }

  /**
   * Converts an Exonum raw transaction to an executable transaction of <em>this</em> service.
   *
   * @param rawTransaction a raw transaction to be converted
   * @return an executable transaction
   * @throws IllegalArgumentException if the raw transaction is malformed
   *         or it doesn't belong to this service
   * @throws NullPointerException if raw transaction is null
   */
  Transaction convertToTransaction(RawTransaction rawTransaction);

  /**
   * Returns a list of root hashes of all Merkelized tables defined by this service,
   * as of the given snapshot of the blockchain state. If the service doesn't have any Merkelized
   * tables, returns an empty list.
   *
   * <p>The core uses this list to aggregate hashes of tables defined by all services
   * into a single Merkelized meta-map.  The hash of this meta-map is considered the hash
   * of the entire blockchain state and is recorded as such in blocks and Precommit messages.
   *
   * @param snapshot a snapshot of the blockchain state. Not valid after this method returns
   * @see ProofListIndexProxy#getRootHash()
   * @see ProofMapIndexProxy#getRootHash()
   */
  default List<HashCode> getStateHashes(Snapshot snapshot) {
    return Collections.emptyList();
  }

  /**
   * Creates handlers that makes up the public API of this service.
   * The handlers are added to the given router, which is then mounted at a path,
   * equal to the service name.
   *
   * <p>Please note that the path prefix is stripped from the request path when it is forwarded to
   * the given router. For example, if your service name is «timestamping»,
   * and you have an endpoint «/timestamp», use this name when defining the handler and it will be
   * available by path «/api/services/timestamping/timestamp»:
   *
   * <pre><code>
   * router.get("/balance").handler((rc) -> {
   *   rc.response().end("2019-04-01T10:15:30+02:00[Europe/Kiev]");
   * });
   * </code></pre>
   *
   * @param node a set-up Exonum node, providing an interface to access
   *             the current blockchain state and submit transactions
   * @param router a router responsible for handling requests to this service
   */
  void createPublicApiHandlers(Node node, Router router);

  /**
   * Handles read-only block commit event. This handler is an optional callback method which is
   * invoked by the blockchain after each block commit. For example, a service can create one or
   * more transactions if a specific condition has occurred.
   *
   * <p>This method is invoked synchronously from the thread that commits the block, therefore,
   * implementations of this method must not perform any blocking or long-running operations.
   *
   * <p>Any exceptions in this method will be swallowed and will not affect the processing of
   * transactions or blocks.
   *
   * @param event the read-only context allowing to access the blockchain state as of that committed
   *     block
   */
  default void afterCommit(BlockCommittedEvent event) {}
}
