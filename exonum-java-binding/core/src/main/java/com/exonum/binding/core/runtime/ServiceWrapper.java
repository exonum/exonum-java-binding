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
import static java.lang.String.format;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.core.service.BlockCommittedEvent;
import com.exonum.binding.core.service.Configurable;
import com.exonum.binding.core.service.Configuration;
import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.service.Service;
import com.exonum.binding.core.service.TransactionConverter;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.transaction.Transaction;
import com.exonum.binding.core.transaction.TransactionContext;
import com.exonum.binding.core.transaction.TransactionExecutionException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.BaseEncoding;
import com.google.common.net.UrlEscapers;
import com.google.inject.Inject;
import io.vertx.ext.web.Router;
import java.util.List;

/**
 * The service wrapper represents an Exonum service as a whole and allows the service runtime
 * to operate on them conveniently. It separates the <em>extension</em>,
 * user-facing, interface from the <em>runtime</em>, internal, interface.
 */
final class ServiceWrapper {

  // These constants are defined in this class till a generic approach to invoke interface
  // methods is implemented.
  // See Configure trait in Exonum.
  @VisibleForTesting static final String CONFIGURE_INTERFACE_NAME = "exonum.Configure";
  @VisibleForTesting static final int VERIFY_CONFIGURATION_TX_ID = 0;
  @VisibleForTesting static final int APPLY_CONFIGURATION_TX_ID = 1;
  /**
   * Default interface comprised of transactions defined in the service implementation
   * (intrinsic to this service).
   * todo: @slowli What's the proper term for this interface (intrinsic, implicit, default)?
   */
  static final String DEFAULT_INTERFACE_NAME = "";

  private final Service service;
  private final TransactionConverter txConverter;
  private final ServiceInstanceSpec instanceSpec;
  private final Node node;

  @Inject
  ServiceWrapper(Service service, TransactionConverter txConverter,
      ServiceInstanceSpec instanceSpec, Node node) {
    this.service = service;
    this.txConverter = txConverter;
    this.instanceSpec = instanceSpec;
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
    service.initialize(view, configuration);
  }

  void executeTransaction(String interfaceName, int txId, byte[] arguments,
      TransactionContext context)
      throws TransactionExecutionException {
    // Decode the transaction data into an executable transaction
    Transaction transaction = convertTransaction(interfaceName, txId, arguments);
    // Execute it
    transaction.execute(context);
  }

  /**
   * Converts an Exonum raw transaction to an executable transaction of this service.
   *
   * @param interfaceName a fully-qualified name of the interface in which the transaction
   *     is defined, or empty string if it is defined in the service directly (implicit interface)
   * @param txId the {@linkplain TransactionMessage#getTransactionId() transaction type identifier}
   *     within the service
   * @param arguments the {@linkplain TransactionMessage#getPayload() serialized transaction
   *     arguments}
   * @return an executable transaction of the service
   * @throws IllegalArgumentException if the transaction is not known to the service, or the
   *     arguments are not valid: e.g., cannot be deserialized, or do not meet the preconditions
   */
  Transaction convertTransaction(String interfaceName, int txId, byte[] arguments) {
    switch (interfaceName) {
      case DEFAULT_INTERFACE_NAME: return convertIntrinsicTransaction(txId, arguments);
      case CONFIGURE_INTERFACE_NAME: return convertConfigurableTransaction(txId, arguments);
      default: throw new IllegalArgumentException(
          format("Unknown interface (name=%s, txId=%d)", interfaceName, txId));
    }
  }

  private Transaction convertIntrinsicTransaction(int txId, byte[] arguments) {
    Transaction transaction = txConverter.toTransaction(txId, arguments);
    if (transaction == null) {
      // Use \n in the format string to ensure the message (which is likely recorded
      // to the blockchain) stays the same on any platform
      throw new NullPointerException(format("Invalid service implementation: "
          + "TransactionConverter#toTransaction must never return null.\n"
          + "Throw an exception if your service does not recognize this message id (%s) "
          + "or arguments (%s)", txId, BaseEncoding.base16().encode(arguments)));
    }
    return transaction;
  }

  private Transaction convertConfigurableTransaction(int txId, byte[] arguments) {
    checkArgument(service instanceof Configurable, "Service (%s) doesn't implement Configurable",
        getName());
    Configurable configurable = (Configurable) service;
    Configuration config = new ServiceConfiguration(arguments);
    switch (txId) {
      case VERIFY_CONFIGURATION_TX_ID:
        return (ctx) -> configurable.verifyConfiguration(ctx.getFork(), config);
      case APPLY_CONFIGURATION_TX_ID:
        return (ctx) -> configurable.applyConfiguration(ctx.getFork(), config);
      default:
        throw new IllegalArgumentException(
            format("Unknown txId (%d) in Configurable interface", txId));
    }
  }

  List<HashCode> getStateHashes(Snapshot snapshot) {
    return service.getStateHashes(snapshot);
  }

  void beforeCommit(Fork fork) {
    service.beforeCommit(fork);
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
}
