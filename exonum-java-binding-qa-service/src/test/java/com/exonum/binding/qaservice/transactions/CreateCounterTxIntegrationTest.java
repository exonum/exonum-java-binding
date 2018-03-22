package com.exonum.binding.qaservice.transactions;

import static com.exonum.binding.qaservice.transactions.QaTransaction.CREATE_COUNTER;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.Hashing;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.qaservice.QaSchema;
import com.exonum.binding.qaservice.QaService;
import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.storage.serialization.StandardSerializers;
import com.exonum.binding.util.LibraryLoader;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.nio.ByteBuffer;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class CreateCounterTxIntegrationTest {

  static {
    LibraryLoader.load();
  }

  @Rule
  public final ExpectedException expectedException = ExpectedException.none();

  static final Message CREATE_COUNTER_MESSAGE_TEMPLATE = new Message.Builder()
      .mergeFrom(Transactions.QA_TX_MESSAGE_TEMPLATE)
      .setMessageType(CREATE_COUNTER.id)
      .setBody(serialize("counter"))
      .buildPartial();

  @Test
  public void converterFromMessageRejectsWrongServiceId() {
    BinaryMessage message = messageBuilder()
        .setServiceId((short) (QaService.ID + 1))
        .buildRaw();

    expectedException.expect(IllegalArgumentException.class);
    CreateCounterTx.converter().fromMessage(message);
  }

  @Test
  public void converterFromMessageRejectsWrongTxId() {
    BinaryMessage message = messageBuilder()
        .setMessageType((short) (CREATE_COUNTER.id + 1))
        .buildRaw();

    expectedException.expect(IllegalArgumentException.class);
    CreateCounterTx.converter().fromMessage(message);
  }

  @Test
  public void converterToMessage() {
    String name = "counter";
    CreateCounterTx tx = new CreateCounterTx(name);

    BinaryMessage expectedMessage = messageBuilder()
        .setMessageType(CREATE_COUNTER.id)
        .setBody(serialize(name))
        .buildRaw();

    // todo: remove extra #getMessage when MessageReader has equals: ECR-992
    assertThat(tx.getMessage().getMessage(), equalTo(expectedMessage.getMessage()));
  }

  @Test
  public void converterRoundtrip() {
    String name = "counter";
    CreateCounterTx tx = new CreateCounterTx(name);

    BinaryMessage message = tx.getMessage();

    CreateCounterTx txFromMessage = CreateCounterTx.converter().fromMessage(message);

    assertThat(txFromMessage, equalTo(tx));
  }

  @Test
  public void rejectsEmptyName() {
    String name = "";

    expectedException.expectMessage("Name must not be blank");
    expectedException.expect(IllegalArgumentException.class);
    new CreateCounterTx(name);
  }

  @Test
  public void isValid() {
    String name = "counter";

    CreateCounterTx tx = new CreateCounterTx(name);
    assertTrue(tx.isValid());
  }

  @Test
  public void executeNewCounter() {
    String name = "counter";

    CreateCounterTx tx = new CreateCounterTx(name);

    try (Database db = new MemoryDb();
         Fork view = db.createFork()) {
      // Execute the transaction
      tx.execute(view);

      // Check it has added entries in both maps.
      QaSchema schema = new QaSchema(view);
      try (MapIndex<HashCode, Long> counters = schema.counters();
           MapIndex<HashCode, String> counterNames = schema.counterNames()) {

        HashCode nameHash = Hashing.defaultHashFunction()
            .hashString(name, UTF_8);

        assertThat(counters.get(nameHash), equalTo(0L));
        assertThat(counterNames.get(nameHash), equalTo(name));
      }
    }
  }

  @Test
  public void executeAlreadyExistingCounter() {
    try (Database db = new MemoryDb();
         Fork view = db.createFork()) {
      String name = "counter";
      Long value = 100500L;
      HashCode nameHash = Hashing.defaultHashFunction()
          .hashString(name, UTF_8);

      // Add a counter with the given name and initial value to both indices manually.
      createCounter(view, name, value);

      // Execute the transaction, that has the same name.
      CreateCounterTx tx = new CreateCounterTx(name);
      tx.execute(view);

      // Check it has not changed the entries in the maps.
      QaSchema schema = new QaSchema(view);
      try (MapIndex<HashCode, Long> counters = schema.counters();
           MapIndex<HashCode, String> counterNames = schema.counterNames()) {
        assertThat(counters.get(nameHash), equalTo(value));
        assertThat(counterNames.get(nameHash), equalTo(name));
      }
    }
  }

  @Test
  public void info() {
    String name = "counter";
    CreateCounterTx tx = new CreateCounterTx(name);

    String info = tx.info();

    Gson gson = QaTransactionGson.instance();
    AnyTransaction<CreateCounterTx> txParams = gson.fromJson(info,
        new TypeToken<AnyTransaction<CreateCounterTx>>(){}.getType()
    );
    assertThat(txParams.service_id, equalTo(QaService.ID));
    assertThat(txParams.message_id, equalTo(CREATE_COUNTER.id));
    assertThat(txParams.body, equalTo(tx));
  }

  @Test
  public void equals() {
    EqualsVerifier.forClass(CreateCounterTx.class)
        .verify();
  }

  /** Creates a builder of create counter transaction message. */
  private static Message.Builder messageBuilder() {
    return new Message.Builder()
        .mergeFrom(CREATE_COUNTER_MESSAGE_TEMPLATE);
  }

  private static ByteBuffer serialize(String txName) {
    return ByteBuffer.wrap(StandardSerializers.string().toBytes(txName));
  }

  /** Creates a counter in the storage with the given name and initial value. */
  static void createCounter(Fork view, String name, Long initialValue) {
    HashCode nameHash = Hashing.defaultHashFunction().hashString(name, UTF_8);
    QaSchema schema = new QaSchema(view);
    try (MapIndex<HashCode, Long> counters = schema.counters();
         MapIndex<HashCode, String> counterNames = schema.counterNames()) {
      counters.put(nameHash, initialValue);
      counterNames.put(nameHash, name);
    }
  }
}
