package com.exonum.binding.qaservice.transactions;

import static com.exonum.binding.qaservice.transactions.TransactionPreconditions.checkMessageSize;
import static com.exonum.binding.qaservice.transactions.TransactionPreconditions.checkTransaction;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.messages.AbstractTransaction;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.qaservice.QaSchema;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.indices.MapIndex;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import java.nio.ByteOrder;
import java.util.Map;

final class ValidThrowingTx extends AbstractTransaction {
  private static final short ID = QaTransaction.VALID_THROWING.id;
  private static final int BODY_SIZE = Long.BYTES;

  private final long seed;

  ValidThrowingTx(BinaryMessage message) {
    super(checkTransaction(message, ID));
    checkMessageSize(message, BODY_SIZE);

    seed = message.getBody()
        .order(ByteOrder.LITTLE_ENDIAN)
        .getLong();
  }

  @Override
  public boolean isValid() {
    return true;
  }

  /**
   * First clears the indices of the service, then throws an exception.
   *
   * @throws IllegalStateException always
   */
  @Override
  public void execute(Fork view) {
    QaSchema schema = new QaSchema(view);
    try (MapIndex<HashCode, Long> counters = schema.counters();
         MapIndex<HashCode, String> names = schema.counterNames()) {
      // Attempt to clear all service indices.
      counters.clear();
      names.clear();

      // Throw an exception. Framework must revert the changes made above.
      throw new IllegalStateException("#execute of this transaction always throws: " + this);
    }
  }

  @Override
  public String info() {
    Map<String, ?> parameters = ImmutableMap.of("seed", Long.toHexString(seed));
    return new QaTransactionJsonWriter().toJson(ID, parameters);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("message", message)
        .add("seed", seed)
        .toString();
  }
}
