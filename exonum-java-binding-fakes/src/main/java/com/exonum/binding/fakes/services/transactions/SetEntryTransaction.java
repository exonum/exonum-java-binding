package com.exonum.binding.fakes.services.transactions;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.indices.EntryIndexProxy;
import com.exonum.binding.storage.serialization.StandardSerializers;

/**
 * A transaction whose behaviour can be configured. It's not a mock: it writes a given value
 * into the database in its {@link #execute(Fork)}.
 *
 * <p>Such transaction is supposed to be used in TransactionProxy integration tests.
 */
public final class SetEntryTransaction implements Transaction {

  static final String ENTRY_NAME = "test_entry";

  private final boolean valid;
  private final String value;
  private final String info;

  /**
   * Creates a transaction with a pre-configured behaviour.
   *
   * @param valid whether a transaction has to be valid (i.e., return true
   *              in its {@link Transaction#isValid()} method)
   * @param value a value to put into an entry {@link #ENTRY_NAME}
   * @param info a value to be returned as this transaction text representation
   *     {@link Transaction#info()}
   */
  public SetEntryTransaction(boolean valid, String value, String info) {
    this.valid = valid;
    this.value = checkNotNull(value);
    this.info = checkNotNull(info);
  }

  @Override
  public boolean isValid() {
    return valid;
  }

  @Override
  public void execute(Fork view) {
    checkState(valid, "Cannot execute an invalid transaction");
    try (EntryIndexProxy<String> entry = createEntry(view)) {
      entry.set(value);
    }
  }

  @Override
  public String info() {
    return info;
  }

  @Override
  public BinaryMessage getMessage() {
    // Not needed for transaction ITs.
    throw new UnsupportedOperationException("Transaction#getMessage is not implemented");
  }

  private EntryIndexProxy<String> createEntry(Fork view) {
    return new EntryIndexProxy<>(ENTRY_NAME, view, StandardSerializers.string());
  }
}
