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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.messages.TransactionExecutionException;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.qaservice.QaSchema;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.util.LibraryLoader;
import org.junit.jupiter.api.Test;

class ValidErrorTxIntegrationTest {

  static {
    LibraryLoader.load();
  }

  @Test
  void executeClearsQaServiceData() throws CloseFailuresException {
    try (MemoryDb db = MemoryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);

      // Initialize storage with a counter equal to 10
      String name = "counter";
      long value = 10L;
      createCounter(view, name, value);

      // Create the transaction
      byte errorCode = 1;
      ValidErrorTx tx = new ValidErrorTx(0L, errorCode, "Boom");

      // Execute the transaction
      assertThrows(TransactionExecutionException.class, () -> tx.execute(view));

      // Check that execute cleared the maps
      QaSchema schema = new QaSchema(view);
      checkIsEmpty(schema.counters());
      checkIsEmpty(schema.counterNames());
    }
  }

  private static <K,V> void checkIsEmpty(MapIndex<K, V> map) {
    assertFalse(map.keys().hasNext());
  }
}
