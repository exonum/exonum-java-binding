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

package com.exonum.binding.qaservice;

import static com.exonum.binding.common.blockchain.ExecutionStatuses.serviceError;
import static com.exonum.binding.qaservice.QaArtifactInfo.ARTIFACT_ID;
import static com.exonum.binding.qaservice.QaArtifactInfo.QA_SERVICE_ID;
import static com.exonum.binding.qaservice.QaArtifactInfo.QA_SERVICE_NAME;
import static com.exonum.binding.qaservice.QaArtifactInfo.createQaServiceTestkit;
import static com.exonum.binding.qaservice.TransactionUtils.createCounter;
import static com.exonum.binding.qaservice.TransactionUtils.newContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.core.blockchain.Blockchain;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.CloseFailuresException;
import com.exonum.binding.core.runtime.ServiceInstanceSpec;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.storage.database.TemporaryDb;
import com.exonum.binding.core.transaction.TransactionContext;
import com.exonum.binding.core.transaction.TransactionExecutionException;
import com.exonum.binding.qaservice.transactions.TxMessageProtos.ErrorTxBody;
import com.exonum.binding.testkit.TestKit;
import com.exonum.binding.testkit.TestKitExtension;
import com.exonum.core.messages.Runtime.ErrorKind;
import com.exonum.core.messages.Runtime.ExecutionError;
import com.exonum.core.messages.Runtime.ExecutionStatus;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@Integration
class ErrorTxTest {

  @RegisterExtension
  TestKitExtension testKitExtension = new TestKitExtension(
      createQaServiceTestkit());

  @ParameterizedTest
  @ValueSource(ints = {Integer.MIN_VALUE, -2, -1, 128, Integer.MAX_VALUE})
  void executeThrowsIfInvalidErrorCode(int errorCode, TestKit testKit) {
    TransactionMessage errorTx = createErrorTransaction(errorCode, null);
    testKit.createBlockWithTransactions(errorTx);

    Snapshot view = testKit.getSnapshot();
    Blockchain blockchain = Blockchain.newInstance(view);
    ExecutionStatus txResult = blockchain.getTxResult(errorTx.hash()).get();
    assertTrue(txResult.hasError());
    ExecutionError error = txResult.getError();
    assertThat(error.getKind())
        .as("actual=%s", error)
        .isEqualTo(ErrorKind.RUNTIME);
    assertThat(error.getDescription()).contains(Integer.toString(errorCode));
  }

  @Test
  @Disabled("ECR-4014")
  void executeNoDescription(TestKit testKit) {
    byte errorCode = 1;
    TransactionMessage errorTx = createErrorTransaction(errorCode, null);
    testKit.createBlockWithTransactions(errorTx);

    Snapshot view = testKit.getSnapshot();
    Blockchain blockchain = Blockchain.newInstance(view);
    Optional<ExecutionStatus> txResult = blockchain.getTxResult(errorTx.hash());
    ExecutionStatus expectedTransactionResult = serviceError(errorCode);
    assertThat(txResult).hasValue(expectedTransactionResult);
  }

  @ParameterizedTest
  @CsvSource({
      "0, ''",
      "1, 'Non-empty description'",
      "127, 'Max error code: 127'",
  })
  @Disabled("ECR-4014")
  void executeWithDescription(byte errorCode, String errorDescription, TestKit testKit) {
    TransactionMessage errorTx = createErrorTransaction(errorCode, errorDescription);
    testKit.createBlockWithTransactions(errorTx);

    Snapshot view = testKit.getSnapshot();
    Blockchain blockchain = Blockchain.newInstance(view);
    Optional<ExecutionStatus> txResult = blockchain.getTxResult(errorTx.hash());
    ExecutionStatus expectedTransactionResult = serviceError(errorCode, errorDescription);
    assertThat(txResult).hasValue(expectedTransactionResult);
  }

  @Test
  void executeClearsQaServiceData() throws CloseFailuresException {
    try (TemporaryDb db = TemporaryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);
      QaSchema schema = new QaSchema(view, QA_SERVICE_NAME);

      // Initialize storage with a counter equal to 10
      String name = "counter";
      long value = 10L;
      createCounter(schema, name, value);

      QaServiceImpl qaService = new QaServiceImpl(
          ServiceInstanceSpec.newInstance(QA_SERVICE_NAME, QA_SERVICE_ID, ARTIFACT_ID));
      // Create the transaction arguments
      ErrorTxBody arguments = ErrorTxBody.newBuilder()
          .setErrorCode(1)
          .setErrorDescription("Foo")
          .build();
      TransactionContext context = newContext(view)
          .serviceName(QA_SERVICE_NAME)
          .serviceId(QA_SERVICE_ID)
          .build();
      // Invoke the transaction
      assertThrows(TransactionExecutionException.class, () -> qaService.error(arguments, context));

      // Check that it has cleared the maps
      assertThat(schema.counters().isEmpty()).isTrue();
      assertThat(schema.counterNames().isEmpty()).isTrue();
    }
  }

  private static TransactionMessage createErrorTransaction(int errorCode,
      @Nullable String errorDescription) {
    return TransactionMessages.createErrorTx(errorCode, errorDescription, QA_SERVICE_ID);
  }
}
