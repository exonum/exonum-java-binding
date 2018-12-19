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
import static com.exonum.binding.qaservice.transactions.CreateCounterTxIntegrationTest.createCounter;
import static com.exonum.binding.qaservice.transactions.IncrementCounterTx.converter;
import static com.exonum.binding.qaservice.transactions.QaTransaction.INCREMENT_COUNTER;
import static com.exonum.binding.qaservice.transactions.TestContextBuilder.newContext;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.test.Bytes;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.transaction.TransactionContext;
import com.exonum.binding.util.LibraryLoader;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class IncrementCounterTxIntegrationTest {

  static {
    LibraryLoader.load();
  }

  @Test
  void converterRejectsWrongServiceId() {
    RawTransaction tx = txTemplate()
        .serviceId((short) (QaService.ID + 1))
        .build();

    assertThrows(IllegalArgumentException.class,
        () -> converter().fromRawTransaction(tx));
  }

  @Test
  void converterRejectsWrongTxId() {
    RawTransaction tx = txTemplate()
        .transactionId((short) (INCREMENT_COUNTER.id() + 1))
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
    IncrementCounterTx txFromMessage = converter().fromRawTransaction(raw);

    assertThat(txFromMessage, equalTo(tx));
  }

  @Test
  @RequiresNativeLibrary
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
      HashCode nameHash = defaultHashFunction().hashString(name, UTF_8);
      IncrementCounterTx tx = new IncrementCounterTx(seed, nameHash);

      // Execute the transaction
      TransactionContext context = spy(newContext(view).create());
      tx.execute(context);
      verify(context).getFork();

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
      IncrementCounterTx tx = new IncrementCounterTx(seed, nameHash);

      // Execute the transaction
      TransactionContext context = spy(newContext(view).create());
      tx.execute(context);
      verify(context).getFork();

      // Check there isn’t such a counter after tx execution
      QaSchema schema = new QaSchema(view);
      MapIndex<HashCode, Long> counters = schema.counters();
      MapIndex<HashCode, String> counterNames = schema.counterNames();
      assertFalse(counters.containsKey(nameHash));
      assertFalse(counterNames.containsKey(nameHash));
    }
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
