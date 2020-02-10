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

import static com.exonum.binding.core.runtime.RuntimeId.JAVA;
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
import com.exonum.binding.core.blockchain.BlockchainData;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.CloseFailuresException;
import com.exonum.binding.core.runtime.ServiceInstanceSpec;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.storage.database.TemporaryDb;
import com.exonum.binding.core.transaction.ExecutionException;
import com.exonum.binding.core.transaction.TransactionContext;
import com.exonum.binding.qaservice.transactions.TxMessageProtos.ErrorTxBody;
import com.exonum.binding.testkit.TestKit;
import com.exonum.binding.testkit.TestKitExtension;
import com.exonum.messages.core.runtime.Errors.ErrorKind;
import com.exonum.messages.core.runtime.Errors.ExecutionError;
import com.exonum.messages.core.runtime.Errors.ExecutionStatus;
import java.util.Optional;
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
    TransactionMessage errorTx = createErrorTransaction(errorCode, "");
    testKit.createBlockWithTransactions(errorTx);

    Snapshot snapshot = testKit.getSnapshot();
    Blockchain blockchain = Blockchain.newInstance(snapshot);
    ExecutionStatus txResult = blockchain.getTxResult(errorTx.hash()).get();
    assertTrue(txResult.hasError());
    ExecutionError error = txResult.getError();
    assertThat(error.getKind())
        .as("actual=%s", error)
        .isEqualTo(ErrorKind.UNEXPECTED);
    assertThat(error.getDescription()).contains(Integer.toString(errorCode));
  }

  @ParameterizedTest
  @CsvSource({
      "0, ''",
      "1, 'Non-empty description'",
      "127, 'Max error code: 127'",
  })
  void executeErrorTx(byte errorCode, String errorDescription, TestKit testKit) {
    TransactionMessage errorTx = createErrorTransaction(errorCode, errorDescription);
    testKit.createBlockWithTransactions(errorTx);

    Snapshot snapshot = testKit.getSnapshot();
    Blockchain blockchain = Blockchain.newInstance(snapshot);
    Optional<ExecutionStatus> txResultOpt = blockchain.getTxResult(errorTx.hash());
    assertThat(txResultOpt).hasValueSatisfying(status -> {
      assertTrue(status.hasError());

      ExecutionError error = status.getError();
      // Verify only the properties EJB is responsible for
      assertThat(error.getKind()).isEqualTo(ErrorKind.SERVICE);
      assertThat(error.getCode()).isEqualTo(errorCode);
      assertThat(error.getDescription()).isEqualTo(errorDescription);
      assertThat(error.getRuntimeId()).isEqualTo(JAVA.getId());
    });
  }

  @Test
  void executeClearsQaServiceData() throws CloseFailuresException {
    try (TemporaryDb db = TemporaryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      Fork fork = db.createFork(cleaner);
      BlockchainData blockchainData = BlockchainData.fromRawAccess(fork, QA_SERVICE_NAME);
      QaSchema schema = new QaSchema(blockchainData);

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
      TransactionContext context = newContext(blockchainData)
          .serviceName(QA_SERVICE_NAME)
          .serviceId(QA_SERVICE_ID)
          .build();
      // Invoke the transaction
      assertThrows(ExecutionException.class, () -> qaService.error(arguments, context));

      // Check that it has cleared the maps
      assertTrue(schema.counters().isEmpty());
    }
  }

  private static TransactionMessage createErrorTransaction(int errorCode,
      String errorDescription) {
    return TransactionMessages.createErrorTx(errorCode, errorDescription, QA_SERVICE_ID);
  }
}
