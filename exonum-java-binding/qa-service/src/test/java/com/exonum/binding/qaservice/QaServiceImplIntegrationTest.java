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

import static com.exonum.binding.common.hash.Hashing.DEFAULT_HASH_SIZE_BYTES;
import static com.exonum.binding.common.hash.Hashing.sha256;
import static com.exonum.binding.qaservice.QaServiceImpl.AFTER_COMMIT_COUNTER_NAME;
import static com.exonum.binding.qaservice.QaServiceImpl.DEFAULT_COUNTER_NAME;
import static com.exonum.binding.qaservice.TransactionUtils.createCreateCounterTransaction;
import static com.exonum.binding.qaservice.TransactionUtils.createIncrementCounterTransaction;
import static com.exonum.binding.qaservice.TransactionUtils.createThrowingTransaction;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.configuration.StoredConfiguration;
import com.exonum.binding.common.configuration.ValidatorKey;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.core.service.Schema;
import com.exonum.binding.core.storage.indices.MapIndex;
import com.exonum.binding.qaservice.transactions.UnknownTx;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.testkit.EmulatedNode;
import com.exonum.binding.testkit.FakeTimeProvider;
import com.exonum.binding.testkit.TestKit;
import com.exonum.binding.testkit.TestKitExtension;
import com.exonum.binding.testkit.TimeProvider;
import com.exonum.binding.testkit.ValidatorCount;
import com.google.common.collect.ImmutableMap;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@RequiresNativeLibrary
// todo: Remove once https://github.com/junit-team/junit5/issues/1925 is released (in 5.5)
@Execution(ExecutionMode.SAME_THREAD)
class QaServiceImplIntegrationTest {

  @RegisterExtension
  TestKitExtension testKitExtension = new TestKitExtension(
      TestKit.builder()
          .withService(QaServiceModule.class));

  private static final short NEW_VALIDATOR_COUNT = 8;

  @Test
  void createDataSchema(TestKit testKit) {
    QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
    testKit.withSnapshot((view) -> {
      Schema dataSchema = service.createDataSchema(view);
      assertThat(dataSchema).isInstanceOf(QaSchema.class);
      return null;
    });
  }

  @Test
  void initialize(TestKit testKit) {
    testKit.withSnapshot((view) -> {
      // TODO: https://jira.bf.local/browse/ECR-2683 is needed for service configuration to be
      //  checked

      // Check that both the default and afterCommit counters were created.
      QaSchema schema = new QaSchema(view);
      MapIndex<HashCode, Long> counters = schema.counters();
      MapIndex<HashCode, String> counterNames = schema.counterNames();

      HashCode defaultCounterId = sha256().hashString(DEFAULT_COUNTER_NAME, UTF_8);
      HashCode afterCommitCounterId = sha256().hashString(AFTER_COMMIT_COUNTER_NAME, UTF_8);

      assertThat(counters.get(defaultCounterId)).isEqualTo(0L);
      assertThat(counterNames.get(defaultCounterId)).isEqualTo(DEFAULT_COUNTER_NAME);
      assertThat(counters.get(afterCommitCounterId)).isEqualTo(0L);
      assertThat(counterNames.get(afterCommitCounterId)).isEqualTo(AFTER_COMMIT_COUNTER_NAME);
      return null;
    });
  }

  @Test
  void submitCreateCounter(TestKit testKit) {
    QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
    String counterName = "bids";
    service.submitCreateCounter(counterName);
    TransactionMessage expectedTransaction = createCreateCounterTransaction(counterName, testKit);
    List<TransactionMessage> inPoolTransactions = testKit.getTransactionPool();
    assertThat(inPoolTransactions).containsExactly(expectedTransaction);
  }

  @Test
  void submitIncrementCounter(TestKit testKit) {
    QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);

    String counterName = "bids";
    HashCode counterId = sha256().hashString(counterName, UTF_8);
    long seed = 1L;
    service.submitIncrementCounter(seed, counterId);
    TransactionMessage expectedTransaction =
        createIncrementCounterTransaction(seed, counterId, testKit);
    List<TransactionMessage> inPoolTransactions = testKit.getTransactionPool();
    assertThat(inPoolTransactions).containsExactly(expectedTransaction);
  }

  @Test
  void submitValidThrowingTx(TestKit testKit) {
    QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
    long seed = 1L;
    service.submitValidThrowingTx(seed);
    TransactionMessage expectedTransaction = createThrowingTransaction(seed, testKit);
    List<TransactionMessage> inPoolTransactions = testKit.getTransactionPool();
    assertThat(inPoolTransactions).containsExactly(expectedTransaction);
  }

  @Test
  void submitUnknownTx(TestKit testKit) {
    Class<IllegalArgumentException> exceptionType = IllegalArgumentException.class;
    QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
    service.submitUnknownTx();

    Exception e = assertThrows(exceptionType, testKit::createBlock);

    String expectedTxId = Integer.toString(UnknownTx.ID);
    assertThat(e.getMessage())
        .contains("failed to convert transaction", expectedTxId);
  }

  @Test
  void getValue(TestKit testKit) {
    QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
    String counterName = "bids";
    TransactionMessage createCounterTransaction = createCreateCounterTransaction(counterName);
    testKit.createBlockWithTransactions(createCounterTransaction);
    HashCode counterId = sha256().hashString(counterName, UTF_8);
    Optional<Counter> counterValue = service.getValue(counterId);
    Counter expectedCounter = new Counter(counterName, 0L);
    assertThat(counterValue).hasValue(expectedCounter);
  }

  @Test
  void getValueNoSuchCounter(TestKit testKit) {
    QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
    HashCode counterId = sha256().hashString("Unknown counter", UTF_8);
    // Check there is no such counter
    assertThat(service.getValue(counterId)).isEmpty();
  }

  @Test
  void afterCommit(TestKit testKit) {
    // After the first block the afterCommit transaction is submitted
    testKit.createBlock();

    // After the second block the afterCommit transaction is executed
    testKit.createBlock();

    testKit.withSnapshot((view) -> {
      QaSchema schema = new QaSchema(view);
      MapIndex<HashCode, Long> counters = schema.counters();
      MapIndex<HashCode, String> counterNames = schema.counterNames();
      HashCode counterId = sha256().hashString(AFTER_COMMIT_COUNTER_NAME, UTF_8);

      assertThat(counters.get(counterId)).isEqualTo(1L);
      assertThat(counterNames.get(counterId)).isEqualTo(AFTER_COMMIT_COUNTER_NAME);
      return null;
    });
  }

  @Test
  void getActualConfiguration(@ValidatorCount(NEW_VALIDATOR_COUNT) TestKit testKit) {
    QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
    StoredConfiguration configuration = service.getActualConfiguration();

    HashCode expectedPreviousCfgHash = HashCode.fromBytes(new byte[DEFAULT_HASH_SIZE_BYTES]);
    assertThat(configuration.previousCfgHash()).isEqualTo(expectedPreviousCfgHash);

    EmulatedNode emulatedNode = testKit.getEmulatedNode();
    List<ValidatorKey> validatorKeys = configuration.validatorKeys();

    assertThat(validatorKeys).hasSize(NEW_VALIDATOR_COUNT);

    PublicKey emulatedNodeServiceKey = emulatedNode.getServiceKeyPair().getPublicKey();
    List<PublicKey> serviceKeys = configuration.validatorKeys().stream()
        .map(ValidatorKey::serviceKey)
        .collect(toList());

    assertThat(serviceKeys).hasSize(NEW_VALIDATOR_COUNT);
    assertThat(serviceKeys).contains(emulatedNodeServiceKey);
  }

  @Nested
  class WithTime {

    private final ZonedDateTime expectedTime = ZonedDateTime
        .of(2000, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC);
    private TestKit testKit;

    @BeforeEach
    void setUpConsolidatedTime() {
      TimeProvider timeProvider = FakeTimeProvider.create(expectedTime);
      testKit = TestKit.builder()
          .withService(QaServiceModule.class)
          .withTimeService(timeProvider)
          .build();

      // Commit two blocks for time oracle to update consolidated time. Two blocks are needed as
      // after the first block time transactions are generated and after the second one they are
      // processed
      testKit.createBlock();
      testKit.createBlock();
    }

    @AfterEach
    void destroyTestKit() {
      testKit.close();
    }

    @Test
    void getTime() {
      QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
      Optional<ZonedDateTime> consolidatedTime = service.getTime();
      assertThat(consolidatedTime).hasValue(expectedTime);
    }

    @Test
    void getValidatorsTime() {
      QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
      Map<PublicKey, ZonedDateTime> validatorsTimes = service.getValidatorsTimes();
      EmulatedNode emulatedNode = testKit.getEmulatedNode();
      PublicKey nodePublicKey = emulatedNode.getServiceKeyPair().getPublicKey();
      Map<PublicKey, ZonedDateTime> expected = ImmutableMap.of(nodePublicKey, expectedTime);
      assertThat(validatorsTimes).isEqualTo(expected);
    }
  }
}
