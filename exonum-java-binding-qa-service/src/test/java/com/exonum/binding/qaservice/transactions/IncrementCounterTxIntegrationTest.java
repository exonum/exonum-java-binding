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
import com.exonum.binding.qaservice.QaService;
import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.ForkProxy;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.util.LibraryLoader;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import java.nio.ByteBuffer;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class IncrementCounterTxIntegrationTest {

  static {
    LibraryLoader.load();
  }

  static Message INC_COUNTER_TX_MESSAGE_TEMPLATE = new Message.Builder()
      .mergeFrom(Transactions.QA_TX_MESSAGE_TEMPLATE)
      .setMessageType(INCREMENT_COUNTER.id())
      .setBody(ByteBuffer.allocate(IncrementCounterTx.BODY_SIZE))
      .buildPartial();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void converterFromMessageRejectsWrongServiceId() {
    BinaryMessage message = messageBuilder()
        .setServiceId((short) (QaService.ID + 1))
        .buildRaw();

    expectedException.expect(IllegalArgumentException.class);
    IncrementCounterTx.converter().fromMessage(message);
  }

  @Test
  public void converterFromMessageRejectsWrongTxId() {
    BinaryMessage message = messageBuilder()
        .setMessageType((short) (INCREMENT_COUNTER.id() + 1))
        .buildRaw();

    expectedException.expect(IllegalArgumentException.class);
    IncrementCounterTx.converter().fromMessage(message);
  }

  @Test
  public void converterRoundtrip() {
    long seed = 0;
    HashCode counterId = Hashing.sha256().hashInt(0);

    IncrementCounterTx tx = new IncrementCounterTx(seed, counterId);
    BinaryMessage message = tx.getMessage();
    IncrementCounterTx txFromMessage = IncrementCounterTx.converter().fromMessage(message);

    assertThat(txFromMessage, equalTo(tx));
  }

  @Test
  public void isValid() {
    long seed = 0;
    HashCode counterId = Hashing.sha256().hashInt(0);
    IncrementCounterTx tx = new IncrementCounterTx(seed, counterId);

    assertTrue(tx.isValid());
  }

  @Test
  public void executeIncrementsCounter() {
    try (Database db = new MemoryDb();
         ForkProxy view = db.createFork()) {
      // Add a new counter with the given name and initial value
      String name = "counter";
      long initialValue = 0;
      createCounter(view, name, initialValue);

      // Create and execute the transaction
      long seed = 0L;
      HashCode nameHash = Hashing.defaultHashFunction().hashString(name, UTF_8);
      IncrementCounterTx tx = new IncrementCounterTx(seed, nameHash);
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
         ForkProxy view = db.createFork()) {
      // Create and execute the transaction that attempts to update an unknown counter
      long seed = 0L;
      String name = "unknown-counter";
      HashCode nameHash = Hashing.defaultHashFunction().hashString(name, UTF_8);
      IncrementCounterTx tx = new IncrementCounterTx(seed, nameHash);
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
    IncrementCounterTx tx = new IncrementCounterTx(seed, nameHash);

    String info = tx.info();

    // Check the transaction parameters in JSON
    Gson gson = QaTransactionGson.instance();

    AnyTransaction<IncrementCounterTx> txParameters = gson.fromJson(info,
        new TypeToken<AnyTransaction<IncrementCounterTx>>(){}.getType());

    assertThat(txParameters.body, equalTo(tx));
  }

  @Test
  public void equals() {
    EqualsVerifier.forClass(IncrementCounterTx.class)
        .withPrefabValues(HashCode.class, HashCode.fromInt(1), HashCode.fromInt(2))
        .verify();
  }

  private static Message.Builder messageBuilder() {
    return new Message.Builder()
        .mergeFrom(INC_COUNTER_TX_MESSAGE_TEMPLATE);
  }
}
