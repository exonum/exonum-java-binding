package com.exonum.binding.qaservice.transactions;

import static com.exonum.binding.qaservice.transactions.TransactionPreconditions.checkMessageSize;
import static com.exonum.binding.qaservice.transactions.TransactionPreconditions.checkTransaction;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.Hashing;
import com.exonum.binding.messages.AbstractTransaction;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.qaservice.QaSchema;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

/**
 * A transaction incrementing the given counter. Always valid, does nothing if the counter
 * is unknown.
 */
final class IncrementCounterTx extends AbstractTransaction {

  private static final short ID = QaTransaction.INCREMENT_COUNTER.id;

  /** A size of message body of this transaction: seed + hash code of the counter name. */
  @VisibleForTesting
  static final int BODY_SIZE = Long.BYTES + Hashing.DEFAULT_HASH_SIZE_BYTES;

  private final long seed;
  private final HashCode counterId;

  /**
   * Creates a new increment counter transaction from the given message.
   *
   * @throws IllegalArgumentException if message is not a valid increment counter transaction
   */
  IncrementCounterTx(BinaryMessage message) {
    super(message);

    checkMessage();

    // Unpack the message.
    ByteBuffer buf = message.getBody().order(ByteOrder.LITTLE_ENDIAN);
    assert buf.remaining() == BODY_SIZE;

    seed = buf.getLong();

    byte[] hash = new byte[Hashing.DEFAULT_HASH_SIZE_BYTES];
    buf.get(hash);
    counterId = HashCode.fromBytes(hash);
  }

  private void checkMessage() {
    checkTransaction(message, ID);
    checkMessageSize(message, BODY_SIZE);
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
    // Long is saved as a hex string, because not all longs are representable as doubles.
    Map<String, Object> txBody = ImmutableMap.of(
        "seed", Long.toHexString(seed),
        "counter_id", counterId
    );
    return new QaTransactionJsonWriter().toJson(ID, txBody);
  }
}
