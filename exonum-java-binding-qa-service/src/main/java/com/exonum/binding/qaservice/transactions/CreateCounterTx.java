package com.exonum.binding.qaservice.transactions;

import static com.exonum.binding.qaservice.transactions.TransactionPreconditions.checkTransaction;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.Hashing;
import com.exonum.binding.messages.AbstractTransaction;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.qaservice.QaSchema;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.storage.serialization.StandardSerializers;
import com.google.common.collect.ImmutableMap;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * A transaction creating a new named counter.
 */
final class CreateCounterTx extends AbstractTransaction {

  private static final short ID = QaTransaction.CREATE_COUNTER.id;

  private final String name;

  CreateCounterTx(BinaryMessage message) {
    super(checkTransaction(message, ID));
    name = from(message.getBody());
  }

  private static String from(ByteBuffer body) {
    byte[] s = getRemainingBytes(body);

    return StandardSerializers.string()
        .fromBytes(s);
  }

  private static byte[] getRemainingBytes(ByteBuffer body) {
    int numBytes = body.remaining();
    byte[] s = new byte[numBytes];
    body.get(s);
    return s;
  }

  @Override
  public boolean isValid() {
    return !name.trim().isEmpty();
  }

  @Override
  public void execute(Fork view) {
    QaSchema schema = new QaSchema(view);
    try (MapIndex<HashCode, Long> counters = schema.counters();
         MapIndex<HashCode, String> names = schema.counterNames()) {

      HashCode counterId = Hashing.defaultHashFunction()
          .hashString(name, UTF_8);
      if (counters.containsKey(counterId)) {
        return;
      }
      assert !names.containsKey(counterId) : "counterNames must not contain the id of " + name;

      counters.put(counterId, 0L);
      names.put(counterId, name);
    }
  }

  @Override
  public String info() {
    Map<String, Object> txBody = ImmutableMap.of("name", name);
    return new QaTransactionJsonWriter().toJson(ID, txBody);
  }
}
