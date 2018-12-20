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

import static com.exonum.binding.common.hash.Hashing.defaultHashFunction;
import static com.exonum.binding.common.serialization.json.JsonSerializer.json;
import static com.exonum.binding.qaservice.transactions.CreateCounterTx.converter;
import static com.exonum.binding.qaservice.transactions.QaTransaction.CREATE_COUNTER;
import static com.exonum.binding.qaservice.transactions.TestContextBuilder.newContext;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.qaservice.QaSchema;
import com.exonum.binding.qaservice.QaService;
import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.test.Bytes;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.transaction.TransactionContext;
import com.exonum.binding.util.LibraryLoader;
import com.google.gson.reflect.TypeToken;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class CreateCounterTxIntegrationTest {

  static {
    LibraryLoader.load();
  }

  @Test
  void converterRejectsWrongServiceId() {
    RawTransaction tx = txTemplate()
        .serviceId((short) (QaService.ID + 1))
        .build();

    assertThrows(IllegalArgumentException.class, () -> converter().fromRawTransaction(tx));
  }

  @Test
  void converterRejectsWrongTxId() {
    RawTransaction tx = txTemplate()
        .transactionId((short) (CREATE_COUNTER.id() + 1))
        .build();

    assertThrows(IllegalArgumentException.class, () -> converter().fromRawTransaction(tx));
  }

  @Test
  void converterRoundtrip() {
    String name = "counter";
    CreateCounterTx tx = new CreateCounterTx(name);

    RawTransaction raw = converter().toRawTransaction(tx);
    CreateCounterTx txFromMessage = converter().fromRawTransaction(raw);

    assertThat(txFromMessage, equalTo(tx));
  }

  @Test
  void rejectsEmptyName() {
    String name = "";

    Exception e = assertThrows(IllegalArgumentException.class,
        () -> new CreateCounterTx(name));
    assertThat(e.getMessage(), containsString("Name must not be blank"));
  }

  @Test
  @RequiresNativeLibrary
  void executeNewCounter() throws CloseFailuresException {
    String name = "counter";

    CreateCounterTx tx = new CreateCounterTx(name);

    try (Database db = MemoryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);

      // Execute the transaction
      TransactionContext context = spy(newContext(view).create());
      tx.execute(context);
      verify(context).getFork();

      // Check it has added entries in both maps.
      QaSchema schema = new QaSchema(view);
      MapIndex<HashCode, Long> counters = schema.counters();
      MapIndex<HashCode, String> counterNames = schema.counterNames();

      HashCode nameHash = defaultHashFunction()
          .hashString(name, UTF_8);

      assertThat(counters.get(nameHash), equalTo(0L));
      assertThat(counterNames.get(nameHash), equalTo(name));
    }
  }

  @Test
  @RequiresNativeLibrary
  void executeAlreadyExistingCounter() throws CloseFailuresException {
    try (Database db = MemoryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);

      String name = "counter";
      Long value = 100500L;
      HashCode nameHash = defaultHashFunction().hashString(name, UTF_8);

      // Add a counter with the given name and initial value to both indices manually.
      createCounter(view, name, value);

      // Execute the transaction, that has the same name.
      CreateCounterTx tx = new CreateCounterTx(name);
      TransactionContext context = spy(newContext(view).create());
      tx.execute(context);
      verify(context).getFork();

      // Check it has not changed the entries in the maps.
      QaSchema schema = new QaSchema(view);
      MapIndex<HashCode, String> counterNames = schema.counterNames();
      assertThat(counterNames.get(nameHash), equalTo(name));

      MapIndex<HashCode, Long> counters = schema.counters();
      assertThat(counters.get(nameHash), equalTo(value));
    }
  }

  @Test
  void info() {
    String name = "counter";
    CreateCounterTx tx = new CreateCounterTx(name);

    String info = tx.info();

    AnyTransaction<CreateCounterTx> txParams = json().fromJson(info,
        new TypeToken<AnyTransaction<CreateCounterTx>>(){}.getType()
    );
    assertThat(txParams.service_id, equalTo(QaService.ID));
    assertThat(txParams.message_id, equalTo(CREATE_COUNTER.id()));
    assertThat(txParams.body, equalTo(tx));
  }


  @Test
  void equals() {
    EqualsVerifier.forClass(CreateCounterTx.class)
        .verify();
  }

  /** Creates a counter in the storage with the given name and initial value. */
  static void createCounter(Fork view, String name, Long initialValue) {
    HashCode nameHash = defaultHashFunction().hashString(name, UTF_8);
    QaSchema schema = new QaSchema(view);
    MapIndex<HashCode, Long> counters = schema.counters();
    MapIndex<HashCode, String> counterNames = schema.counterNames();
    counters.put(nameHash, initialValue);
    counterNames.put(nameHash, name);
  }

  private static RawTransaction.Builder txTemplate() {
    return RawTransaction.newBuilder()
        .transactionId(CREATE_COUNTER.id())
        .serviceId(QaService.ID)
        .payload(Bytes.bytes());
  }
}
