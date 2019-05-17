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

import com.exonum.binding.blockchain.Block;
import com.exonum.binding.common.blockchain.TransactionLocation;
import com.exonum.binding.common.blockchain.TransactionResult;
import com.exonum.binding.common.configuration.StoredConfiguration;
import com.exonum.binding.common.configuration.ValidatorKey;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.qaservice.transactions.QaTransaction;
import com.exonum.binding.qaservice.transactions.UnknownTx;
import com.exonum.binding.service.Schema;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.testkit.EmulatedNode;
import com.exonum.binding.testkit.EmulatedNodeType;
import com.exonum.binding.testkit.FakeTimeProvider;
import com.exonum.binding.testkit.TestKit;
import com.exonum.binding.testkit.TimeProvider;
import com.exonum.binding.util.LibraryLoader;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@RequiresNativeLibrary
class QaServiceImplIntegrationTest {

  static {
    LibraryLoader.load();
  }

  private ListAppender logAppender;

  @BeforeEach
  void setUp() {
    logAppender = getCapturingLogAppender();
  }

  private static ListAppender getCapturingLogAppender() {
    LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
    Configuration config = ctx.getConfiguration();
    ListAppender appender = (ListAppender) config.getAppenders().get("ListAppender");
    // Clear the appender so that it doesn't contain entries from the previous tests.
    appender.clear();
    return appender;
  }

  @AfterEach
  void tearDown() {
    logAppender.clear();
  }

  @Test
  void createDataSchema() {
    try (TestKit testKit = TestKit.forService(QaServiceModule.class)) {
      QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
      testKit.withSnapshot((view) -> {
        Schema dataSchema = service.createDataSchema(view);
        assertThat(dataSchema).isInstanceOf(QaSchema.class);
        return null;
      });
    }
  }

  @Test
  void getStateHashesLogsThem() {
    try (TestKit testKit = TestKit.forService(QaServiceModule.class)) {
      QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
      testKit.withSnapshot((view) -> {
        List<HashCode> stateHashes = service.getStateHashes(view);
        int numMerkelizedCollections = 1;
        assertThat(stateHashes).hasSize(numMerkelizedCollections);

        List<String> logMessages = logAppender.getMessages();
        // Logger contains two messages as #getStateHashes is called during service instantiation
        int expectedNumMessages = 2;
        assertThat(logMessages).hasSize(expectedNumMessages);

        assertThat(logMessages.get(0))
            .contains("ERROR")
            .contains(stateHashes.get(0).toString());
        return null;
      });
    }
  }

  @Test
  void initialize() {
    try (TestKit testKit = TestKit.forService(QaServiceModule.class)) {
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
  }

  @Test
  void submitCreateCounter() {
    try (TestKit testKit = TestKit.forService(QaServiceModule.class)) {
      QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
      String counterName = "bids";
      service.submitCreateCounter(counterName);
      TransactionMessage expectedTransaction = createCreateCounterTransaction(counterName, testKit);
      List<TransactionMessage> inPoolTransactions =
          testKit.findTransactionsInPool(tx -> tx.getServiceId() == QaService.ID
              && tx.getTransactionId() == QaTransaction.CREATE_COUNTER.id()
              && tx.equals(expectedTransaction));
      assertThat(inPoolTransactions).hasSize(1);
    }
  }

  @Test
  void submitIncrementCounter() {
    try (TestKit testKit = TestKit.forService(QaServiceModule.class)) {
      QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
      String counterName = "bids";
      service.submitCreateCounter(counterName);
      testKit.createBlock();

      HashCode counterId = sha256().hashString(counterName, UTF_8);
      long seed = 1L;
      service.submitIncrementCounter(seed, counterId);
      TransactionMessage expectedTransaction =
          createIncrementCounterTransaction(seed, counterId, testKit);
      List<TransactionMessage> inPoolTransactions =
          testKit.findTransactionsInPool(tx -> tx.getServiceId() == QaService.ID
              && tx.getTransactionId() == QaTransaction.INCREMENT_COUNTER.id()
              && tx.equals(expectedTransaction));
      assertThat(inPoolTransactions).hasSize(1);
    }
  }

  @Test
  void submitValidThrowingTx() {
    try (TestKit testKit = TestKit.forService(QaServiceModule.class)) {
      QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
      long seed = 1L;
      service.submitValidThrowingTx(seed);
      TransactionMessage expectedTransaction = createThrowingTransaction(seed, testKit);
      List<TransactionMessage> inPoolTransactions =
          testKit.findTransactionsInPool(tx -> tx.getServiceId() == QaService.ID
              && tx.getTransactionId() == QaTransaction.VALID_THROWING.id()
              && tx.equals(expectedTransaction));
      assertThat(inPoolTransactions).hasSize(1);
    }
  }

  @Test
  void submitUnknownTx() {
    Class<IllegalArgumentException> exceptionType = IllegalArgumentException.class;
    try (TestKit testKit = TestKit.forService(QaServiceModule.class)) {
      QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
      service.submitUnknownTx();

      Exception e = assertThrows(exceptionType, testKit::createBlock);

      String expectedTxId = Integer.toString(UnknownTx.ID);
      assertThat(e.getMessage())
          .contains("failed to convert transaction", expectedTxId);
    }
  }

  @Test
  void getValue() {
    try (TestKit testKit = TestKit.forService(QaServiceModule.class)) {
      QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
      String counterName = "bids";
      TransactionMessage createCounterTransaction = createCreateCounterTransaction(counterName);
      testKit.createBlockWithTransactions(createCounterTransaction);
      HashCode counterId = sha256().hashString(counterName, UTF_8);
      Optional<Counter> counterValue = service.getValue(counterId);
      Counter expectedCounter = new Counter(counterName, 0L);
      assertThat(counterValue).hasValue(expectedCounter);
    }
  }

  @Test
  void getValueNoSuchCounter() {
    try (TestKit testKit = TestKit.forService(QaServiceModule.class)) {
      QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
      HashCode counterId = sha256().hashString("Unknown counter", UTF_8);
      // Check there is no such counter
      assertThat(service.getValue(counterId)).isEmpty();
    }
  }

  @Test
  void getHeight() {
    try (TestKit testKit = TestKit.forService(QaServiceModule.class)) {
      QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
      testKit.createBlock();
      Height expectedHeight = new Height(1L);
      assertThat(service.getHeight()).isEqualTo(expectedHeight);
    }
  }

  @Test
  void getBlockHashes() {
    try (TestKit testKit = TestKit.forService(QaServiceModule.class)) {
      QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
      Block block = testKit.createBlock();
      List<HashCode> hashes = service.getBlockHashes();
      // Should contain genesis and created block hashes
      assertThat(hashes).hasSize(2);
      assertThat(hashes.get(1)).isEqualTo(block.getBlockHash());
    }
  }

  @Test
  void getBlockTransactionsByHeight() {
    try (TestKit testKit = TestKit.forService(QaServiceModule.class)) {
      QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
      TransactionMessage createCounterTransaction = createCreateCounterTransaction("counterName");
      testKit.createBlockWithTransactions(createCounterTransaction);
      List<HashCode> transactionHashes = service.getBlockTransactions(1L);
      assertThat(transactionHashes).isEqualTo(ImmutableList.of(createCounterTransaction.hash()));
    }
  }

  @Test
  void afterCommit() {
    try (TestKit testKit = TestKit.forService(QaServiceModule.class)) {
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
  }

  @Test
  void getActualConfiguration() {
    short validatorCount = 8;
    try (TestKit testKit = TestKit.builder(EmulatedNodeType.VALIDATOR)
        .withValidators(validatorCount)
        .withService(QaServiceModule.class)
        .build()) {
      QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
      StoredConfiguration configuration = service.getActualConfiguration();

      HashCode expectedPreviousCfgHash = HashCode.fromBytes(new byte[DEFAULT_HASH_SIZE_BYTES]);
      assertThat(configuration.previousCfgHash()).isEqualTo(expectedPreviousCfgHash);

      EmulatedNode emulatedNode = testKit.getEmulatedNode();
      List<ValidatorKey> validatorKeys = configuration.validatorKeys();

      assertThat(validatorKeys).hasSize(validatorCount);

      PublicKey emulatedNodeServiceKey = emulatedNode.getServiceKeyPair().getPublicKey();
      List<PublicKey> serviceKeys = configuration.validatorKeys().stream()
          .map(ValidatorKey::serviceKey)
          .collect(toList());

      assertThat(serviceKeys).hasSize(validatorCount);
      assertThat(serviceKeys).contains(emulatedNodeServiceKey);
    }
  }

  @Test
  void getBlockTransactionsByBlockId() {
    try (TestKit testKit = TestKit.forService(QaServiceModule.class)) {
      QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
      TransactionMessage createCounterTransaction = createCreateCounterTransaction("counterName");
      testKit.createBlockWithTransactions(createCounterTransaction);
      List<HashCode> blockTransactions = service.getBlockTransactions(1L);
      assertThat(blockTransactions).hasSize(1);
      assertThat(blockTransactions.get(0)).isEqualTo(createCounterTransaction.hash());
    }
  }

  @Test
  void getTxMessages() {
    try (TestKit testKit = TestKit.forService(QaServiceModule.class)) {
      QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
      TransactionMessage createCounterTransaction = createCreateCounterTransaction("counterName");
      testKit.createBlockWithTransactions(createCounterTransaction);
      Map<HashCode, TransactionMessage> txMessages = service.getTxMessages();
      // Contains two transactions - one submitted above and the second committed in afterCommit
      assertThat(txMessages).hasSize(2);
      assertThat(txMessages.get(createCounterTransaction.hash()))
          .isEqualTo(createCounterTransaction);
    }
  }

  @Test
  void getTxResults() {
    try (TestKit testKit = TestKit.forService(QaServiceModule.class)) {
      QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
      TransactionMessage createCounterTransaction = createCreateCounterTransaction("counterName");
      testKit.createBlockWithTransactions(createCounterTransaction);
      Map<HashCode, TransactionResult> txResults = service.getTxResults();
      assertThat(txResults)
          .isEqualTo(ImmutableMap.of(createCounterTransaction.hash(),
              TransactionResult.successful()));
    }
  }

  @Test
  void getTxResult() {
    try (TestKit testKit = TestKit.forService(QaServiceModule.class)) {
      QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
      TransactionMessage createCounterTransaction = createCreateCounterTransaction("counterName");
      testKit.createBlockWithTransactions(createCounterTransaction);
      Optional<TransactionResult> txResult = service.getTxResult(createCounterTransaction.hash());
      assertThat(txResult).hasValue(TransactionResult.successful());
    }
  }

  @Test
  void getTxLocations() {
    try (TestKit testKit = TestKit.forService(QaServiceModule.class)) {
      QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
      TransactionMessage createCounterTransaction = createCreateCounterTransaction("counterName");
      testKit.createBlockWithTransactions(createCounterTransaction);
      Map<HashCode, TransactionLocation> txLocations = service.getTxLocations();
      TransactionLocation expectedTransactionLocation = TransactionLocation.valueOf(1L, 0L);
      assertThat(txLocations)
          .isEqualTo(ImmutableMap.of(createCounterTransaction.hash(),
              expectedTransactionLocation));
    }
  }

  @Test
  void getTxLocation() {
    try (TestKit testKit = TestKit.forService(QaServiceModule.class)) {
      QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
      TransactionMessage createCounterTransaction = createCreateCounterTransaction("counterName");
      testKit.createBlockWithTransactions(createCounterTransaction);
      Optional<TransactionLocation> txLocation =
          service.getTxLocation(createCounterTransaction.hash());
      TransactionLocation expectedTransactionLocation = TransactionLocation.valueOf(1L, 0L);
      assertThat(txLocation).hasValue(expectedTransactionLocation);
    }
  }

  @Test
  void getBlocks() {
    try (TestKit testKit = TestKit.forService(QaServiceModule.class)) {
      QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
      Block block = testKit.createBlock();
      Map<HashCode, Block> blocks = service.getBlocks();
      assertThat(blocks).hasSize(2);
      assertThat(blocks.get(block.getBlockHash())).isEqualTo(block);
    }
  }

  @Test
  void getBlockByHeight() {
    try (TestKit testKit = TestKit.forService(QaServiceModule.class)) {
      QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
      Block block = testKit.createBlock();
      Block actualBlock = service.getBlockByHeight(block.getHeight());
      assertThat(actualBlock).isEqualTo(block);
    }
  }

  @Test
  void getBlockById() {
    try (TestKit testKit = TestKit.forService(QaServiceModule.class)) {
      QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
      Block block = testKit.createBlock();
      Optional<Block> actualBlock = service.getBlockById(block.getBlockHash());
      assertThat(actualBlock).hasValue(block);
    }
  }

  @Test
  void getLastBlock() {
    try (TestKit testKit = TestKit.forService(QaServiceModule.class)) {
      QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
      Block block = testKit.createBlock();
      Block lastBlock = service.getLastBlock();
      assertThat(lastBlock).isEqualTo(block);
    }
  }

  @Nested
  class WithTime {

    private final ZonedDateTime expectedTime = ZonedDateTime
        .of(2000, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC);
    private TestKit testKit;

    @BeforeEach
    void setUpConsolidatedTime() {
      TimeProvider timeProvider = FakeTimeProvider.create(expectedTime);
      testKit = TestKit.builder(EmulatedNodeType.VALIDATOR)
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
