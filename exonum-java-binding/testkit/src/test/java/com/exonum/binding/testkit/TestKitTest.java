/*
 * Copyright 2019 The Exonum Team
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

package com.exonum.binding.testkit;

import static com.exonum.binding.common.blockchain.ExecutionStatuses.success;
import static com.exonum.binding.testkit.TestKit.MAX_SERVICE_INSTANCE_ID;
import static com.exonum.binding.testkit.TestService.THROWING_VALUE;
import static com.exonum.binding.testkit.TestService.constructAfterCommitTransaction;
import static com.exonum.binding.testkit.TestTransaction.BODY_CHARSET;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.crypto.CryptoFunction;
import com.exonum.binding.common.crypto.CryptoFunctions;
import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.core.blockchain.Block;
import com.exonum.binding.core.blockchain.Blockchain;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.storage.database.View;
import com.exonum.binding.core.storage.indices.MapIndex;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.core.transaction.RawTransaction;
import com.exonum.binding.testkit.TestProtoMessages.TestConfiguration;
import com.exonum.binding.time.TimeSchema;
import com.exonum.core.messages.Blockchain.Config;
import com.exonum.core.messages.Blockchain.ValidatorKeys;
import com.exonum.core.messages.Runtime.ExecutionStatus;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TestKitTest extends TestKitTestWithArtifactsCreated {
  private static final String TIME_SERVICE_NAME = "time-service";
  private static final int TIME_SERVICE_ID = 10;

  private static final CryptoFunction CRYPTO_FUNCTION = CryptoFunctions.ed25519();
  private static final KeyPair KEY_PAIR = CRYPTO_FUNCTION.generateKeyPair();
  private static final ZonedDateTime TIME =
      ZonedDateTime.of(2000, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC);

  @RegisterExtension
  TestKitExtension testKitExtension = new TestKitExtension(
      TestKit.builder()
          .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
          .withService(ARTIFACT_ID, SERVICE_NAME, SERVICE_ID, SERVICE_CONFIGURATION)
          .withArtifactsDirectory(artifactsDirectory));

  @Test
  @Disabled("Disabled until ProofMapIndexProxy 32 byte key restriction is relaxed")
  void createTestKitForSingleServiceWithDefaultConfiguration() {
    // Deploy service that ignores configuration and should initialize correctly
    // with the default one
    try (TestKit testKit = TestKit.forService(ARTIFACT_ID_2, ARTIFACT_FILENAME_2,
        SERVICE_NAME_2, SERVICE_ID_2, artifactsDirectory)) {
      checkTestService2Initialization(testKit, SERVICE_NAME_2, SERVICE_ID_2);
    }
  }

  @Test
  @Disabled("Disabled until ProofMapIndexProxy 32 byte key restriction is relaxed")
  void createTestKitWithBuilderForSingleService() {
    try (TestKit testKit = TestKit.builder()
        .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
        .withService(ARTIFACT_ID, SERVICE_NAME, SERVICE_ID, SERVICE_CONFIGURATION)
        .withArtifactsDirectory(artifactsDirectory)
        .build()) {
      checkTestServiceInitialization(testKit, SERVICE_NAME, SERVICE_ID);
    }
  }

  @Test
  @Disabled("Disabled until ProofMapIndexProxy 32 byte key restriction is relaxed")
  void createTestKitWithTwoServiceInstancesSameArtifact() {
    try (TestKit testKit = TestKit.builder()
        .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
        .withService(ARTIFACT_ID, SERVICE_NAME, SERVICE_ID, SERVICE_CONFIGURATION)
        .withService(ARTIFACT_ID, SERVICE_NAME_2, SERVICE_ID_2, SERVICE_CONFIGURATION)
        .withArtifactsDirectory(artifactsDirectory)
        .build()) {
      checkTestServiceInitialization(testKit, SERVICE_NAME, SERVICE_ID);
      checkTestServiceInitialization(testKit, SERVICE_NAME_2, SERVICE_ID_2);
    }
  }

  @Test
  void createTestKitWithDeployedArtifactWithoutCreatedServicesThrows() {
    Class<IllegalArgumentException> exceptionType = IllegalArgumentException.class;
    TestKit.Builder testKitBuilder = TestKit.builder()
        .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
        .withArtifactsDirectory(artifactsDirectory);
    IllegalArgumentException thrownException = assertThrows(exceptionType, testKitBuilder::build);
    assertThat(thrownException.getMessage())
        .isEqualTo("Following service artifacts were deployed, but not used for"
            + " service instantiation: [%s]", ARTIFACT_ID.toString());
  }

  @Test
  void createTestKitWithoutDeployedArtifactThrows() {
    Class<IllegalArgumentException> exceptionType = IllegalArgumentException.class;
    TestKit.Builder testKitBuilder = TestKit.builder()
        .withArtifactsDirectory(artifactsDirectory);
    IllegalArgumentException thrownException = assertThrows(exceptionType,
        () -> testKitBuilder.withService(ARTIFACT_ID, SERVICE_NAME, SERVICE_ID,
            SERVICE_CONFIGURATION));
    assertThat(thrownException.getMessage())
        .isEqualTo("Service %s should be deployed first in order to be created",
        ARTIFACT_ID.toString());
  }

  @Test
  void createTestKitWithDifferentDeployedArtifactAndCreatedServiceThrows() {
    Class<IllegalArgumentException> exceptionType = IllegalArgumentException.class;
    TestKit.Builder testKitBuilder = TestKit.builder()
        .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
        .withArtifactsDirectory(artifactsDirectory);
    IllegalArgumentException thrownException = assertThrows(exceptionType,
        () -> testKitBuilder.withService(ARTIFACT_ID_2, SERVICE_NAME_2, SERVICE_ID_2));
    assertThat(thrownException.getMessage())
        .isEqualTo("Service %s should be deployed first in order to be created",
        ARTIFACT_ID_2.toString());
  }

  @Test
  void createTestKitWithoutArtifactsDirectoryThrows() {
    Class<IllegalStateException> exceptionType = IllegalStateException.class;
    TestKit.Builder testKitBuilder = TestKit.builder()
        .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
        .withService(ARTIFACT_ID, SERVICE_NAME, SERVICE_ID, SERVICE_CONFIGURATION);
    IllegalStateException thrownException = assertThrows(exceptionType, testKitBuilder::build);
    assertThat(thrownException.getMessage()).isEqualTo("Artifacts directory was not set.");
  }

  @Test
  void createTestKitWithNoFileThrows() {
    String nonexistentArtifactFilename = "nonexistent-artifact.jar";
    Class<RuntimeException> exceptionType = RuntimeException.class;
    TestKit.Builder testKitBuilder = TestKit.builder()
        .withDeployedArtifact(ARTIFACT_ID, nonexistentArtifactFilename)
        .withService(ARTIFACT_ID, SERVICE_NAME, SERVICE_ID, SERVICE_CONFIGURATION)
        .withArtifactsDirectory(artifactsDirectory);
    RuntimeException thrownException = assertThrows(exceptionType, testKitBuilder::build);
    assertThat(thrownException.getMessage())
        .contains("Failed to load the service from ", nonexistentArtifactFilename);
  }

  @Test
  void createTestKitWithInvalidArtifactThrows(@TempDir Path directory) throws Exception {
    String invalidArtifactFilename = "invalid-artifact.jar";
    createInvalidArtifact(directory, invalidArtifactFilename);
    Class<RuntimeException> exceptionType = RuntimeException.class;
    TestKit.Builder testKitBuilder = TestKit.builder()
        .withDeployedArtifact(ARTIFACT_ID, invalidArtifactFilename)
        .withService(ARTIFACT_ID, SERVICE_NAME, SERVICE_ID, SERVICE_CONFIGURATION)
        .withArtifactsDirectory(artifactsDirectory);
    RuntimeException thrownException = assertThrows(exceptionType, testKitBuilder::build);
    assertThat(thrownException.getMessage()).contains("Unable to create blockchain instance");
  }

  @Test
  void createTestKitWithCustomConfiguration() {
    String configurationValue = "Custom value";
    TestConfiguration testConfiguration = TestConfiguration.newBuilder()
        .setValue(configurationValue)
        .build();
    try (TestKit testKit = TestKit.builder()
        .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
        .withService(ARTIFACT_ID, SERVICE_NAME, SERVICE_ID, testConfiguration)
        .withArtifactsDirectory(artifactsDirectory)
        .build()) {
      // Check that configuration value is used in initialization
      Snapshot view = testKit.getSnapshot();
      TestSchema testSchema = new TestSchema(view, SERVICE_ID);
      ProofMapIndexProxy<HashCode, String> testProofMap = testSchema.testMap();
      Map<HashCode, String> testMap = toMap(testProofMap);
      Map<HashCode, String> expected = ImmutableMap.of(
          TestService.INITIAL_ENTRY_KEY, configurationValue);
      assertThat(testMap).isEqualTo(expected);
    }
  }

  @Test
  void createTestKitWithThrowingInitialization() {
    TestConfiguration invalidConfiguration = TestConfiguration.newBuilder()
        .setValue(THROWING_VALUE)
        .build();
    Class<RuntimeException> exceptionType = RuntimeException.class;
    TestKit.Builder testKitBuilder = TestKit.builder()
        .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
        // Initialize with special invalid configuration
        .withService(ARTIFACT_ID, SERVICE_NAME, SERVICE_ID, invalidConfiguration)
        .withArtifactsDirectory(artifactsDirectory);
    RuntimeException thrownException = assertThrows(exceptionType, testKitBuilder::build);
    assertThat(thrownException.getMessage())
        .contains("Service configuration had an invalid value:", THROWING_VALUE);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, MAX_SERVICE_INSTANCE_ID + 1})
  void createTestKitWithInvalidServiceId(int invalidServiceId) {
    Class<IllegalArgumentException> exceptionType = IllegalArgumentException.class;
    TestKit.Builder testKitBuilder = TestKit.builder()
        .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
        .withArtifactsDirectory(artifactsDirectory);
    assertThrows(exceptionType,
        () -> testKitBuilder.withService(ARTIFACT_ID, SERVICE_NAME, invalidServiceId));
  }

  @Test
  @Disabled("Disabled until ProofMapIndexProxy 32 byte key restriction is relaxed")
  void createTestKitWithBuilderForMultipleDifferentServices() {
    try (TestKit testKit = TestKit.builder()
        .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
        .withService(ARTIFACT_ID, SERVICE_NAME, SERVICE_ID, SERVICE_CONFIGURATION)
        .withDeployedArtifact(ARTIFACT_ID_2, ARTIFACT_FILENAME_2)
        .withService(ARTIFACT_ID_2, SERVICE_NAME_2, SERVICE_ID_2)
        .withArtifactsDirectory(artifactsDirectory)
        .build()) {
      checkTestServiceInitialization(testKit, SERVICE_NAME, SERVICE_ID);
      checkTestService2Initialization(testKit, SERVICE_NAME_2, SERVICE_ID_2);
    }
  }

  @Test
  @Disabled("Disabled until ProofMapIndexProxy 32 byte key restriction is relaxed")
  void createTestKitWithTimeService() {
    TimeProvider timeProvider = FakeTimeProvider.create(TIME);
    try (TestKit testKit = TestKit.builder()
        .withTimeService(TIME_SERVICE_NAME, TIME_SERVICE_ID, timeProvider)
        .withArtifactsDirectory(artifactsDirectory)
        .build()) {
      checkIfServiceEnabled(testKit, TIME_SERVICE_NAME, TIME_SERVICE_ID);
    }
  }

  private void checkTestServiceInitialization(TestKit testKit, String serviceName, int serviceId) {
    Snapshot view = testKit.getSnapshot();
    // Check that genesis block was committed
    checkGenesisBlockCommit(view);

    // Check that service appears in dispatcher schema
    checkIfServiceEnabled(testKit, serviceName, serviceId);

    // Check that initialization changed database state
    TestSchema testSchema = new TestSchema(view, serviceId);
    ProofMapIndexProxy<HashCode, String> testProofMap = testSchema.testMap();
    Map<HashCode, String> testMap = toMap(testProofMap);
    Map<HashCode, String> expected = ImmutableMap.of(
        TestService.INITIAL_ENTRY_KEY, CONFIGURATION_VALUE);
    assertThat(testMap).isEqualTo(expected);
  }

  private void checkTestService2Initialization(TestKit testKit, String serviceName,
                                               int serviceId) {
    // Check that genesis block was committed
    checkGenesisBlockCommit(testKit.getSnapshot());

    // Check that service appears in dispatcher schema
    checkIfServiceEnabled(testKit, serviceName, serviceId);
  }

  private void checkGenesisBlockCommit(Snapshot view) {
    Blockchain blockchain = Blockchain.newInstance(view);
    assertThat(blockchain.getBlockHashes().size()).isEqualTo(1L);
  }

  @Test
  void createTestKitWithSeveralValidators() {
    short validatorCount = 2;
    try (TestKit testKit = TestKit.builder()
        .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
        .withService(ARTIFACT_ID, SERVICE_NAME, SERVICE_ID, SERVICE_CONFIGURATION)
        .withValidators(validatorCount)
        .withArtifactsDirectory(artifactsDirectory)
        .build()) {
      Snapshot view = testKit.getSnapshot();
      Blockchain blockchain = Blockchain.newInstance(view);
      assertThat(blockchain.getConsensusConfiguration().getValidatorKeysCount())
          .isEqualTo(validatorCount);
    }
  }

  @Test
  void createTestKitWithAuditorAndAdditionalValidators() {
    short validatorCount = 2;
    try (TestKit testKit = TestKit.builder()
        .withNodeType(EmulatedNodeType.AUDITOR)
        .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
        .withService(ARTIFACT_ID, SERVICE_NAME, SERVICE_ID, SERVICE_CONFIGURATION)
        .withValidators(validatorCount)
        .withArtifactsDirectory(artifactsDirectory)
        .build()) {
      Snapshot view = testKit.getSnapshot();
      Blockchain blockchain = Blockchain.newInstance(view);
      assertThat(blockchain.getConsensusConfiguration().getValidatorKeysCount())
          .isEqualTo(validatorCount);
    }
  }

  @Test
  void setInvalidValidatorCount() {
    Class<IllegalArgumentException> exceptionType = IllegalArgumentException.class;
    short invalidValidatorCount = 0;
    TestKit.Builder testKitBuilder = TestKit.builder()
        .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
        .withService(ARTIFACT_ID, SERVICE_NAME, SERVICE_ID, SERVICE_CONFIGURATION)
        .withArtifactsDirectory(artifactsDirectory);
    assertThrows(exceptionType, () -> testKitBuilder.withValidators(invalidValidatorCount));
  }

  @Test
  void createTestKitMoreThanMaxServiceNumber() {
    Class<IllegalArgumentException> exceptionType = IllegalArgumentException.class;
    TestKit.Builder testKitBuilder = TestKit.builder()
            .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
            .withArtifactsDirectory(artifactsDirectory);
    for (int i = 0; i < TestKit.MAX_SERVICE_NUMBER + 1; i++) {
      String serviceName = SERVICE_NAME + i;
      int serviceId = SERVICE_ID + i;
      testKitBuilder = testKitBuilder.withService(ARTIFACT_ID, serviceName, serviceId);
    }
    assertThrows(exceptionType, testKitBuilder::build);
  }

  @Test
  void createTestKitWithoutServices() {
    try (TestKit testKit = TestKit.builder()
        .withArtifactsDirectory(artifactsDirectory)
        .build()) {
      // Shouldn't throw
    }
  }

  @Test
  void createEmptyBlock(TestKit testKit) {
    Block block = testKit.createBlock();
    assertThat(block.getNumTransactions()).isEqualTo(0);

    Snapshot view = testKit.getSnapshot();
    Blockchain blockchain = Blockchain.newInstance(view);
    assertThat(blockchain.getHeight()).isEqualTo(1);
    assertThat(block).isEqualTo(blockchain.getBlock(1));
  }

  @Test
  void afterCommitSubmitsTransaction(TestKit testKit) {
    // Create a block so that afterCommit transaction is submitted
    Block block = testKit.createBlock();
    List<TransactionMessage> inPoolTransactions = testKit
        .findTransactionsInPool(tx -> tx.getServiceId() == SERVICE_ID);
    assertThat(inPoolTransactions).hasSize(1);
    TransactionMessage inPoolTransaction = inPoolTransactions.get(0);
    RawTransaction afterCommitTransaction =
        constructAfterCommitTransaction(SERVICE_ID, block.getHeight());

    assertThat(inPoolTransaction.getServiceId())
        .isEqualTo(afterCommitTransaction.getServiceId());
    assertThat(inPoolTransaction.getTransactionId())
        .isEqualTo(afterCommitTransaction.getTransactionId());
    ByteString expectedPayload = ByteString.copyFrom(afterCommitTransaction.getPayload());
    assertThat(inPoolTransaction.getPayload()).isEqualTo(expectedPayload);

    Block nextBlock = testKit.createBlock();
    assertThat(nextBlock.getNumTransactions()).isEqualTo(1);
    assertThat(nextBlock.getHeight()).isEqualTo(2);
  }

  @Test
  void createBlockWithTransactionIgnoresInPoolTransactions(TestKit testKit) {
    // Create a block so that afterCommit transaction is submitted
    testKit.createBlock();

    Block block = testKit.createBlockWithTransactions();
    assertThat(block.getNumTransactions()).isEqualTo(0);

    // Two blocks were created, so two afterCommit transactions should be submitted into pool
    List<TransactionMessage> inPoolTransactions = testKit
        .findTransactionsInPool(tx -> tx.getServiceId() == SERVICE_ID);
    assertThat(inPoolTransactions).hasSize(2);
  }

  @Test
  void getTransactionPool(TestKit testKit) {
    // Create two blocks with no transactions, so two afterCommit transactions are stored in
    // the transaction pool
    Block block1 = testKit.createBlock();
    // Use #createBlockWithTransactions() so that an empty block is created and first afterCommit
    // transaction stays in pool
    Block block2 = testKit.createBlockWithTransactions();
    RawTransaction afterCommitTransaction1 =
        constructAfterCommitTransaction(SERVICE_ID, block1.getHeight());
    RawTransaction afterCommitTransaction2 =
        constructAfterCommitTransaction(SERVICE_ID, block2.getHeight());
    List<RawTransaction> rawTransactionsInPool = testKit.getTransactionPool().stream()
        .map(RawTransaction::fromMessage)
        .collect(toList());
    assertThat(rawTransactionsInPool)
        .containsExactlyInAnyOrder(afterCommitTransaction1, afterCommitTransaction2);
  }

  @Test
  void createBlockWithSingleTransaction(TestKit testKit) {
    TransactionMessage message = constructTestTransactionMessage("Test message");
    Block block = testKit.createBlockWithTransactions(message);
    assertThat(block.getNumTransactions()).isEqualTo(1);

    Snapshot view = testKit.getSnapshot();
    Blockchain blockchain = Blockchain.newInstance(view);
    assertThat(blockchain.getHeight()).isEqualTo(1);
    assertThat(block).isEqualTo(blockchain.getBlock(1));
    Map<HashCode, ExecutionStatus> transactionResults = toMap(blockchain.getTxResults());
    assertThat(transactionResults).hasSize(1);
    ExecutionStatus transactionResult = transactionResults.get(message.hash());
    assertThat(transactionResult).isEqualTo(success());
  }

  @Test
  void createBlockWithTransactions(TestKit testKit) {
    TransactionMessage message = constructTestTransactionMessage("Test message");
    TransactionMessage message2 = constructTestTransactionMessage("Test message 2");

    Block block = testKit.createBlockWithTransactions(ImmutableList.of(message, message2));
    assertThat(block.getNumTransactions()).isEqualTo(2);

    testKit.withSnapshot((view) -> checkTransactionsCommittedSuccessfully(
        view, block, message, message2));
  }

  @Test
  void createBlockWithTransactionsVarargs(TestKit testKit) {
    TransactionMessage message = constructTestTransactionMessage("Test message");
    TransactionMessage message2 = constructTestTransactionMessage("Test message 2");

    Block block = testKit.createBlockWithTransactions(message, message2);
    assertThat(block.getNumTransactions()).isEqualTo(2);

    testKit.withSnapshot((view) -> checkTransactionsCommittedSuccessfully(
        view, block, message, message2));
  }

  private TransactionMessage constructTestTransactionMessage(String payload) {
    return constructTestTransactionMessage(payload, KEY_PAIR);
  }

  private TransactionMessage constructTestTransactionMessage(String payload, KeyPair keyPair) {
    return TransactionMessage.builder()
        .serviceId(SERVICE_ID)
        .transactionId(TestTransaction.ID)
        .payload(payload.getBytes(BODY_CHARSET))
        .sign(keyPair, CRYPTO_FUNCTION);
  }

  private void checkTransactionsCommittedSuccessfully(
      View view, Block block, TransactionMessage message, TransactionMessage message2) {
    Blockchain blockchain = Blockchain.newInstance(view);
    assertThat(blockchain.getHeight()).isEqualTo(1);
    assertThat(block).isEqualTo(blockchain.getBlock(1));
    Map<HashCode, ExecutionStatus> transactionResults = toMap(blockchain.getTxResults());
    assertThat(transactionResults).hasSize(2);

    ExecutionStatus transactionResult = transactionResults.get(message.hash());
    assertThat(transactionResult).isEqualTo(success());
    ExecutionStatus transactionResult2 = transactionResults.get(message2.hash());
    assertThat(transactionResult2).isEqualTo(success());
  }

  @Test
  void createBlockWithTransactionWithWrongServiceId(TestKit testKit) {
    short wrongServiceId = SERVICE_ID + 1;
    TransactionMessage message = TransactionMessage.builder()
        .serviceId(wrongServiceId)
        .transactionId(TestTransaction.ID)
        .payload("Test message".getBytes(BODY_CHARSET))
        .sign(KEY_PAIR, CRYPTO_FUNCTION);
    IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class,
        () -> testKit.createBlockWithTransactions(message));
    String expectedMessage = String.format("No service with id=%s in the Java runtime",
        wrongServiceId);
    assertThat(thrownException.getCause().getMessage()).contains(expectedMessage);
  }

  @Test
  void createBlockWithTransactionWithWrongTransactionId(TestKit testKit) {
    short wrongTransactionId = (short) (TestTransaction.ID + 1);
    TransactionMessage message = TransactionMessage.builder()
        .serviceId(SERVICE_ID)
        .transactionId(wrongTransactionId)
        .payload("Test message".getBytes(BODY_CHARSET))
        .sign(KEY_PAIR, CRYPTO_FUNCTION);
    IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class,
        () -> testKit.createBlockWithTransactions(message));
    assertThat(thrownException.getMessage())
        .contains("failed to convert transaction", Integer.toString(SERVICE_ID),
            message.toString());
  }

  @Test
  void getValidatorEmulatedNode(TestKit testKit) {
    EmulatedNode node = testKit.getEmulatedNode();
    assertThat(node.getNodeType()).isEqualTo(EmulatedNodeType.VALIDATOR);
    assertThat(node.getValidatorId()).isNotEmpty();

    Snapshot view = testKit.getSnapshot();
    Blockchain blockchain = Blockchain.newInstance(view);
    Config configuration = blockchain.getConsensusConfiguration();

    // Check the public service key of the emulated node is included
    List<PublicKey> serviceKeys = configuration.getValidatorKeysList().stream()
        .map(ValidatorKeys::getServiceKey)
        .map(key -> PublicKey.fromBytes(key.getData().toByteArray()))
        .collect(toList());
    PublicKey emulatedNodeServiceKey = node.getServiceKeyPair().getPublicKey();
    List<PublicKey> expectedKeys = ImmutableList.of(emulatedNodeServiceKey);
    assertThat(serviceKeys).isEqualTo(expectedKeys);
  }

  @Test
  void getAuditorEmulatedNode() {
    try (TestKit testKit = TestKit.builder()
        .withNodeType(EmulatedNodeType.AUDITOR)
        .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
        .withService(ARTIFACT_ID, SERVICE_NAME, SERVICE_ID, SERVICE_CONFIGURATION)
        .withArtifactsDirectory(artifactsDirectory)
        .build()) {
      EmulatedNode node = testKit.getEmulatedNode();
      assertThat(node.getNodeType()).isEqualTo(EmulatedNodeType.AUDITOR);
      assertThat(node.getValidatorId()).isEmpty();
      assertThat(node.getServiceKeyPair()).isNotNull();
    }
  }

  @Test
  @Disabled("Till ProofMap in hashing flavour is implemented: ECR-3779")
  void timeServiceWorksInTestKit() {
    FakeTimeProvider timeProvider = FakeTimeProvider.create(TIME);
    try (TestKit testKit = TestKit.builder()
        .withTimeService(TIME_SERVICE_NAME, TIME_SERVICE_ID, timeProvider)
        .withArtifactsDirectory(artifactsDirectory)
        .build()) {
      // Commit two blocks for time oracle to prepare consolidated time. Two blocks are needed as
      // after the first block time transactions are generated and after the second one they are
      // processed
      testKit.createBlock();
      testKit.createBlock();
      testKit.withSnapshot((view) -> {
        TimeSchema timeSchema = TimeSchema.newInstance(view, TIME_SERVICE_NAME);
        Optional<ZonedDateTime> consolidatedTime = timeSchema.getTime().toOptional();
        assertThat(consolidatedTime).contains(TIME);

        // Check that validatorsTimes contains one exactly entry with TestKit emulated node's
        // public key and time provider's time
        checkValidatorsTimes(timeSchema, testKit, TIME);
      });

      // Update time in time provider
      ZonedDateTime newTime = TIME.plusDays(1);
      timeProvider.setTime(newTime);
      // Commit two blocks for time oracle to update consolidated time. Two blocks are needed as
      // after the first block time transactions are generated and after the second one they are
      // processed
      testKit.createBlock();
      testKit.createBlock();
      testKit.withSnapshot((view) -> {
        TimeSchema timeSchema = TimeSchema.newInstance(view, TIME_SERVICE_NAME);
        Optional<ZonedDateTime> consolidatedTime = timeSchema.getTime().toOptional();
        assertThat(consolidatedTime).contains(newTime);
      });
    }
  }

  @Test
  void createTestKitWithTimeServiceAndTooManyValidators() {
    TimeProvider timeProvider = FakeTimeProvider.create(TIME);
    Class<IllegalArgumentException> exceptionType = IllegalArgumentException.class;
    short invalidValidatorCount = TestKit.MAX_VALIDATOR_COUNT_WITH_ENABLED_TIME_SERVICE + 1;
    TestKit.Builder testKitBuilder = TestKit.builder()
        .withTimeService(TIME_SERVICE_NAME, TIME_SERVICE_ID, timeProvider)
        .withValidators(invalidValidatorCount);
    IllegalArgumentException thrownException = assertThrows(exceptionType, testKitBuilder::build);
    String expectedMessage = String.format("Number of validators (%s) should be less than or equal"
        + " to %s when TimeService is instantiated.",
        invalidValidatorCount, TestKit.MAX_VALIDATOR_COUNT_WITH_ENABLED_TIME_SERVICE);
    assertThat(thrownException).hasMessageContaining(expectedMessage);
  }

  @Test
  void getSnapshot(TestKit testKit) {
    Class<IllegalStateException> exceptionType = IllegalStateException.class;
    Snapshot view1 = testKit.getSnapshot();
    Snapshot view2 = testKit.getSnapshot();
    Cleaner snapshotCleaner = testKit.snapshotCleaner;
    assertThat(snapshotCleaner.getNumRegisteredActions()).isEqualTo(2);

    testKit.close();

    // Verify that snapshot proxies were closed
    assertThrows(exceptionType, view1::getViewNativeHandle);
    assertThrows(exceptionType, view2::getViewNativeHandle);
  }

  private void checkValidatorsTimes(
      TimeSchema timeSchema, TestKit testKit, ZonedDateTime expectedTime) {
    Map<PublicKey, ZonedDateTime> validatorsTimes = toMap(timeSchema.getValidatorsTimes());
    EmulatedNode emulatedNode = testKit.getEmulatedNode();
    PublicKey nodePublicKey = emulatedNode.getServiceKeyPair().getPublicKey();
    Map<PublicKey, ZonedDateTime> expected = ImmutableMap.of(nodePublicKey, expectedTime);
    assertThat(validatorsTimes).isEqualTo(expected);
  }

  private <K, V> Map<K, V> toMap(MapIndex<K, V> mapIndex) {
    return Maps.toMap(mapIndex.keys(), mapIndex::get);
  }
}
