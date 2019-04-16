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
import static com.exonum.binding.common.hash.Hashing.sha256;
import static com.exonum.binding.common.serialization.json.JsonSerializer.json;
import static com.exonum.binding.qaservice.transactions.ContextUtils.newContext;
import static com.exonum.binding.qaservice.transactions.CreateCounterTxIntegrationTest.createCounter;
import static com.exonum.binding.qaservice.transactions.IncrementCounterTx.converter;
import static com.exonum.binding.qaservice.transactions.QaTransaction.INCREMENT_COUNTER;
import static com.exonum.binding.qaservice.transactions.TransactionError.UNKNOWN_COUNTER;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.qaservice.QaSchema;
import com.exonum.binding.qaservice.QaService;
import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.test.Bytes;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.transaction.TransactionContext;
import com.exonum.binding.transaction.TransactionExecutionException;
import com.exonum.binding.util.LibraryLoader;
import com.google.gson.reflect.TypeToken;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class IncrementCounterTxIntegrationTest {

  static {
    LibraryLoader.load();
  }

  @Test
  void converterRejectsWrongServiceId() {
    RawTransaction tx = txTemplate()
        .serviceId((short) -1)
        .build();

    assertThrows(IllegalArgumentException.class,
        () -> converter().fromRawTransaction(tx));
  }

  @Test
  void converterRejectsWrongTxId() {
    RawTransaction tx = txTemplate()
        .transactionId((short) -1)
        .build();

    assertThrows(IllegalArgumentException.class,
        () -> converter().fromRawTransaction(tx));
  }

  @Test
  void converterRoundtrip() {
    long seed = 0;
    HashCode counterId = sha256().hashInt(0);

    IncrementCounterTx tx = new IncrementCounterTx(seed, counterId);
    RawTransaction raw = converter().toRawTransaction(tx);
    IncrementCounterTx txFromRaw = converter().fromRawTransaction(raw);

    assertThat(txFromRaw, equalTo(tx));
  }

  @Test
  @RequiresNativeLibrary
  void executeIncrementsCounter() throws Exception {
    try (Database db = MemoryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);

      // Add a new counter with the given name and initial value
      String name = "counter";
      long initialValue = 0;
      createCounter(view, name, initialValue);

      // Create and execute the transaction
      long seed = 0L;
      HashCode nameHash = defaultHashFunction().hashString(name, UTF_8);
      IncrementCounterTx tx = new IncrementCounterTx(seed, nameHash);

      // Execute the transaction
      TransactionContext context = newContext(view);
      tx.execute(context);

      // Check the counter has an incremented value
      QaSchema schema = new QaSchema(view);
      ProofMapIndexProxy<HashCode, Long> counters = schema.counters();
      long expectedValue = initialValue + 1;
      assertThat(counters.get(nameHash), equalTo(expectedValue));
    }
  }

  @Test
  @RequiresNativeLibrary
  void executeNoSuchCounter() throws CloseFailuresException {
    try (Database db = MemoryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);

      // Create and execute the transaction that attempts to update an unknown counter
      long seed = 0L;
      String name = "unknown-counter";
      HashCode nameHash = defaultHashFunction().hashString(name, UTF_8);

      // Execute the transaction
      IncrementCounterTx tx = new IncrementCounterTx(seed, nameHash);
      TransactionContext context = newContext(view);
      TransactionExecutionException e = assertThrows(TransactionExecutionException.class,
          () -> tx.execute(context));
      assertThat(e.getErrorCode(), equalTo(UNKNOWN_COUNTER.code));
    }
  }

  @Test
  void info() {
    // Create a transaction with the given parameters.
    long seed = Long.MAX_VALUE - 1;
    String name = "new_counter";
    HashCode nameHash = defaultHashFunction().hashString(name, UTF_8);
    IncrementCounterTx tx = new IncrementCounterTx(seed, nameHash);

    String info = tx.info();

    // Check the transaction parameters in JSON
    AnyTransaction<IncrementCounterTx> txParameters = json().fromJson(info,
        new TypeToken<AnyTransaction<IncrementCounterTx>>(){}.getType());

    assertThat(txParameters.body, equalTo(tx));
  }

  @Test
  void equals() {
    EqualsVerifier.forClass(IncrementCounterTx.class)
        .withPrefabValues(HashCode.class, HashCode.fromInt(1), HashCode.fromInt(2))
        .verify();
  }

  private static RawTransaction.Builder txTemplate() {
    return RawTransaction.newBuilder()
        .transactionId(INCREMENT_COUNTER.id())
        .serviceId(QaService.ID)
        .payload(Bytes.bytes());
  }
}
