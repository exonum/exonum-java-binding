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
import static com.exonum.binding.qaservice.TransactionUtils.createThrowingTransaction;
import static com.exonum.binding.qaservice.TransactionUtils.newContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.blockchain.TransactionResult;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.core.blockchain.Blockchain;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.CloseFailuresException;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.database.MemoryDb;
import com.exonum.binding.core.transaction.RawTransaction;
import com.exonum.binding.core.transaction.TransactionContext;
import com.exonum.binding.qaservice.QaSchema;
import com.exonum.binding.qaservice.QaService;
import com.exonum.binding.qaservice.QaServiceModule;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.testkit.TestKit;
import com.exonum.binding.testkit.TestKitExtension;
import com.google.gson.reflect.TypeToken;
import java.util.Optional;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ThrowingTxIntegrationTest {

  @RegisterExtension
  TestKitExtension testKitExtension = new TestKitExtension(
      TestKit.builder()
          .withService(QaServiceModule.class));

  @Test
  void converterRoundtrip() {
    long seed = 10L;
    ThrowingTx tx = new ThrowingTx(seed);

    RawTransaction message = ThrowingTx.converter().toRawTransaction(tx);

    ThrowingTx txFromRaw = ThrowingTx.converter().fromRawTransaction(message);

    assertThat(txFromRaw).isEqualTo(tx);
  }

  @Test
  void info() {
    long seed = 10L;
    ThrowingTx tx = new ThrowingTx(seed);
    String info = tx.info();

    AnyTransaction<ThrowingTx> txParams = json().fromJson(info,
        new TypeToken<AnyTransaction<ThrowingTx>>(){}.getType());

    assertThat(txParams.service_id).isEqualTo(QaService.ID);
    assertThat(txParams.message_id).isEqualTo(QaTransaction.VALID_THROWING.id());
    assertThat(txParams.body).isEqualTo(tx);
  }

  @Test
  @RequiresNativeLibrary
  void executeThrows(TestKit testKit) {
    TransactionMessage throwingTx = createThrowingTransaction(0L);
    testKit.createBlockWithTransactions(throwingTx);

    testKit.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      Optional<TransactionResult> txResult = blockchain.getTxResult(throwingTx.hash());
      assertThat(txResult).isNotEmpty();
      TransactionResult transactionResult = txResult.get();
      assertThat(transactionResult.getType()).isEqualTo(TransactionResult.Type.UNEXPECTED_ERROR);
      assertThat(transactionResult.getErrorCode()).isEmpty();
      assertThat(transactionResult.getErrorDescription())
          .contains("#execute of this transaction always throws");
      return null;
    });
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
      ThrowingTx tx = new ThrowingTx(0L);

      // Execute the transaction
      TransactionContext context = newContext(view);
      IllegalStateException expected = assertThrows(IllegalStateException.class,
          () -> tx.execute(context));

      // Check that execute cleared the maps
      QaSchema schema = new QaSchema(view);
      assertThat(schema.counters().isEmpty()).isTrue();
      assertThat(schema.counterNames().isEmpty()).isTrue();

      // Check the exception message
      String message = expected.getMessage();
      assertThat(message).contains("#execute of this transaction always throws");
    }
  }

  @Test
  void equals() {
    EqualsVerifier.forClass(ThrowingTx.class)
        .verify();
  }
}
