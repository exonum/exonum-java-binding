package com.exonum.binding.core.runtime;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.core.service.BlockCommittedEvent;
import com.exonum.binding.core.service.Service;
import com.exonum.binding.core.service.TransactionConverter;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.transaction.Transaction;
import com.exonum.binding.core.transaction.TransactionContext;
import com.exonum.binding.core.transaction.TransactionExecutionException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.BaseEncoding;
import com.google.inject.Inject;
import java.util.List;
import java.util.Properties;

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

  void configure(Fork view, Properties configuration) {
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

  @VisibleForTesting
  Service getService() {
    return service;
  }
}
