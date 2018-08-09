/*
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.qaservice.transactions;

import static com.exonum.binding.qaservice.transactions.CreateCounterTxIntegrationTest.createCounter;
import static com.exonum.binding.qaservice.transactions.IncrementCounterTx.serializeBody;
import static com.exonum.binding.qaservice.transactions.QaTransaction.INCREMENT_COUNTER;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.Hashing;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.qaservice.QaSchema;
import com.exonum.binding.qaservice.QaService;
import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.util.LibraryLoader;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class IncrementCounterTxIntegrationTest {

  static {
    LibraryLoader.load();
  }

  static Message MESSAGE_TEMPLATE = new Message.Builder()
      .mergeFrom(Transactions.QA_TX_MESSAGE_TEMPLATE)
      .setMessageType(INCREMENT_COUNTER.id())
      .setBody(serializeBody(new IncrementCounterTx(1, Hashing.sha256().hashInt(1))))
      .buildPartial();

  @Test
  void converterFromMessageRejectsWrongServiceId() {
    BinaryMessage message = messageBuilder()
        .setServiceId((short) (QaService.ID + 1))
        .buildRaw();

    assertThrows(IllegalArgumentException.class,
        () -> IncrementCounterTx.converter().fromMessage(message));
  }

  @Test
  void converterFromMessageRejectsWrongTxId() {
    BinaryMessage message = messageBuilder()
        .setMessageType((short) (INCREMENT_COUNTER.id() + 1))
        .buildRaw();

    assertThrows(IllegalArgumentException.class,
        () -> IncrementCounterTx.converter().fromMessage(message));
  }

  @Test
  void converterRoundtrip() {
    long seed = 0;
    HashCode counterId = Hashing.sha256().hashInt(0);

    IncrementCounterTx tx = new IncrementCounterTx(seed, counterId);
    BinaryMessage message = tx.getMessage();
    IncrementCounterTx txFromMessage = IncrementCounterTx.converter().fromMessage(message);

    assertThat(txFromMessage, equalTo(tx));
  }

  @Test
  void isValid() {
    long seed = 0;
    HashCode counterId = Hashing.sha256().hashInt(0);
    IncrementCounterTx tx = new IncrementCounterTx(seed, counterId);

    assertTrue(tx.isValid());
  }

  @Test
  void executeIncrementsCounter() throws CloseFailuresException {
    try (Database db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);

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
      ProofMapIndexProxy<HashCode, Long> counters = schema.counters();
      long expectedValue = initialValue + 1;
      assertThat(counters.get(nameHash), equalTo(expectedValue));
    }
  }

  @Test
  void executeNoSuchCounter() throws CloseFailuresException {
    try (Database db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);
      // Create and execute the transaction that attempts to update an unknown counter
      long seed = 0L;
      String name = "unknown-counter";
      HashCode nameHash = Hashing.defaultHashFunction().hashString(name, UTF_8);
      IncrementCounterTx tx = new IncrementCounterTx(seed, nameHash);
      tx.execute(view);

      // Check there isn’t such a counter after tx execution
      QaSchema schema = new QaSchema(view);
      MapIndex<HashCode, Long> counters = schema.counters();
      MapIndex<HashCode, String> counterNames = schema.counterNames();
      assertFalse(counters.containsKey(nameHash));
      assertFalse(counterNames.containsKey(nameHash));
    }
  }

  @Test
  void info() {
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
  void equals() {
    EqualsVerifier.forClass(IncrementCounterTx.class)
        .withPrefabValues(HashCode.class, HashCode.fromInt(1), HashCode.fromInt(2))
        .verify();
  }

  private static Message.Builder messageBuilder() {
    return new Message.Builder()
        .mergeFrom(MESSAGE_TEMPLATE);
  }
}
