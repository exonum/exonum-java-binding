package com.exonum.binding.qaservice.transactions;

import static com.exonum.binding.qaservice.transactions.QaTransactionTemplate.newQaTransactionBuilder;
import static com.exonum.binding.qaservice.transactions.TransactionPreconditions.checkMessageSize;
import static com.exonum.binding.qaservice.transactions.TransactionPreconditions.checkTransaction;
import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.Hashing;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.qaservice.QaSchema;
import com.exonum.binding.qaservice.transactions.converters.TransactionMessageConverter;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A transaction incrementing the given counter. Always valid, does nothing if the counter
 * is unknown.
 */
public final class IncrementCounterTx implements Transaction {

  private static final short ID = QaTransaction.INCREMENT_COUNTER.id;

  /** A size of message body of this transaction: seed + hash code of the counter name. */
  @VisibleForTesting
  static final int BODY_SIZE = Long.BYTES + Hashing.DEFAULT_HASH_SIZE_BYTES;

  private final long seed;
  private final HashCode counterId;

  /**
   * Creates a new increment counter transaction.
   *
   * @param seed transaction seed
   * @param counterId counter id, a hash of the counter name
   */
  public IncrementCounterTx(long seed, HashCode counterId) {
    this.seed = seed;
    this.counterId = checkNotNull(counterId);
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public void execute(Fork view) {
    QaSchema schema = new QaSchema(view);
    try (ProofMapIndexProxy<HashCode, Long> counters = schema.counters()) {
      // Increment the counter if there is such.
      if (counters.containsKey(counterId)) {
        long newValue = counters.get(counterId) + 1;
        counters.put(counterId, newValue);
      }
    }
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
    IncrementCounterTx that = (IncrementCounterTx) o;
    return seed == that.seed
        && Objects.equal(counterId, that.counterId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(seed, counterId);
  }

  static TransactionMessageConverter<IncrementCounterTx> converter() {
    return MessageConverter.INSTANCE;
  }

  private enum MessageConverter implements TransactionMessageConverter<IncrementCounterTx> {
    INSTANCE;

    @Override
    public IncrementCounterTx fromMessage(Message message) {
      checkMessage(message);

      // Unpack the message.
      ByteBuffer buf = message.getBody().order(ByteOrder.LITTLE_ENDIAN);
      assert buf.remaining() == BODY_SIZE;

      long seed = buf.getLong();

      byte[] hash = new byte[Hashing.DEFAULT_HASH_SIZE_BYTES];
      buf.get(hash);
      HashCode counterId = HashCode.fromBytes(hash);

      return new IncrementCounterTx(seed, counterId);
    }

    @Override
    public BinaryMessage toMessage(IncrementCounterTx transaction) {
      return newQaTransactionBuilder(ID)
          .setBody(serialize(transaction))
          .buildRaw();
    }

    private void checkMessage(Message message) {
      checkTransaction(message, ID);
      checkMessageSize(message, BODY_SIZE);
    }

    private static ByteBuffer serialize(IncrementCounterTx transaction) {
      ByteBuffer body = ByteBuffer.allocate(BODY_SIZE)
          .order(ByteOrder.LITTLE_ENDIAN)
          .putLong(transaction.seed)
          .put(transaction.counterId.asBytes());
      body.rewind();
      return body;
    }
  }
}
