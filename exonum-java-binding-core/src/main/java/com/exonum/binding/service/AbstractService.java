package com.exonum.binding.service;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.storage.database.SnapshotProxy;
import com.exonum.binding.storage.database.ViewProxy;
import java.util.List;

/**
 * A base class for user services.
 */
public abstract class AbstractService implements Service {

  private final short id;
  private final String name;
  private final TransactionConverter transactionConverter;

  /**
   * Creates an AbstractService.
   *
   * @param id an id of the service
   * @param name a name of the service
   * @param transactionConverter a transaction converter that is aware of
   *                             all transactions of this service
   */
  protected AbstractService(short id, String name, TransactionConverter transactionConverter) {
    this.id = id;
    checkArgument(!name.isEmpty(), "The service name must not be empty");
    this.name = name;
    this.transactionConverter = checkNotNull(transactionConverter);
  }

  @Override
  public short getId() {
    return id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Transaction convertToTransaction(BinaryMessage message) {
    return transactionConverter.toTransaction(message);
  }

  @Override
  public List<HashCode> getStateHashes(SnapshotProxy snapshot) {
    return createDataSchema(snapshot).getStateHashes();
  }

  /**
   * Creates a data schema of this service.
   *
   * @param view a database view
   * @return a data schema of the service
   */
  protected abstract Schema createDataSchema(ViewProxy view);
}
