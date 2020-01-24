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
import static com.exonum.binding.qaservice.TransactionMessages.createThrowingTx;
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
import com.exonum.binding.qaservice.transactions.TxMessageProtos.ThrowingTxBody;
import com.exonum.binding.testkit.TestKit;
import com.exonum.binding.testkit.TestKitExtension;
import com.exonum.core.messages.Runtime.ErrorKind;
import com.exonum.core.messages.Runtime.ExecutionError;
import com.exonum.core.messages.Runtime.ExecutionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@Integration
class ThrowingTxTest {

  @RegisterExtension
  TestKitExtension testKitExtension = new TestKitExtension(
      QaArtifactInfo.createQaServiceTestkit());

  @Test
  void throwingTxMustHaveUnexpectedErrorCode(TestKit testKit) {
    long seed = 0L;
    TransactionMessage throwingTx = createThrowingTx(seed, QA_SERVICE_ID);
    testKit.createBlockWithTransactions(throwingTx);

    Snapshot snapshot = testKit.getSnapshot();
    Blockchain blockchain = Blockchain.newInstance(snapshot);
    ExecutionStatus txResult = blockchain.getTxResult(throwingTx.hash()).get();
    assertTrue(txResult.hasError());
    ExecutionError error = txResult.getError();
    // Verify only the properties EJB is responsible for
    assertThat(error.getKind()).isEqualTo(ErrorKind.UNEXPECTED);
    assertThat(error.getDescription())
        .contains("#execute of this transaction always throws")
        .contains(String.valueOf(throwingTx.hash()))
        .contains(Long.toString(seed));
    assertThat(error.getRuntimeId()).isEqualTo(JAVA.getId());
  }

  @Test
  void executeClearsQaServiceData() throws CloseFailuresException {
    try (TemporaryDb db = TemporaryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Fork fork = db.createFork(cleaner);
      QaSchema schema = new QaSchema(fork, QA_SERVICE_NAME);

      // Initialize storage with a counter equal to 10
      String name = "counter";
      long value = 10L;
      createCounter(schema, name, value);

      QaServiceImpl qaService = new QaServiceImpl(
          ServiceInstanceSpec.newInstance(QA_SERVICE_NAME, QA_SERVICE_ID, ARTIFACT_ID));

      // Create the transaction arguments
      ThrowingTxBody arguments = ThrowingTxBody.newBuilder()
          .setSeed(17L)
          .build();
      TransactionContext context = newContext(fork)
          .serviceName(QA_SERVICE_NAME)
          .serviceId(QA_SERVICE_ID)
          .build();
      // Invoke the transaction
      assertThrows(IllegalStateException.class,
          () -> qaService.throwing(arguments, context));

      // Check that it has cleared the maps
      assertThat(schema.counters().isEmpty()).isTrue();
      assertThat(schema.counterNames().isEmpty()).isTrue();
    }
  }
}
