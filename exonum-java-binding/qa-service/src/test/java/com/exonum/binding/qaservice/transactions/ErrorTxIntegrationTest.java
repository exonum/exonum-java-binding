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
import static com.exonum.binding.qaservice.TransactionUtils.createCounter;
import static com.exonum.binding.qaservice.TransactionUtils.newContext;
import static com.exonum.binding.qaservice.TransactionUtils.toTransactionMessage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.blockchain.TransactionResult;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.core.blockchain.Blockchain;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.CloseFailuresException;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.database.MemoryDb;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.transaction.RawTransaction;
import com.exonum.binding.core.transaction.Transaction;
import com.exonum.binding.core.transaction.TransactionContext;
import com.exonum.binding.core.transaction.TransactionExecutionException;
import com.exonum.binding.qaservice.QaSchema;
import com.exonum.binding.qaservice.QaService;
import com.exonum.binding.qaservice.QaServiceModule;
import com.exonum.binding.test.Bytes;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.testkit.TestKit;
import com.exonum.binding.testkit.TestKitExtension;
import com.google.gson.reflect.TypeToken;
import java.util.Optional;
import javax.annotation.Nullable;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ErrorTxIntegrationTest {

  @RegisterExtension
  TestKitExtension testKitExtension = new TestKitExtension(
      TestKit.builder()
          .withService(QaServiceModule.class));

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

    assertThat(txFromRaw).isEqualTo(tx);
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
  void executeNoDescription(TestKit testKit) {
    byte errorCode = 1;
    TransactionMessage errorTx = createErrorTransaction(0L, errorCode, null);
    testKit.createBlockWithTransactions(errorTx);

    Snapshot view = testKit.getSnapshot();
    Blockchain blockchain = Blockchain.newInstance(view);
    Optional<TransactionResult> txResult = blockchain.getTxResult(errorTx.hash());
    TransactionResult expectedTransactionResult = TransactionResult.error(errorCode, null);
    assertThat(txResult).hasValue(expectedTransactionResult);
  }

  @Test
  @RequiresNativeLibrary
  void executeWithDescription(TestKit testKit) {
    byte errorCode = 1;
    String errorDescription = "Test";
    TransactionMessage errorTx = createErrorTransaction(0L, errorCode, errorDescription);
    testKit.createBlockWithTransactions(errorTx);

    Snapshot view = testKit.getSnapshot();
    Blockchain blockchain = Blockchain.newInstance(view);
    Optional<TransactionResult> txResult = blockchain.getTxResult(errorTx.hash());
    TransactionResult expectedTransactionResult =
        TransactionResult.error(errorCode, errorDescription);
    assertThat(txResult).hasValue(expectedTransactionResult);
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
      ErrorTx tx = new ErrorTx(0L, errorCode, "Foo");

      // Execute the transaction
      TransactionContext context = newContext(view);
      assertThrows(TransactionExecutionException.class, () -> tx.execute(context));

      // Check that execute cleared the maps
      QaSchema schema = new QaSchema(view);

      assertThat(schema.counters().isEmpty()).isTrue();
      assertThat(schema.counterNames().isEmpty()).isTrue();
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

    assertThat(txFromJson.message_id).isEqualTo(QaTransaction.VALID_ERROR.id());
    assertThat(txFromJson.body).isEqualTo(tx);
  }

  @Test
  void equals() {
    EqualsVerifier.forClass(ErrorTx.class)
        .verify();
  }

  private TransactionMessage createErrorTransaction(
      long seed, byte errorCode, @Nullable String errorDescription) {
    ErrorTx errorTx = new ErrorTx(seed, errorCode, errorDescription);
    RawTransaction rawTransaction = errorTx.toRawTransaction();
    return toTransactionMessage(rawTransaction);
  }

  private static RawTransaction.Builder txTemplate() {
    return RawTransaction.newBuilder()
        .transactionId(QaTransaction.VALID_ERROR.id())
        .serviceId(QaService.ID)
        .payload(Bytes.bytes());
  }

}
