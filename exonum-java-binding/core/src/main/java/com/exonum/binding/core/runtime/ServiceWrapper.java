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

package com.exonum.binding.core.runtime;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Throwables.throwIfInstanceOf;
import static java.lang.String.format;

import com.exonum.binding.core.service.BlockCommittedEvent;
import com.exonum.binding.core.service.Configurable;
import com.exonum.binding.core.service.Configuration;
import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.service.Service;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.transaction.ExecutionException;
import com.exonum.binding.core.transaction.TransactionContext;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.UrlEscapers;
import com.google.inject.Inject;
import io.vertx.ext.web.Router;

/**
 * The service wrapper represents an Exonum service as a whole and allows the service runtime
 * to operate on them conveniently. It separates the <em>extension</em>,
 * user-facing, interface from the <em>runtime</em>, internal, interface.
 */
final class ServiceWrapper {

  /**
   * Default interface comprised of transactions defined in the service implementation
   * (intrinsic to this service).
   */
  static final String DEFAULT_INTERFACE_NAME = "";
  /**
   * The id of the supervisor service instance, allowed to invoke configuration operations.
   *
   * <p>See SUPERVISOR_INSTANCE_ID in Exonum.
   */
  static final int SUPERVISOR_SERVICE_ID = 0;

  // These constants are defined in this class till a generic approach to invoke interface
  // methods is implemented.
  // See Configure trait in Exonum.
  @VisibleForTesting static final String CONFIGURE_INTERFACE_NAME = "exonum.Configure";
  @VisibleForTesting static final int VERIFY_CONFIGURATION_TX_ID = 0;
  @VisibleForTesting static final int APPLY_CONFIGURATION_TX_ID = 1;

  private final Service service;
  private final ServiceInstanceSpec instanceSpec;
  private final TransactionInvoker invoker;
  private final Node node;

  /**
   * Creates a new ServiceWrapper.
   *
   * @param service a service instance
   * @param instanceSpec an instance specification
   * @param transactionInvoker an invoker for the given {@code service}
   * @param node a node to inject into a service API controller
   */
  @Inject
  ServiceWrapper(Service service, ServiceInstanceSpec instanceSpec,
      TransactionInvoker transactionInvoker, Node node) {
    this.service = service;
    this.instanceSpec = instanceSpec;
    this.invoker = transactionInvoker;
    this.node = node;
  }

  /**
   * Returns the service instance.
   */
  Service getService() {
    return service;
  }

  /**
   * Returns the name of this service instance.
   */
  String getName() {
    return instanceSpec.getName();
  }

  /**
   * Returns id of this service instance.
   */
  int getId() {
    return instanceSpec.getId();
  }

  void initialize(Fork view, Configuration configuration) {
    callServiceMethod(() -> service.initialize(view, configuration));
  }

  void resume(Fork view, Configuration arguments) {
    callServiceMethod(() -> service.resume(view, arguments));
  }

  void executeTransaction(String interfaceName, int txId, byte[] arguments, int callerServiceId,
      TransactionContext context) {
    switch (interfaceName) {
      case DEFAULT_INTERFACE_NAME: {
        executeIntrinsicTransaction(txId, arguments, context);
        break;
      }
      case CONFIGURE_INTERFACE_NAME: {
        executeConfigurableTransaction(txId, arguments, callerServiceId, context);
        break;
      }
      default: throw new IllegalArgumentException(
          format("Unknown interface (name=%s, txId=%d)", interfaceName, txId));
    }
  }

  private void executeIntrinsicTransaction(int txId, byte[] arguments, TransactionContext context) {
    invoker.invokeTransaction(txId, arguments, context);
  }

  private void executeConfigurableTransaction(int txId, byte[] arguments, int callerServiceId,
      TransactionContext context) {
    // Check the service implements Configurable
    checkArgument(service instanceof Configurable, "Service (%s) doesn't implement Configurable",
        getName());
    // Check the caller is the supervisor
    checkArgument(callerServiceId == SUPERVISOR_SERVICE_ID, "Invalid caller service id (%s). "
        + "Operations in Configurable interface may only be invoked by the supervisor service (%s)",
        callerServiceId, SUPERVISOR_SERVICE_ID);
    // Invoke the Configurable operation
    Configurable configurable = (Configurable) service;
    Fork fork = context.getFork();
    Configuration config = new ServiceConfiguration(arguments);
    switch (txId) {
      case VERIFY_CONFIGURATION_TX_ID:
        callServiceMethod(() -> configurable.verifyConfiguration(fork, config));
        break;
      case APPLY_CONFIGURATION_TX_ID:
        callServiceMethod(() -> configurable.applyConfiguration(fork, config));
        break;
      default:
        throw new IllegalArgumentException(
            format("Unknown txId (%d) in Configurable interface", txId));
    }
  }

  /**
   * Calls a service method — a method that is specified to throw {@link ExecutionException}.
   *
   * <p>Exceptions are handled as follows:
   * - {@link ExecutionException} is propagated as-is
   * - Any other exception is wrapped into {@link UnexpectedExecutionException}
   */
  private static void callServiceMethod(Runnable serviceMethod) {
    try {
      serviceMethod.run();
    } catch (Exception e) {
      // Propagate ExecutionExceptions as-is
      throwIfInstanceOf(e, ExecutionException.class);
      // Wrap any other exception type
      throw new UnexpectedExecutionException(e);
    }
  }

  void afterTransactions(Fork fork) {
    try {
      service.afterTransactions(fork);
    } catch (ExecutionException e) {
      // Re-throw as is to keep the error code
      // todo: consider _always_ wrapping in UserCodeException to avoid conditional code
      //  both in all exception handlers **and** in native code? Would it be simpler?
      //  It seems that an even simpler approach is to _not_ distinguish service-originated
      //  'unexpected' errors and 'runtime' errors — then we don't need to wrap any exceptions
      //  (if we also forbid checked in @Transaction methods), but in this case we will
      //  go **against** the spec (see ErrorKind) and lose the benefit of error kind telling
      //  the error source ('unexpected' — only in service code,
      //  'runtime' — only somewhere in the runtime).
      throw e;
    } catch (Exception e) {
      throw new UnexpectedExecutionException(e);
    }
  }

  void afterCommit(BlockCommittedEvent event) {
    service.afterCommit(event);
  }

  void createPublicApiHandlers(Router router) {
    service.createPublicApiHandlers(node, router);
  }

  /**
   * Returns the relative path fragment on which to mount the API of this service.
   * The path fragment is already escaped and can be combined with other URL path fragments.
   */
  String getPublicApiRelativePath() {
    // At the moment, we treat the service name as a single path segment (i.e., our path
    // fragment consists of a single segment — all slashes will be escaped).
    // todo: [ECR-3448] make this user-configurable? If so, is it one of predefined keys
    //  in the normal service configuration, or a separate configuration?
    return UrlEscapers.urlPathSegmentEscaper()
        .escape(getName());
  }

  /**
   * Closes an access to the node within the service.
   */
  void requestToStop() {
    node.close();
  }
}
