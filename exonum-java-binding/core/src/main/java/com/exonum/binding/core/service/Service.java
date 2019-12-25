/*
 * Copyright 2019 The Exonum Team
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

package com.exonum.binding.core.service;

import com.exonum.binding.core.storage.database.Fork;
import io.vertx.ext.web.Router;

/**
 * An Exonum service.
 *
 * <p>You shall usually subclass an {@link AbstractService} which implements some of the methods
 * declared in this interface.
 */
public interface Service {

  /**
   * Performs an initial configuration of the service instance. This method is called <em>once</em>
   * after the service instance is added to the blockchain and allows initializing
   * some persistent data of the service.
   *
   * <p>As Exonum passes the configuration parameters only once and does not persist them for
   * later access, this service method must make any needed changes to the database based
   * on these parameters. For example, it may initialize some collections in its schema
   * (including one-off initialization that does not depend on the parameters);
   * or save all or some configuration parameters as is for later retrieval in transactions
   * and/or read requests.
   *
   * @param fork a database fork to apply changes to. Not valid after this method returns
   * @param configuration the service configuration parameters
   * @throws IllegalArgumentException if the configuration parameters are not valid (e.g.,
   *     malformed, or do not meet the preconditions). Exonum will stop the service if
   *     its initialization fails
   * @see Configurable
   */
  default void initialize(Fork fork, Configuration configuration) {
    // No configuration
  }

  /**
   * Creates handlers that make up the public HTTP API of this service.
   * The handlers are added to the given router, which is then mounted at the following path:
   * {@code /api/services/<service-name>}.
   *
   * <p>Please note that the path prefix shown above is stripped from the request path
   * when it is forwarded to the given router. For example, if your service name is «timestamping»,
   * and you have an endpoint «/timestamp», use «/timestamp» as the endpoint name when defining
   * the handler and it will be available by path «/api/services/timestamping/timestamp»:
   *
   * <pre>{@code
   * router.get("/timestamp").handler((rc) -> {
   *   rc.response().end("2019-04-01T10:15:30+02:00[Europe/Kiev]");
   * });
   * }</pre>
   *
   * <p>Please remember that Java services use a <em>separate</em> server from Rust services.
   * The Java server TCP port is specified on node start, see
   * <a href="https://exonum.com/doc/version/0.13-rc.2/get-started/java-binding/#running-the-node">
   * documentation</a> for details.
   *
   * @param node a set-up Exonum node, providing an interface to access
   *             the current blockchain state and submit transactions
   * @param router a router responsible for handling requests to this service
   * @see <a href="https://exonum.com/doc/version/0.13-rc.2/get-started/java-binding/#external-service-api">
   *   Documentation on service API</a>
   */
  void createPublicApiHandlers(Node node, Router router);

  /**
   * Handles the changes made by all transactions included in the upcoming block.
   * This handler is an optional callback method invoked by the blockchain after all transactions
   * in a block are executed, but before it is committed. The service can modify its state
   * in this handler, therefore, implementations must be deterministic and use only the current
   * database state as their input.
   *
   * <p>This method is invoked synchronously from the thread that commits the block, therefore,
   * implementations of this method must not perform any blocking or long-running operations.
   *
   * <p>Any exceptions in this method will revert any changes made to the database by it,
   * but will not affect the processing of this block.
   */
  default void afterTransactions(Fork fork) {}

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
