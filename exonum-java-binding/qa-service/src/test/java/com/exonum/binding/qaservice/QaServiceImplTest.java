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

import static com.exonum.binding.common.hash.Hashing.sha256;
import static com.exonum.binding.common.serialization.json.JsonSerializer.json;
import static com.exonum.binding.qaservice.ApiController.QaPaths.GET_CONSENSUS_CONFIGURATION_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.SUBMIT_INCREMENT_COUNTER_TX_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.SUBMIT_UNKNOWN_TX_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.TIME_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.VALIDATORS_TIMES_PATH;
import static com.exonum.binding.qaservice.ApiControllerTest.multiMap;
import static com.exonum.binding.qaservice.QaArtifactInfo.QA_SERVICE_ID;
import static com.exonum.binding.qaservice.QaArtifactInfo.QA_SERVICE_NAME;
import static com.exonum.binding.qaservice.QaServiceImpl.AFTER_COMMIT_COUNTER_NAME;
import static com.exonum.binding.qaservice.QaServiceImpl.DEFAULT_COUNTER_NAME;
import static com.exonum.binding.qaservice.TransactionMessages.createCreateCounterTx;
import static com.exonum.binding.qaservice.TransactionMessages.createIncrementCounterTx;
import static com.exonum.binding.qaservice.TransactionMessages.createUnknownTx;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.blockchain.ExecutionStatuses;
import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.core.blockchain.Blockchain;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.storage.indices.MapIndex;
import com.exonum.binding.qaservice.Config.InitialConfiguration;
import com.exonum.binding.qaservice.Service.ConfigChange;
import com.exonum.binding.qaservice.Service.ConfigPropose;
import com.exonum.binding.qaservice.Service.ServiceConfig;
import com.exonum.binding.testkit.EmulatedNode;
import com.exonum.binding.testkit.FakeTimeProvider;
import com.exonum.binding.testkit.TestKit;
import com.exonum.binding.testkit.TestKitExtension;
import com.exonum.binding.testkit.TimeProvider;
import com.exonum.core.messages.Blockchain.Config;
import com.exonum.core.messages.Runtime.ExecutionStatus;
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
import org.junit.jupiter.api.Disabled;
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
            InitialConfiguration.newBuilder()
                .setTimeOracleName(timeServiceName)
                .build())
        .withTimeService(timeServiceName, 2, TimeProvider.systemTime())
        .build()) {
      Snapshot view = testKit.getSnapshot();
      QaSchema schema = new QaSchema(view, serviceName);
      // Check the time dependency is saved
      Optional<String> timeService = schema.timeOracleName().toOptional();
      assertThat(timeService).hasValue(timeServiceName);

      // Check that both the default and afterCommit counters were created.
      MapIndex<HashCode, Long> counters = schema.counters();
      MapIndex<HashCode, String> counterNames = schema.counterNames();

      HashCode defaultCounterId = sha256().hashString(DEFAULT_COUNTER_NAME, UTF_8);
      HashCode afterCommitCounterId = sha256().hashString(AFTER_COMMIT_COUNTER_NAME, UTF_8);

      assertThat(counters.get(defaultCounterId)).isEqualTo(0L);
      assertThat(counterNames.get(defaultCounterId)).isEqualTo(DEFAULT_COUNTER_NAME);
      assertThat(counters.get(afterCommitCounterId)).isEqualTo(0L);
      assertThat(counterNames.get(afterCommitCounterId)).isEqualTo(AFTER_COMMIT_COUNTER_NAME);
    }
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
            InitialConfiguration.newBuilder()
                .setTimeOracleName(timeServiceName)
                .build());

    assertThrows(RuntimeException.class, testKitBuilder::build);
  }

  @Test
  void reconfigureService(TestKit testKit) {
    KeyPair serviceKeyPair = testKit.getEmulatedNode().getServiceKeyPair();
    // Determine the config application height
    long nextHeight = 1 + testKit.applySnapshot(s -> Blockchain.newInstance(s)
        .getHeight());

    // Propose the config change
    String newTimeOracleName = "mars-time";
    ConfigPropose propose = ConfigPropose.newBuilder()
        .setActualFrom(nextHeight)
        .addChanges(ConfigChange.newBuilder()
            .setService(ServiceConfig.newBuilder()
                .setInstanceId(QA_SERVICE_ID)
                .setParams(InitialConfiguration.newBuilder()
                    .setTimeOracleName(newTimeOracleName)
                    .build()
                    .toByteString())
                .build())
            .build())
        .build();
    TransactionMessage proposeTx = TransactionMessage.builder()
        // fixme: constants
        .serviceId(0)
        .transactionId(2)
        .payload(propose.toByteArray())
        .sign(serviceKeyPair);
    testKit.createBlockWithTransactions(proposeTx);

    // Check the proposal status
    Snapshot s = testKit.getSnapshot();
    Optional<ExecutionStatus> proposalStatus = Blockchain.newInstance(s)
        .getTxResult(proposeTx.hash());
    assertThat(proposalStatus).hasValue(ExecutionStatuses.success());

    // Check the application status
    QaSchema qaSchema = new QaSchema(s, QA_SERVICE_NAME);
    Optional<String> actualTimeOracle = qaSchema.timeOracleName().toOptional();
    assertThat(actualTimeOracle).hasValue(newTimeOracleName);
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

  private void checkAfterCommitCounter(TestKit testKit, long expectedValue) {
    Snapshot view = testKit.getSnapshot();
    QaSchema schema = new QaSchema(view, QA_SERVICE_NAME);
    MapIndex<HashCode, Long> counters = schema.counters();
    MapIndex<HashCode, String> counterNames = schema.counterNames();
    HashCode counterId = sha256().hashString(AFTER_COMMIT_COUNTER_NAME, UTF_8);

    assertThat(counters.get(counterId)).isEqualTo(expectedValue);
    assertThat(counterNames.get(counterId)).isEqualTo(AFTER_COMMIT_COUNTER_NAME);
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
      HashCode counterId = sha256().hashString(counterName, UTF_8);
      long seed = 17L;
      MultiMap params = multiMap("counterId", counterId.toString(),
          "seed", Long.toString(seed));

      post(SUBMIT_INCREMENT_COUNTER_TX_PATH)
          .sendForm(params, context.succeeding(response -> context.verify(() -> {
            // Check response
            assertThat(response.statusCode()).isEqualTo(HTTP_CREATED);

            // Verify the service submitted a transaction
            List<TransactionMessage> transactionPool = testKit.getTransactionPool();

            KeyPair expectedAuthor = testKit.getEmulatedNode().getServiceKeyPair();
            TransactionMessage expectedMessage = createIncrementCounterTx(seed, counterId,
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
      HashCode counterId = sha256().hashString(counterName, UTF_8);
      get("counter/" + counterId)
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
      HashCode counterId = sha256().hashString("Unknown counter", UTF_8);
      // Check there is no such counter
      get("counter/" + counterId)
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
    @Disabled("Disabled till proof map keys are fixed")
    void getTimeNotYetAvailable(VertxTestContext context) {
      get(TIME_PATH)
          .send(context.succeeding(response -> context.verify(() -> {
            // Verify the response
            assertThat(response.statusCode()).isEqualTo(HTTP_NOT_FOUND);
            context.completeNow();
          })));
    }

    @Disabled("Disabled till proof map keys are fixed")
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
