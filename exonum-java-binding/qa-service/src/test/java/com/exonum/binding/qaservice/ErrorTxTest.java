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
import static com.exonum.binding.qaservice.QaArtifactInfo.QA_SERVICE_ID;
import static com.exonum.binding.qaservice.QaArtifactInfo.QA_SERVICE_NAME;
import static com.exonum.binding.qaservice.QaArtifactInfo.createQaServiceTestkit;
import static com.exonum.binding.qaservice.TransactionUtils.createCounter;
import static com.exonum.binding.qaservice.TransactionUtils.newContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.core.blockchain.Blockchain;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.CloseFailuresException;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.storage.database.TemporaryDb;
import com.exonum.binding.core.transaction.TransactionContext;
import com.exonum.binding.core.transaction.TransactionExecutionException;
import com.exonum.binding.qaservice.Integration;
import com.exonum.binding.qaservice.QaSchema;
import com.exonum.binding.qaservice.TransactionMessages;
import com.exonum.binding.testkit.TestKit;
import com.exonum.binding.testkit.TestKitExtension;
import com.exonum.core.messages.Runtime.ExecutionStatus;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@Integration
class ErrorTxTest {

  @RegisterExtension
  TestKitExtension testKitExtension = new TestKitExtension(
      createQaServiceTestkit());

  /*
  Review: constructorRejectsInvalidErrorCode and InvalidDescription as tx tests.
   */
  @Test
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

  @Test
  void executeWithDescription(TestKit testKit) {
    byte errorCode = 1;
    String errorDescription = "Test";
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

      // TODO: refactor this test
      // Create the transaction
//      byte errorCode = 1;
//      ErrorTx tx = new ErrorTx(0L, errorCode, "Foo");
//
//      // Execute the transaction
//      TransactionContext context = newContext(view)
//          .serviceId(QA_SERVICE_ID)
//          .serviceName(QA_SERVICE_NAME)
//          .build();
//      assertThrows(TransactionExecutionException.class, () -> tx.execute(context));
//
//      // Check that execute cleared the maps
//      assertThat(schema.counters().isEmpty()).isTrue();
//      assertThat(schema.counterNames().isEmpty()).isTrue();
    }
  }

  private static TransactionMessage createErrorTransaction(byte errorCode,
      @Nullable String errorDescription) {
    return TransactionMessages.createErrorTx(errorCode, errorDescription, QA_SERVICE_ID);
  }
}
