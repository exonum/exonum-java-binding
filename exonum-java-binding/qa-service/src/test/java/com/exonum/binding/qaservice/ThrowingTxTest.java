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
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.storage.database.TemporaryDb;
import com.exonum.binding.core.transaction.TransactionContext;
import com.exonum.binding.qaservice.Integration;
import com.exonum.binding.qaservice.QaArtifactInfo;
import com.exonum.binding.qaservice.QaSchema;
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
  void executeThrows(TestKit testKit) {
    TransactionMessage throwingTx = createThrowingTx(0L, QA_SERVICE_ID);
    testKit.createBlockWithTransactions(throwingTx);

    Snapshot view = testKit.getSnapshot();
    Blockchain blockchain = Blockchain.newInstance(view);
    /*
     todo: In tests it might be useful not only to be able to easily create expected
      results (which is relatively easy with a combination of factory methods and a builder),
      but also to access the description and match it against a condition (e.g., contains, or
      containsIgnoringCase), which is not perfect:

        ExecutionStatus txResult = blockchain.getTxResult(throwingTx.hash())
            .orElseThrow(() -> new AssertionError("No result"));

        assertTrue(txResult.hasError());
        String description = txResult
            .getError() // ! might throw without check above
            .getDescription();
        assertThat(description).containsIgnoringCase("foo");
     */
    ExecutionStatus txResult = blockchain.getTxResult(throwingTx.hash()).get();
    assertTrue(txResult.hasError());
    ExecutionError error = txResult.getError();
    assertThat(error.getKind()).isEqualTo(ErrorKind.RUNTIME);
    assertThat(error.getDescription()).contains("#execute of this transaction always throws");
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
//      ThrowingTx tx = new ThrowingTx(0L);
//
//      // Execute the transaction
//      TransactionContext context = newContext(view)
//          .serviceName(QA_SERVICE_NAME)
//          .serviceId(QA_SERVICE_ID)
//          .build();
//      IllegalStateException expected = assertThrows(IllegalStateException.class,
//          () -> tx.execute(context));
//
//      // Check that execute cleared the maps
//      assertThat(schema.counters().isEmpty()).isTrue();
//      assertThat(schema.counterNames().isEmpty()).isTrue();
//
//      // Check the exception message
//      String message = expected.getMessage();
//      assertThat(message).contains("#execute of this transaction always throws");
    }
  }
}
