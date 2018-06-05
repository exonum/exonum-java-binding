package com.exonum.binding.qaservice.transactions;

import static com.exonum.binding.qaservice.transactions.QaTransactionTemplate.newQaTransactionBuilder;
import static com.exonum.binding.qaservice.transactions.TransactionPreconditions.checkMessageSize;
import static com.exonum.binding.qaservice.transactions.TransactionPreconditions.checkTransaction;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.qaservice.QaSchema;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.indices.MapIndex;
import com.google.common.base.Objects;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class ValidThrowingTx implements Transaction {

  private static final short ID = QaTransaction.VALID_THROWING.id();

  private final long seed;

  public ValidThrowingTx(long seed) {
    this.seed = seed;
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
    MapIndex<HashCode, Long> counters = schema.counters();
    MapIndex<HashCode, String> names = schema.counterNames();

    // Attempt to clear all service indices.
    counters.clear();
    names.clear();

    // Throw an exception. Framework must revert the changes made above.
    throw new IllegalStateException("#execute of this transaction always throws: " + this);
  }

  @Override
  public String info() {
    return new QaTransactionGson().toJson(ID, this);
  }

  @Override
  public BinaryMessage getMessage() {
    return converter().toMessage(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ValidThrowingTx that = (ValidThrowingTx) o;
    return seed == that.seed;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(seed);
  }

  static TransactionMessageConverter<ValidThrowingTx> converter() {
    return TransactionConverter.INSTANCE;
  }

  private enum TransactionConverter implements TransactionMessageConverter<ValidThrowingTx> {
    INSTANCE;

    static final int BODY_SIZE = Long.BYTES;

    @Override
    public ValidThrowingTx fromMessage(Message message) {
      checkMessage(message);

      long seed = message.getBody()
          .order(ByteOrder.LITTLE_ENDIAN)
          .getLong();

      return new ValidThrowingTx(seed);
    }

    @Override
    public BinaryMessage toMessage(ValidThrowingTx transaction) {
      return newQaTransactionBuilder(ID)
          .setBody(serialize(transaction))
          .buildRaw();
    }

    private void checkMessage(Message txMessage) {
      checkTransaction(txMessage, ID);
      checkMessageSize(txMessage, BODY_SIZE);
    }

    private static ByteBuffer serialize(ValidThrowingTx transaction) {
      ByteBuffer body = ByteBuffer.allocate(Long.BYTES)
          .order(ByteOrder.LITTLE_ENDIAN)
          .putLong(transaction.seed);
      body.rewind();
      return body;
    }
  }
}
