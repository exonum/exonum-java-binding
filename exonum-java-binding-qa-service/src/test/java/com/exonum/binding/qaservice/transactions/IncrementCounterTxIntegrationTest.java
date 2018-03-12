package com.exonum.binding.qaservice.transactions;

import static com.exonum.binding.qaservice.transactions.CreateCounterTxIntegrationTest.createCounter;
import static com.exonum.binding.qaservice.transactions.QaTransaction.INCREMENT_COUNTER;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.Hashing;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.qaservice.QaSchema;
import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.util.LibraryLoader;
import com.google.gson.Gson;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.Test;

public class IncrementCounterTxIntegrationTest {

  static {
    LibraryLoader.load();
  }

  static Message INC_COUNTER_TX_MESSAGE_TEMPLATE = new Message.Builder()
      .mergeFrom(Transactions.QA_TX_MESSAGE_TEMPLATE)
      .setMessageType(INCREMENT_COUNTER.id)
      .setBody(ByteBuffer.allocate(IncrementCounterTx.BODY_SIZE))
      .buildPartial();

  @Test
  public void isValid() {
    BinaryMessage message = messageBuilder()
        .buildRaw();
    IncrementCounterTx tx = new IncrementCounterTx(message);

    assertTrue(tx.isValid());
  }

  @Test
  public void executeIncrementsCounter() {
    try (Database db = new MemoryDb();
         Fork view = db.createFork()) {
      // Add a new counter with the given name and initial value
      String name = "counter";
      long initialValue = 0;
      createCounter(view, name, initialValue);


      // Create and execute the transaction
      HashCode nameHash = Hashing.defaultHashFunction().hashString(name, UTF_8);
      BinaryMessage message = messageBuilder()
          .setBody(bodyOf(1L, nameHash))
          .buildRaw();
      IncrementCounterTx tx = new IncrementCounterTx(message);
      tx.execute(view);

      // Check the counter has an incremented value
      QaSchema schema = new QaSchema(view);
      try (ProofMapIndexProxy<HashCode, Long> counters = schema.counters()) {
        long expectedValue = initialValue + 1;
        assertThat(counters.get(nameHash), equalTo(expectedValue));
      }
    }
  }

  @Test
  public void executeNoSuchCounter() {
    try (Database db = new MemoryDb();
         Fork view = db.createFork()) {
      // Create and execute the transaction that attempts to update an unknown counter
      String name = "unknown-counter";
      HashCode nameHash = Hashing.defaultHashFunction().hashString(name, UTF_8);
      BinaryMessage message = messageBuilder()
          .setBody(bodyOf(1L, nameHash))
          .buildRaw();
      IncrementCounterTx tx = new IncrementCounterTx(message);
      tx.execute(view);

      // Check there isn’t such a counter after tx execution
      QaSchema schema = new QaSchema(view);
      try (MapIndex<HashCode, Long> counters = schema.counters();
           MapIndex<HashCode, String> counterNames = schema.counterNames()) {
        assertFalse(counters.containsKey(nameHash));
        assertFalse(counterNames.containsKey(nameHash));
      }
    }
  }

  @Test
  public void info() {
    // Create a transaction with the given parameters.
    long seed = Long.MAX_VALUE - 1;
    String name = "new_counter";
    HashCode nameHash = Hashing.defaultHashFunction()
        .hashString(name, UTF_8);
    BinaryMessage message = messageBuilder()
        .setBody(bodyOf(seed, nameHash))
        .buildRaw();
    IncrementCounterTx tx = new IncrementCounterTx(message);

    String info = tx.info();

    // Check the transaction parameters in JSON
    Gson gson = new Gson();
    AnyTransaction txParameters = gson.fromJson(info, AnyTransaction.class);

    long actualSeed = Long.parseLong((String) txParameters.body.get("seed"), 16);
    assertThat(actualSeed, equalTo(seed));

    HashCode counterId = HashCode.fromString((String) txParameters.body.get("counter_id"));
    assertThat(counterId, equalTo(nameHash));
  }

  private static Message.Builder messageBuilder() {
    return new Message.Builder()
        .mergeFrom(INC_COUNTER_TX_MESSAGE_TEMPLATE);
  }

  private static ByteBuffer bodyOf(long seed, HashCode nameHash) {
    ByteBuffer body = ByteBuffer.allocate(IncrementCounterTx.BODY_SIZE)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putLong(seed)
        .put(nameHash.asBytes());
    body.rewind();
    return body;
  }
}
