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

import static com.exonum.binding.common.hash.Hashing.defaultHashFunction;
import static com.exonum.binding.common.hash.Hashing.sha256;
import static com.exonum.binding.qaservice.QaArtifactInfo.QA_SERVICE_ID;
import static com.exonum.binding.qaservice.QaArtifactInfo.QA_SERVICE_NAME;
import static com.exonum.binding.qaservice.TransactionError.UNKNOWN_COUNTER;
import static com.exonum.binding.qaservice.TransactionMessages.createCreateCounterTx;
import static com.exonum.binding.qaservice.TransactionMessages.createIncrementCounterTx;
import static com.exonum.core.messages.Runtime.ErrorKind.SERVICE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.core.blockchain.Blockchain;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.storage.indices.MapIndex;
import com.exonum.binding.testkit.TestKit;
import com.exonum.binding.testkit.TestKitExtension;
import com.exonum.core.messages.Runtime.ExecutionError;
import com.exonum.core.messages.Runtime.ExecutionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@Integration
class IncrementCounterTxTest {

  @RegisterExtension
  TestKitExtension testKitExtension = new TestKitExtension(
      QaArtifactInfo.createQaServiceTestkit());

  @Test
  void executeIncrementsCounter(TestKit testKit) {
    // Add a new counter with the given name and initial value
    String counterName = "counter";
    TransactionMessage createCounterTx = createCreateCounterTx(counterName, QA_SERVICE_ID);
    testKit.createBlockWithTransactions(createCounterTx);

    // Submit and execute the transaction
    long seed = 0L;
    HashCode counterId = sha256().hashString(counterName, UTF_8);
    TransactionMessage incrementTx = createIncrementCounterTx(seed, counterId, QA_SERVICE_ID);
    testKit.createBlockWithTransactions(incrementTx);

    // Check the counter has an incremented value
    Snapshot view = testKit.getSnapshot();
    QaSchema schema = new QaSchema(view, QA_SERVICE_NAME);
    MapIndex<HashCode, Long> counters = schema.counters();
    long expectedValue = 1;

    assertThat(counters.get(counterId)).isEqualTo(expectedValue);
  }

  @Test
  void executeNoSuchCounter(TestKit testKit) {
    String counterName = "unknown-counter";
    HashCode counterId = defaultHashFunction().hashString(counterName, UTF_8);
    TransactionMessage incrementCounterTx = createIncrementCounterTx(0L, counterId,
        QA_SERVICE_ID);
    testKit.createBlockWithTransactions(incrementCounterTx);

    Snapshot view = testKit.getSnapshot();
    Blockchain blockchain = Blockchain.newInstance(view);
    ExecutionStatus txResult = blockchain.getTxResult(incrementCounterTx.hash()).get();
    ExecutionError error = txResult.getError();
    assertThat(error.getKind()).isEqualTo(SERVICE);
    assertThat(error.getCode()).isEqualTo(UNKNOWN_COUNTER.code);
  }
}
