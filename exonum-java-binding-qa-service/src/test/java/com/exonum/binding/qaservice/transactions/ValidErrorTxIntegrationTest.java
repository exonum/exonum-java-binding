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
import static com.exonum.binding.qaservice.transactions.QaTransaction.INCREMENT_COUNTER;
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

class ValidErrorTxIntegrationTest {

  @BeforeAll
  static void loadLibrary() {
    LibraryLoader.load();
  }

  @Test
  void converterFromMessageRejectsWrongServiceId() {
    RawTransaction tx = txTemplate()
        .serviceId((short) (QaService.ID + 1))
        .build();

    assertThrows(IllegalArgumentException.class,
        () -> IncrementCounterTx.converter().fromRawTransaction(tx));
  }

  @Test
  void converterFromMessageRejectsWrongTxId() {
    RawTransaction tx = txTemplate()
        .transactionId((short) (INCREMENT_COUNTER.id() + 1))
        .build();

    assertThrows(IllegalArgumentException.class,
        () -> IncrementCounterTx.converter().fromRawTransaction(tx));
  }

  @Test
  void converterRoundtrip() {
    ValidErrorTx tx = new ValidErrorTx(1L, (byte) 2, "Foo");

    RawTransaction raw = ValidErrorTx.converter().toRawTransaction(tx);
    ValidErrorTx txFromMessage = ValidErrorTx.converter().fromRawTransaction(raw);

    assertThat(txFromMessage, equalTo(tx));
  }

  @Test
  void constructorRejectsInvalidErrorCode() {
    byte invalidErrorCode = -1;
    assertThrows(IllegalArgumentException.class,
        () -> new ValidErrorTx(1L, invalidErrorCode, "Boom"));
  }

  @Test
  void constructorRejectsInvalidDescription() {
    String invalidDescription = "";
    assertThrows(IllegalArgumentException.class,
        () -> new ValidErrorTx(1L, (byte) 1, invalidDescription));
  }

  @Test
  @RequiresNativeLibrary
  void executeNoDescription() throws CloseFailuresException {
    try (MemoryDb db = MemoryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      TransactionContext context = new QaContext(db.createFork(cleaner));

      byte errorCode = 2;
      Transaction tx = new ValidErrorTx(1L, errorCode, null);

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
      TransactionContext context = new QaContext(db.createFork(cleaner));

      byte errorCode = 2;
      String description = "Boom";
      Transaction tx = new ValidErrorTx(1L, errorCode, description);

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
      TransactionContext context = new QaContext(view);
      // Initialize storage with a counter equal to 10
      String name = "counter";
      long value = 10L;
      createCounter(view, name, value);

      // Create the transaction
      byte errorCode = 1;
      ValidErrorTx tx = new ValidErrorTx(0L, errorCode, "Boom");

      // Execute the transaction
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
    Transaction tx = new ValidErrorTx(seed, errorCode, errorMessage);

    String txInJson = QaTransactionJson.toJson(ValidErrorTx.ID, tx);

    AnyTransaction<ValidErrorTx> txFromJson = json().fromJson(txInJson,
        new TypeToken<AnyTransaction<ValidErrorTx>>() {
        }.getType());

    assertThat(txFromJson.message_id, equalTo(QaTransaction.VALID_ERROR.id()));
    assertThat(txFromJson.body, equalTo(tx));
  }

  @Test
  void equals() {
    EqualsVerifier.forClass(ValidErrorTx.class)
        .verify();
  }

  private static RawTransaction.Builder txTemplate() {
    return RawTransaction.newBuilder()
        .transactionId(ValidErrorTx.ID)
        .serviceId(QaService.ID)
        .payload(Bytes.bytes());
  }

}
