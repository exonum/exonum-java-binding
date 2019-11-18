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

import static com.exonum.binding.common.blockchain.ExecutionStatuses.serviceError;
import static com.exonum.binding.common.crypto.CryptoFunctions.ed25519;
import static com.exonum.binding.common.hash.Hashing.sha256;
import static com.exonum.binding.qaservice.QaArtifactInfo.QA_SERVICE_ID;
import static com.exonum.binding.qaservice.QaArtifactInfo.QA_SERVICE_NAME;
import static com.exonum.binding.qaservice.TransactionMessages.createCreateCounterTx;
import static com.exonum.binding.qaservice.transactions.TransactionError.COUNTER_ALREADY_EXISTS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.core.blockchain.Blockchain;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.storage.indices.MapIndex;
import com.exonum.binding.qaservice.Integration;
import com.exonum.binding.qaservice.QaArtifactInfo;
import com.exonum.binding.qaservice.QaSchema;
import com.exonum.binding.testkit.TestKit;
import com.exonum.binding.testkit.TestKitExtension;
import com.exonum.core.messages.Runtime.ExecutionStatus;
import java.util.Optional;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@Integration
class CreateCounterTxTest {

  @RegisterExtension
  TestKitExtension testKitExtension = new TestKitExtension(
      QaArtifactInfo.createQaServiceTestkit()
  );

  @Test
  void rejectsEmptyName() {
    String name = "";

    Exception e = assertThrows(IllegalArgumentException.class,
        () -> new CreateCounterTx(name));
    assertThat(e.getMessage()).contains("Name must not be blank");
  }

  @Test
  void executeNewCounter(TestKit testKit) {
    String counterName = "counter";
    TransactionMessage tx = createCreateCounterTx(counterName, QA_SERVICE_ID);
    testKit.createBlockWithTransactions(tx);

    Snapshot view = testKit.getSnapshot();
    QaSchema schema = new QaSchema(view, QA_SERVICE_NAME);
    MapIndex<HashCode, Long> counters = schema.counters();
    MapIndex<HashCode, String> counterNames = schema.counterNames();
    HashCode counterId = sha256().hashString(counterName, UTF_8);

    assertThat(counters.get(counterId)).isEqualTo(0L);
    assertThat(counterNames.get(counterId)).isEqualTo(counterName);
  }

  @Test
  void executeAlreadyExistingCounter(TestKit testKit) {
    String counterName = "counter";
    KeyPair key1 = ed25519().generateKeyPair();
    KeyPair key2 = ed25519().generateKeyPair();
    TransactionMessage transactionMessage = createCreateCounterTx(counterName, QA_SERVICE_ID, key1);
    TransactionMessage transactionMessage2 = createCreateCounterTx(counterName, QA_SERVICE_ID,
        key2);
    testKit.createBlockWithTransactions(transactionMessage);
    testKit.createBlockWithTransactions(transactionMessage2);

    Snapshot view = testKit.getSnapshot();
    Blockchain blockchain = Blockchain.newInstance(view);
    Optional<ExecutionStatus> txResult = blockchain.getTxResult(transactionMessage2.hash());
    ExecutionStatus expectedTransactionResult = serviceError(COUNTER_ALREADY_EXISTS.code);
    assertThat(txResult).hasValue(expectedTransactionResult);
  }

  @Test
  void equals() {
    EqualsVerifier.forClass(CreateCounterTx.class)
        .verify();
  }

}
