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

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.core.service.BlockCommittedEvent;
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

  private final Service service;
  private final TransactionConverter txConverter;
  private final ServiceInstanceSpec instanceSpec;

  @Inject
  ServiceWrapper(Service service, TransactionConverter txConverter,
      ServiceInstanceSpec instanceSpec) {
    this.service = service;
    this.txConverter = txConverter;
    this.instanceSpec = instanceSpec;
  }

  String getName() {
    return instanceSpec.getName();
  }

  int getId() {
    return instanceSpec.getId();
  }

  void configure(Fork view, Configuration configuration) {
    service.configure(view, configuration);
  }

  void executeTransaction(int txId, byte[] arguments, TransactionContext context)
      throws TransactionExecutionException {
    // Decode the transaction data into an executable transaction
    Transaction transaction = txConverter.toTransaction(txId, arguments);
    if (transaction == null) {
      // Use \n in the format string to ensure the message (which is likely recorded
      // to the blockchain) stays the same on any platform
      throw new NullPointerException(String.format("Invalid service implementation: "
          + "TransactionConverter#toTransaction must never return null.\n"
          + "Throw an exception if your service does not recognize this message id (%s) "
          + "or arguments (%s)", txId, BaseEncoding.base16().encode(arguments)));
    }
    // Execute it
    transaction.execute(context);
  }

  List<HashCode> getStateHashes(Snapshot snapshot) {
    return service.getStateHashes(snapshot);
  }

  void afterCommit(BlockCommittedEvent event) {
    service.afterCommit(event);
  }

  void createPublicApiHandlers(Node node, Router router) {
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

  @VisibleForTesting
  Service getService() {
    return service;
  }
}
