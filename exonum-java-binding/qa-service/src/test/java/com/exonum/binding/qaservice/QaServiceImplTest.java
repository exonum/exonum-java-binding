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

import static com.exonum.binding.common.serialization.json.JsonSerializer.json;
import static com.exonum.binding.qaservice.ApiController.QaPaths.GET_CONSENSUS_CONFIGURATION_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.SUBMIT_INCREMENT_COUNTER_TX_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.SUBMIT_UNKNOWN_TX_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.TIME_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.VALIDATORS_TIMES_PATH;
import static com.exonum.binding.qaservice.ApiControllerTest.multiMap;
import static com.exonum.binding.qaservice.QaArtifactInfo.ARTIFACT_ID;
import static com.exonum.binding.qaservice.QaArtifactInfo.QA_SERVICE_ID;
import static com.exonum.binding.qaservice.QaArtifactInfo.QA_SERVICE_NAME;
import static com.exonum.binding.qaservice.QaExecutionError.RESUME_SERVICE_ERROR;
import static com.exonum.binding.qaservice.QaServiceImpl.AFTER_COMMIT_COUNTER_NAME;
import static com.exonum.binding.qaservice.QaServiceImpl.AFTER_TXS_COUNTER_NAME;
import static com.exonum.binding.qaservice.QaServiceImpl.BEFORE_TXS_COUNTER_NAME;
import static com.exonum.binding.qaservice.QaServiceImpl.DEFAULT_COUNTER_NAME;
import static com.exonum.binding.qaservice.TransactionMessages.createCreateCounterTx;
import static com.exonum.binding.qaservice.TransactionMessages.createIncrementCounterTx;
import static com.exonum.binding.qaservice.TransactionMessages.createUnknownTx;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.core.blockchain.Blockchain;
import com.exonum.binding.core.blockchain.BlockchainData;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.CloseFailuresException;
import com.exonum.binding.core.runtime.ServiceInstanceSpec;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.database.TemporaryDb;
import com.exonum.binding.core.storage.indices.MapIndex;
import com.exonum.binding.core.transaction.ExecutionException;
import com.exonum.binding.qaservice.Config.QaConfiguration;
import com.exonum.binding.qaservice.Config.QaResumeArguments;
import com.exonum.binding.test.Integration;
import com.exonum.binding.testkit.EmulatedNode;
import com.exonum.binding.testkit.FakeTimeProvider;
import com.exonum.binding.testkit.TestKit;
import com.exonum.binding.testkit.TestKitExtension;
import com.exonum.binding.testkit.TimeProvider;
import com.exonum.messages.core.Blockchain.Config;
import com.google.common.collect.ImmutableMap;
import com.google.gson.reflect.TypeToken;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Integration
@Execution(ExecutionMode.SAME_THREAD) // Use same thread execution mode as each test will
// instantiate its own vertx
class QaServiceImplTest {

  private static final ZonedDateTime INITIAL_TIME = ZonedDateTime.now(ZoneOffset.UTC);

  private final FakeTimeProvider timeProvider = FakeTimeProvider.create(INITIAL_TIME);

  @RegisterExtension
  final TestKitExtension testKitExtension = new TestKitExtension(
      QaArtifactInfo.createQaServiceTestkit(timeProvider));

  @Test
  void initialize() {
    String serviceName = "qa";
    String timeServiceName = "time";
    try (TestKit testKit = TestKit.builder()
        .withArtifactsDirectory(QaArtifactInfo.ARTIFACT_DIR)
        .withDeployedArtifact(QaArtifactInfo.ARTIFACT_ID, QaArtifactInfo.ARTIFACT_FILENAME)
        .withService(QaArtifactInfo.ARTIFACT_ID, serviceName, 1,
            QaConfiguration.newBuilder()
                .setTimeOracleName(timeServiceName)
                .build())
        .withTimeService(timeServiceName, 2, TimeProvider.systemTime())
        .build()) {
      BlockchainData snapshot = testKit.getBlockchainData(serviceName);
      QaSchema schema = new QaSchema(snapshot);
      // Check the time dependency is saved
      Optional<String> timeService = schema.timeOracleName().toOptional();
      assertThat(timeService).hasValue(timeServiceName);

      // Check that all the default counters were created.
      MapIndex<String, Long> counters = schema.counters();

      assertThat(counters.get(DEFAULT_COUNTER_NAME)).isEqualTo(0L);
      assertThat(counters.get(AFTER_COMMIT_COUNTER_NAME)).isEqualTo(0L);
      assertThat(counters.get(BEFORE_TXS_COUNTER_NAME)).isEqualTo(0L);
      // This one is immediately incremented for genesis-created QA service
      assertThat(counters.get(AFTER_TXS_COUNTER_NAME)).isEqualTo(1L);
    }
  }

  @Test
  void resume() throws CloseFailuresException {
    String counterName = "resume";
    ServiceInstanceSpec spec = ServiceInstanceSpec
        .newInstance(QA_SERVICE_NAME, QA_SERVICE_ID, ARTIFACT_ID);
    byte[] arguments = QaResumeArguments.newBuilder()
        .setCounterName(counterName)
        .setShouldThrowException(false)
        .build()
        .toByteArray();

    try (TemporaryDb db = TemporaryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      Fork fork = db.createFork(cleaner);
      BlockchainData blockchainData = BlockchainData.fromRawAccess(fork, QA_SERVICE_NAME);

      QaServiceImpl qaService = new QaServiceImpl(spec);
      qaService.resume(blockchainData, arguments);

      QaSchema schema = new QaSchema(blockchainData);
      MapIndex<String, Long> counters = schema.counters();
      assertThat(counters.get(counterName)).isEqualTo(0L);
    }
  }

  @Test
  void resumeShouldThrowException() {
    ServiceInstanceSpec spec = ServiceInstanceSpec
        .newInstance(QA_SERVICE_NAME, QA_SERVICE_ID, ARTIFACT_ID);
    byte[] arguments = QaResumeArguments.newBuilder()
        .setShouldThrowException(true)
        .build()
        .toByteArray();
    BlockchainData fork = mock(BlockchainData.class);

    QaServiceImpl qaService = new QaServiceImpl(spec);

    ExecutionException exception = assertThrows(ExecutionException.class,
        () -> qaService.resume(fork, arguments));
    assertThat(exception.getErrorCode()).isEqualTo(RESUME_SERVICE_ERROR.code);
  }

  @Test
  void initializeBadConfig() {
    String serviceName = "qa";
    // Empty name is not allowed
    String timeServiceName = "";
    TestKit.Builder testKitBuilder = TestKit.builder()
        .withArtifactsDirectory(QaArtifactInfo.ARTIFACT_DIR)
        .withDeployedArtifact(QaArtifactInfo.ARTIFACT_ID, QaArtifactInfo.ARTIFACT_FILENAME)
        .withService(QaArtifactInfo.ARTIFACT_ID, serviceName, 1,
            QaConfiguration.newBuilder()
                .setTimeOracleName(timeServiceName)
                .build());

    assertThrows(RuntimeException.class, testKitBuilder::build);
  }

  @Test
  void beforeTransactions(TestKit testKit) {
    // Initially, before txs must be 0 for services added in genesis block,
    // because beforeTransactions is not called for genesis-block added services.
    checkCounter(testKit, BEFORE_TXS_COUNTER_NAME, 0L);

    // Create several blocks
    for (int i = 1; i <= 2; i++) {
      testKit.createBlock();
      checkCounter(testKit, BEFORE_TXS_COUNTER_NAME, i);
    }
  }

  @Test
  void afterTransactions(TestKit testKit) {
    // Initially, after txs must be 1 for services added in genesis block.
    // afterTransactions is called for genesis-block added services.
    checkCounter(testKit, AFTER_TXS_COUNTER_NAME, 1L);
    // Create several blocks
    for (int i = 1; i <= 2; i++) {
      testKit.createBlock();
      checkCounter(testKit, AFTER_TXS_COUNTER_NAME, 1L + i);
    }
  }

  @Test
  void afterCommit(TestKit testKit) {
    // Create a block so that the transaction, submitted in after commit handler,
    // executed after the genesis block, is executed.
    testKit.createBlock();

    checkAfterCommitCounter(testKit, 1L);

    // Create another block, so that the transaction submitted after the last block above,
    // is also executed
    testKit.createBlock();

    checkAfterCommitCounter(testKit, 2L);
  }

  private static void checkAfterCommitCounter(TestKit testkit, long expectedValue) {
    checkCounter(testkit, AFTER_COMMIT_COUNTER_NAME, expectedValue);
  }

  private static void checkCounter(TestKit testkit, String counterName, long expectedValue) {
    BlockchainData snapshot = testkit.getBlockchainData(QA_SERVICE_NAME);
    QaSchema schema = new QaSchema(snapshot);
    MapIndex<String, Long> counters = schema.counters();
    assertThat(counters.get(counterName)).isEqualTo(expectedValue);
  }

  @Nested
  @ExtendWith(VertxExtension.class)
  class ApiTests {

    private static final String HOST = "0.0.0.0";
    private static final String PATH_PREFIX = "/api/services/" + QA_SERVICE_NAME + "/";
    private WebClient webClient;
    private int port;

    @BeforeEach
    void setUpClient(Vertx vertx, TestKit testKit) {
      webClient = WebClient.create(vertx);
      port = testKit.getPort();
    }

    @Test
    void submitIncrementCounter(TestKit testKit, VertxTestContext context) {
      // Send 'submitIncrementCounter' request
      String counterName = "test counter";
      long seed = 17L;
      MultiMap params = multiMap("counterName", counterName,
          "seed", Long.toString(seed));

      post(SUBMIT_INCREMENT_COUNTER_TX_PATH)
          .sendForm(params, context.succeeding(response -> context.verify(() -> {
            // Check response
            assertThat(response.statusCode()).isEqualTo(HTTP_CREATED);

            // Verify the service submitted a transaction
            List<TransactionMessage> transactionPool = testKit.getTransactionPool();

            KeyPair expectedAuthor = testKit.getEmulatedNode().getServiceKeyPair();
            TransactionMessage expectedMessage = createIncrementCounterTx(seed, counterName,
                QA_SERVICE_ID, expectedAuthor);

            assertThat(transactionPool).contains(expectedMessage);

            context.completeNow();
          })));
    }

    @Test
    void submitUnknownTx(TestKit testKit, VertxTestContext context) {
      // Submit the unknown tx
      post(SUBMIT_UNKNOWN_TX_PATH)
          .send(context.succeeding(response -> context.verify(() -> {
            // Check response
            assertThat(response.statusCode()).isEqualTo(HTTP_CREATED);

            // Verify the service submitted a transaction
            List<TransactionMessage> transactionPool = testKit.getTransactionPool();

            KeyPair expectedAuthor = testKit.getEmulatedNode().getServiceKeyPair();
            TransactionMessage expectedMessage = createUnknownTx(QA_SERVICE_ID, expectedAuthor);
            assertThat(transactionPool).contains(expectedMessage);

            context.completeNow();
          })));
    }

    @Test
    void getValue(TestKit testKit, VertxTestContext context) {
      // Create a counter
      String counterName = "bids";
      TransactionMessage createCounterTransaction =
          createCreateCounterTx(counterName, QA_SERVICE_ID);
      testKit.createBlockWithTransactions(createCounterTransaction);

      // Request its value
      get("counter/" + counterName)
          .send(context.succeeding(response -> context.verify(() -> {
            // Verify the response
            assertThat(response.statusCode()).isEqualTo(HTTP_OK);
            Counter counter = json().fromJson(response.bodyAsString(), Counter.class);
            Counter expected = new Counter(counterName, 0L);
            assertThat(counter).isEqualTo(expected);

            context.completeNow();
          })));
    }

    @Test
    void getValueNoSuchCounter(VertxTestContext context) {
      // Check there is no such counter
      get("counter/unknown")
          .send(context.succeeding(response -> context.verify(() -> {
            // Verify the response
            assertThat(response.statusCode()).isEqualTo(HTTP_NOT_FOUND);
            context.completeNow();
          })));
    }

    @Test
    void getConsensusConfiguration(TestKit testKit, VertxTestContext context) {
      get(GET_CONSENSUS_CONFIGURATION_PATH)
          .send(context.succeeding(response -> context.verify(() -> {
            // Verify the response
            assertThat(response.statusCode()).isEqualTo(HTTP_OK);
            byte[] body = response.bodyAsBuffer().getBytes();
            Config config = Config.parseFrom(body);

            Blockchain blockchain = Blockchain.newInstance(testKit.getSnapshot());
            Config expectedConfig = blockchain.getConsensusConfiguration();

            assertThat(config).isEqualTo(expectedConfig);

            context.completeNow();
          })));
    }

    @Test
    void getTimeNotYetAvailable(VertxTestContext context) {
      get(TIME_PATH)
          .send(context.succeeding(response -> context.verify(() -> {
            // Verify the response
            assertThat(response.statusCode()).isEqualTo(HTTP_NOT_FOUND);
            context.completeNow();
          })));
    }

    @Nested
    class WithConsolidatedTime {

      @BeforeEach
      void setUpConsolidatedTime(TestKit testKit) {
        // Commit two blocks for time oracle to update consolidated time. Two blocks are needed as
        // after the first block time transactions are generated and after the second one they are
        // processed
        testKit.createBlock();
        testKit.createBlock();
      }

      @Test
      void getTime(VertxTestContext context) {
        get(TIME_PATH)
            .send(context.succeeding(response -> context.verify(() -> {
              // Verify the response
              assertThat(response.statusCode()).isEqualTo(HTTP_OK);
              TimeDto time = json().fromJson(response.bodyAsString(), TimeDto.class);

              TimeDto expected = new TimeDto(INITIAL_TIME);
              assertThat(time).isEqualTo(expected);

              context.completeNow();
            })));
      }

      @Test
      void getValidatorsTime(TestKit testKit, VertxTestContext context) {
        get(VALIDATORS_TIMES_PATH)
            .send(context.succeeding(response -> context.verify(() -> {
              Map<PublicKey, ZonedDateTime> actual = json().fromJson(response.bodyAsString(),
                  new TypeToken<Map<PublicKey, ZonedDateTime>>() {
                  }.getType());

              EmulatedNode emulatedNode = testKit.getEmulatedNode();
              PublicKey nodePublicKey = emulatedNode.getServiceKeyPair().getPublicKey();
              Map<PublicKey, ZonedDateTime> expected = ImmutableMap.of(nodePublicKey, INITIAL_TIME);

              assertThat(actual).isEqualTo(expected);

              context.completeNow();
            })));
      }
    }

    private HttpRequest<Buffer> post(String relativePath) {
      String path = fullRequestPath(relativePath);
      return webClient.post(port, HOST, path);
    }

    private HttpRequest<Buffer> get(String relativePath) {
      String path = fullRequestPath(relativePath);
      return webClient.get(port, HOST, path);
    }

    private String fullRequestPath(String relativePath) {
      return PATH_PREFIX + relativePath;
    }
  }
}
