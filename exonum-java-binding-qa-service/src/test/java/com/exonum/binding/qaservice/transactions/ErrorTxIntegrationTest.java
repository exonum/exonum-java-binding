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

import static com.exonum.binding.common.serialization.json.JsonSerializer.json;
import static com.exonum.binding.qaservice.transactions.CreateCounterTxIntegrationTest.createCounter;
import static com.exonum.binding.qaservice.transactions.TestContextBuilder.newContext;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.qaservice.QaSchema;
import com.exonum.binding.qaservice.QaService;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.test.Bytes;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.transaction.Transaction;
import com.exonum.binding.transaction.TransactionContext;
import com.exonum.binding.transaction.TransactionExecutionException;
import com.exonum.binding.util.LibraryLoader;
import com.google.gson.reflect.TypeToken;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ErrorTxIntegrationTest {

  @BeforeAll
  static void loadLibrary() {
    LibraryLoader.load();
  }

  @Test
  void converterRejectsWrongServiceId() {
    RawTransaction tx = txTemplate()
        .serviceId((short) -1)
        .build();

    assertThrows(IllegalArgumentException.class,
        () -> IncrementCounterTx.converter().fromRawTransaction(tx));
  }

  @Test
  void converterRejectsWrongTxId() {
    RawTransaction tx = txTemplate()
        .transactionId((short) -1)
        .build();

    assertThrows(IllegalArgumentException.class,
        () -> IncrementCounterTx.converter().fromRawTransaction(tx));
  }

  @Test
  void converterRoundtrip() {
    ErrorTx tx = new ErrorTx(1L, (byte) 2, "Foo");

    RawTransaction raw = ErrorTx.converter().toRawTransaction(tx);
    ErrorTx txFromRaw = ErrorTx.converter().fromRawTransaction(raw);

    assertThat(txFromRaw, equalTo(tx));
  }

  @Test
  void constructorRejectsInvalidErrorCode() {
    byte invalidErrorCode = -1;
    assertThrows(IllegalArgumentException.class,
        () -> new ErrorTx(1L, invalidErrorCode, "Boom"));
  }

  @Test
  void constructorRejectsInvalidDescription() {
    String invalidDescription = "";
    assertThrows(IllegalArgumentException.class,
        () -> new ErrorTx(1L, (byte) 1, invalidDescription));
  }

  @Test
  @RequiresNativeLibrary
  void executeNoDescription() throws CloseFailuresException {
    try (MemoryDb db = MemoryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);

      byte errorCode = 2;
      Transaction tx = new ErrorTx(1L, errorCode, null);

      TransactionContext context = newContext(view).create();
      TransactionExecutionException expected = assertThrows(TransactionExecutionException.class,
          () -> tx.execute(context));

      assertThat(expected.getErrorCode(), equalTo(errorCode));
      assertNull(expected.getMessage());
    }
  }

  @Test
  @RequiresNativeLibrary
  void executeWithDescription() throws CloseFailuresException {
    try (MemoryDb db = MemoryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);

      byte errorCode = 2;
      String description = "Boom";
      Transaction tx = new ErrorTx(1L, errorCode, description);

      TransactionContext context = newContext(view).create();
      TransactionExecutionException expected = assertThrows(TransactionExecutionException.class,
          () -> tx.execute(context));

      assertThat(expected.getErrorCode(), equalTo(errorCode));
      assertThat(expected.getMessage(), equalTo(description));
    }
  }

  @Test
  @RequiresNativeLibrary
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
      ErrorTx tx = new ErrorTx(0L, errorCode, "Boom");

      // Execute the transaction
      TransactionContext context = newContext(view).create();
      assertThrows(TransactionExecutionException.class, () -> tx.execute(context));

      // Check that execute cleared the maps
      QaSchema schema = new QaSchema(view);

      assertTrue(schema.counters().isEmpty());
      assertTrue(schema.counterNames().isEmpty());
    }
  }

  @CsvSource({
      "1, 0, Boom", // min error code value
      "-1, 1, 'Longer error message'",
      "9223372036854775807, 127,", // Max seed value, max error code value, null message
  })
  @ParameterizedTest
  void info(long seed, byte errorCode, String errorMessage) {
    Transaction tx = new ErrorTx(seed, errorCode, errorMessage);

    String txInJson = tx.info();

    AnyTransaction<ErrorTx> txFromJson = json().fromJson(txInJson,
        new TypeToken<AnyTransaction<ErrorTx>>(){}.getType());

    assertThat(txFromJson.message_id, equalTo(QaTransaction.VALID_ERROR.id()));
    assertThat(txFromJson.body, equalTo(tx));
  }

  @Test
  void equals() {
    EqualsVerifier.forClass(ErrorTx.class)
        .verify();
  }

  private static RawTransaction.Builder txTemplate() {
    return RawTransaction.newBuilder()
        .transactionId(QaTransaction.VALID_ERROR.id())
        .serviceId(QaService.ID)
        .payload(Bytes.bytes());
  }

}
