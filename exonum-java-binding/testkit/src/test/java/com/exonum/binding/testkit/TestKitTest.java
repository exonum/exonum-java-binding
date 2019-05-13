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

import static com.exonum.binding.testkit.TestService.constructAfterCommitTransaction;
import static com.exonum.binding.testkit.TestTransaction.BODY_CHARSET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import com.exonum.binding.blockchain.Block;
import com.exonum.binding.blockchain.Blockchain;
import com.exonum.binding.common.blockchain.TransactionResult;
import com.exonum.binding.common.crypto.CryptoFunction;
import com.exonum.binding.common.crypto.CryptoFunctions;
import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.service.AbstractServiceModule;
import com.exonum.binding.service.InternalServerError;
import com.exonum.binding.service.Node;
import com.exonum.binding.service.Service;
import com.exonum.binding.service.ServiceModule;
import com.exonum.binding.service.TransactionConverter;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.time.TimeSchema;
import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.transaction.Transaction;
import com.exonum.binding.util.LibraryLoader;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Singleton;
import io.vertx.ext.web.Router;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TestKitTest {

  private static final CryptoFunction CRYPTO_FUNCTION = CryptoFunctions.ed25519();
  private static final ZonedDateTime TIME =
      ZonedDateTime.of(2000, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC);

  static {
    LibraryLoader.load();
  }

  @Test
  void createTestKitForSingleService() {
    TestService service;
    try (TestKit testKit = TestKit.forService(TestServiceModule.class)) {
      service = testKit.getService(TestService.SERVICE_ID, TestService.class);
      checkTestServiceInitialization(testKit, service);
    }
  }

  @Test
  void createTestKitWithBuilderForSingleService() {
    try (TestKit testKit = TestKit.builder(EmulatedNodeType.VALIDATOR)
        .withService(TestServiceModule.class)
        .build()) {
      TestService service = testKit.getService(TestService.SERVICE_ID, TestService.class);
      checkTestServiceInitialization(testKit, service);
    }
  }

  @Test
  void createTestKitWithBuilderForMultipleSameServices() {
    Class<IllegalArgumentException> exceptionType = IllegalArgumentException.class;
    List<Class<? extends ServiceModule>> serviceModules = ImmutableList.of(TestServiceModule.class,
        TestServiceModule.class);
    TestKit.Builder testKitBuilder = TestKit.builder(EmulatedNodeType.VALIDATOR)
        .withServices(serviceModules);
    assertThrows(exceptionType, testKitBuilder::build);
  }

  @Test
  void createTestKitWithBuilderForMultipleDifferentServices() {
    try (TestKit testKit = TestKit.builder(EmulatedNodeType.VALIDATOR)
        .withService(TestServiceModule.class)
        .withService(TestServiceModule2.class)
        .build()) {
      TestService service = testKit.getService(TestService.SERVICE_ID, TestService.class);
      checkTestServiceInitialization(testKit, service);
      TestService2 service2 = testKit.getService(TestService2.SERVICE_ID, TestService2.class);
      checkTestService2Initialization(testKit, service2);
    }
  }

  @Test
  void createTestKitWithBuilderForMultipleDifferentServicesVarargs() {
    try (TestKit testKit = TestKit.builder(EmulatedNodeType.VALIDATOR)
        .withServices(TestServiceModule.class, TestServiceModule2.class)
        .build()) {
      TestService service = testKit.getService(TestService.SERVICE_ID, TestService.class);
      checkTestServiceInitialization(testKit, service);
      TestService2 service2 = testKit.getService(TestService2.SERVICE_ID, TestService2.class);
      checkTestService2Initialization(testKit, service2);
    }
  }

  @Test
  void createTestKitWithTimeService() {
    TimeProvider timeProvider = FakeTimeProvider.create(TIME);
    try (TestKit testKit = TestKit.builder(EmulatedNodeType.VALIDATOR)
        .withService(TestServiceModule.class)
        .withTimeService(timeProvider)
        .build()) {
      TestService service = testKit.getService(TestService.SERVICE_ID, TestService.class);
      checkTestServiceInitialization(testKit, service);
    }
  }

  private void checkTestServiceInitialization(TestKit testKit, TestService service) {
    // Check that TestKit contains an instance of TestService
    assertThat(service.getId()).isEqualTo(TestService.SERVICE_ID);
    assertThat(service.getName()).isEqualTo(TestService.SERVICE_NAME);

    // Check that TestService API is mounted
    Node serviceNode = service.getNode();
    EmulatedNode emulatedTestKitNode = testKit.getEmulatedNode();
    assertThat(serviceNode.getPublicKey())
        .isEqualTo(emulatedTestKitNode.getServiceKeyPair().getPublicKey());
    testKit.withSnapshot((view) -> {
      // Check that initialization changed database state
      TestSchema testSchema = service.createDataSchema(view);
      ProofMapIndexProxy<HashCode, String> testProofMap = testSchema.testMap();
      Map<HashCode, String> testMap = toMap(testProofMap);
      Map<HashCode, String> expected = ImmutableMap.of(
          TestService.INITIAL_ENTRY_KEY, TestService.INITIAL_ENTRY_VALUE);
      assertThat(testMap).isEqualTo(expected);

      // Check that genesis block was committed
      Blockchain blockchain = Blockchain.newInstance(view);
      assertThat(blockchain.getBlockHashes().size()).isEqualTo(1L);
      return null;
    });
  }

  private void checkTestService2Initialization(TestKit testKit, TestService2 service) {
    // Check that TestKit contains an instance of TestService2
    assertThat(service.getId()).isEqualTo(TestService2.SERVICE_ID);
    assertThat(service.getName()).isEqualTo(TestService2.SERVICE_NAME);

    // Check that TestService2 API is mounted
    Node serviceNode = service.getNode();
    EmulatedNode emulatedTestKitNode = testKit.getEmulatedNode();
    assertThat(serviceNode.getPublicKey())
        .isEqualTo(emulatedTestKitNode.getServiceKeyPair().getPublicKey());
    testKit.withSnapshot((view) -> {
      // Check that genesis block was committed
      Blockchain blockchain = Blockchain.newInstance(view);
      assertThat(blockchain.getBlockHashes().size()).isEqualTo(1L);
      return null;
    });
  }

  @Test
  void createTestKitWithSeveralValidators() {
    short validatorCount = 2;
    try (TestKit testKit = TestKit.builder(EmulatedNodeType.VALIDATOR)
        .withService(TestServiceModule.class)
        .withValidators(validatorCount)
        .build()) {
      testKit.withSnapshot((view) -> {
        Blockchain blockchain = Blockchain.newInstance(view);
        assertThat(blockchain.getActualConfiguration().validatorKeys().size())
            .isEqualTo(validatorCount);
        return null;
      });
    }
  }

  @Test
  void createTestKitWithAuditorAndAdditionalValidators() {
    short validatorCount = 2;
    try (TestKit testKit = TestKit.builder(EmulatedNodeType.AUDITOR)
        .withService(TestServiceModule.class)
        .withValidators(validatorCount)
        .build()) {
      testKit.withSnapshot((view) -> {
        Blockchain blockchain = Blockchain.newInstance(view);
        assertThat(blockchain.getActualConfiguration().validatorKeys().size())
            .isEqualTo(validatorCount);
        return null;
      });
    }
  }

  @Test
  void setInvalidValidatorCount() {
    Class<IllegalArgumentException> exceptionType = IllegalArgumentException.class;
    short invalidValidatorCount = 0;
    TestKit.Builder testKitBuilder = TestKit.builder(EmulatedNodeType.VALIDATOR)
        .withService(TestServiceModule.class);
    assertThrows(exceptionType, () -> testKitBuilder.withValidators(invalidValidatorCount));
  }

  @Test
  void requestWrongServiceClass() {
    Class<IllegalArgumentException> exceptionType = IllegalArgumentException.class;
    try (TestKit testKit = TestKit.builder(EmulatedNodeType.VALIDATOR)
        .withService(TestServiceModule.class)
        .build()) {
      assertThrows(exceptionType,
          () -> testKit.getService(TestService.SERVICE_ID, TestService2.class));
    }
  }

  @Test
  void requestWrongServiceId() {
    Class<IllegalArgumentException> exceptionType = IllegalArgumentException.class;
    try (TestKit testKit = TestKit.forService(TestServiceModule.class)) {
      assertThrows(exceptionType, () -> testKit.getService((short) -1, TestService2.class));
    }
  }

  @Test
  void createTestKitMoreThanMaxServiceNumber() {
    Class<IllegalArgumentException> exceptionType = IllegalArgumentException.class;
    List<Class<? extends ServiceModule>> serviceModules = new ArrayList<>();
    for (int i = 0; i < TestKit.MAX_SERVICE_NUMBER + 1; i++) {
      serviceModules.add(TestServiceModule.class);
    }
    TestKit.Builder testKitBuilder = TestKit.builder(EmulatedNodeType.VALIDATOR)
        .withServices(serviceModules);
    assertThrows(exceptionType, testKitBuilder::build);
  }

  @Test
  void createTestKitWithoutServices() {
    Class<IllegalArgumentException> exceptionType = IllegalArgumentException.class;
    TestKit.Builder testKitBuilder = TestKit.builder(EmulatedNodeType.VALIDATOR);
    assertThrows(exceptionType, testKitBuilder::build);
  }

  @Test
  void createEmptyBlock() {
    try (TestKit testKit = TestKit.forService(TestServiceModule.class)) {
      Block block = testKit.createBlock();
      assertThat(block.getNumTransactions()).isEqualTo(0);

      testKit.withSnapshot((view) -> {
        Blockchain blockchain = Blockchain.newInstance(view);
        assertThat(blockchain.getHeight()).isEqualTo(1);
        assertThat(block).isEqualTo(blockchain.getBlock(1));
        return null;
      });
    }
  }

  @Test
  void afterCommitSubmitsTransaction() {
    try (TestKit testKit = TestKit.forService(TestServiceModule.class)) {
      // Create a block so that afterCommit transaction is submitted
      Block block = testKit.createBlock();
      List<TransactionMessage> inPoolTransactions = testKit
          .findTransactionsInPool(tx -> tx.getServiceId() == TestService.SERVICE_ID);
      assertThat(inPoolTransactions).hasSize(1);
      TransactionMessage inPoolTransaction = inPoolTransactions.get(0);
      RawTransaction afterCommitTransaction = constructAfterCommitTransaction(block.getHeight());

      assertThat(inPoolTransaction.getServiceId())
          .isEqualTo(afterCommitTransaction.getServiceId());
      assertThat(inPoolTransaction.getTransactionId())
          .isEqualTo(afterCommitTransaction.getTransactionId());
      assertThat(inPoolTransaction.getPayload()).isEqualTo(afterCommitTransaction.getPayload());

      Block nextBlock = testKit.createBlock();
      assertThat(nextBlock.getNumTransactions()).isEqualTo(1);
      assertThat(nextBlock.getHeight()).isEqualTo(2);
    }
  }

  @Test
  void createBlockWithTransactionIgnoresInPoolTransactions() {
    try (TestKit testKit = TestKit.forService(TestServiceModule.class)) {
      // Create a block so that afterCommit transaction is submitted
      testKit.createBlock();

      Block block = testKit.createBlockWithTransactions();
      assertThat(block.getNumTransactions()).isEqualTo(0);

      // Two blocks were created, so two afterCommit transactions should be submitted into pool
      List<TransactionMessage> inPoolTransactions = testKit
          .findTransactionsInPool(tx -> tx.getServiceId() == TestService.SERVICE_ID);
      assertThat(inPoolTransactions).hasSize(2);
    }
  }

  @Test
  void createBlockWithSingleTransaction() {
    try (TestKit testKit = TestKit.forService(TestServiceModule.class)) {
      TransactionMessage message = constructTestTransactionMessage("Test message", testKit);
      Block block = testKit.createBlockWithTransactions(message);
      assertThat(block.getNumTransactions()).isEqualTo(1);

      testKit.withSnapshot((view) -> {
        Blockchain blockchain = Blockchain.newInstance(view);
        assertThat(blockchain.getHeight()).isEqualTo(1);
        assertThat(block).isEqualTo(blockchain.getBlock(1));
        Map<HashCode, TransactionResult> transactionResults = toMap(blockchain.getTxResults());
        assertThat(transactionResults).hasSize(1);
        TransactionResult transactionResult = transactionResults.get(message.hash());
        assertThat(transactionResult).isEqualTo(TransactionResult.successful());
        return null;
      });
    }
  }

  @Test
  void createBlockWithTransactions() {
    try (TestKit testKit = TestKit.forService(TestServiceModule.class)) {
      TransactionMessage message = constructTestTransactionMessage("Test message", testKit);
      TransactionMessage message2 = constructTestTransactionMessage("Test message 2", testKit);

      Block block = testKit.createBlockWithTransactions(ImmutableList.of(message, message2));
      assertThat(block.getNumTransactions()).isEqualTo(2);

      testKit.withSnapshot((view) -> {
        checkTransactionsCommittedSuccessfully(view, block, message, message2);
        return null;
      });
    }
  }

  @Test
  void nodeSubmittedTransactionsArePlacedInPool() {
    try (TestKit testKit = TestKit.forService(TestServiceModule.class)) {
      TestService service = testKit.getService(TestService.SERVICE_ID, TestService.class);

      TransactionMessage message = constructTestTransactionMessage("Test message", testKit);
      RawTransaction rawTransaction = TestKit.toRawTransaction(message);

      try {
        service.getNode().submitTransaction(rawTransaction);
      } catch (InternalServerError e) {
        fail(e);
      }

      List<TransactionMessage> transactionsInPool =
          testKit.findTransactionsInPool(tx -> tx.getServiceId() == TestService.SERVICE_ID);
      assertThat(transactionsInPool).isEqualTo(ImmutableList.of(message));
    }
  }

  @Test
  void findTransactionsInPool() {
    try (TestKit testKit = TestKit.forService(TestServiceModule.class)) {
      TestService service = testKit.getService(TestService.SERVICE_ID, TestService.class);

      TransactionMessage message = constructTestTransactionMessage("Test message", testKit);
      RawTransaction rawTransaction = TestKit.toRawTransaction(message);
      TransactionMessage message2 = constructTestTransactionMessage("Test message 2", testKit);
      RawTransaction rawTransaction2 = TestKit.toRawTransaction(message2);

      try {
        service.getNode().submitTransaction(rawTransaction);
        service.getNode().submitTransaction(rawTransaction2);
      } catch (InternalServerError e) {
        fail(e);
      }

      List<TransactionMessage> transactionsInPool =
          testKit.findTransactionsInPool(
              tx -> Arrays.equals(tx.getPayload(), message.getPayload()));
      assertThat(transactionsInPool).isEqualTo(ImmutableList.of(message));
    }
  }

  @Test
  void createBlockWithTransactionsVarargs() {
    try (TestKit testKit = TestKit.forService(TestServiceModule.class)) {
      TransactionMessage message = constructTestTransactionMessage("Test message", testKit);
      TransactionMessage message2 = constructTestTransactionMessage("Test message 2", testKit);

      Block block = testKit.createBlockWithTransactions(message, message2);
      assertThat(block.getNumTransactions()).isEqualTo(2);

      testKit.withSnapshot((view) -> {
        checkTransactionsCommittedSuccessfully(view, block, message, message2);
        return null;
      });
    }
  }

  private TransactionMessage constructTestTransactionMessage(String payload, TestKit testKit) {
    EmulatedNode emulatedNode = testKit.getEmulatedNode();
    KeyPair emulatedNodeKeyPair = emulatedNode.getServiceKeyPair();
    return TransactionMessage.builder()
        .serviceId(TestService.SERVICE_ID)
        .transactionId(TestTransaction.ID)
        .payload(payload.getBytes(BODY_CHARSET))
        .sign(emulatedNodeKeyPair, CRYPTO_FUNCTION);
  }

  private void checkTransactionsCommittedSuccessfully(
      View view, Block block, TransactionMessage message, TransactionMessage message2) {
    Blockchain blockchain = Blockchain.newInstance(view);
    assertThat(blockchain.getHeight()).isEqualTo(1);
    assertThat(block).isEqualTo(blockchain.getBlock(1));
    Map<HashCode, TransactionResult> transactionResults = toMap(blockchain.getTxResults());
    assertThat(transactionResults).hasSize(2);

    TransactionResult transactionResult = transactionResults.get(message.hash());
    assertThat(transactionResult).isEqualTo(TransactionResult.successful());
    TransactionResult transactionResult2 = transactionResults.get(message2.hash());
    assertThat(transactionResult2).isEqualTo(TransactionResult.successful());
  }

  @Test
  void createBlockWithTransactionWithWrongServiceId() {
    try (TestKit testKit = TestKit.forService(TestServiceModule.class)) {
      short wrongServiceId = (short) (TestService.SERVICE_ID + 1);
      EmulatedNode emulatedNode = testKit.getEmulatedNode();
      KeyPair emulatedNodeKeyPair = emulatedNode.getServiceKeyPair();
      TransactionMessage message = TransactionMessage.builder()
          .serviceId(wrongServiceId)
          .transactionId(TestTransaction.ID)
          .payload("Test message".getBytes(BODY_CHARSET))
          .sign(emulatedNodeKeyPair, CRYPTO_FUNCTION);
      IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class,
          () -> testKit.createBlockWithTransactions(message));
      RawTransaction rawTransaction = TestKit.toRawTransaction(message);
      String expectedMessage = String.format("Unknown service id (%s) in transaction (%s)",
          wrongServiceId, rawTransaction);
      assertThat(thrownException).hasMessageContaining(expectedMessage);
    }
  }

  @Test
  void createBlockWithTransactionWithWrongTransactionId() {
    try (TestKit testKit = TestKit.forService(TestServiceModule.class)) {
      short wrongTransactionId = (short) (TestTransaction.ID + 1);
      EmulatedNode emulatedNode = testKit.getEmulatedNode();
      KeyPair emulatedNodeKeyPair = emulatedNode.getServiceKeyPair();
      TransactionMessage message = TransactionMessage.builder()
          .serviceId(TestService.SERVICE_ID)
          .transactionId(wrongTransactionId)
          .payload("Test message".getBytes(BODY_CHARSET))
          .sign(emulatedNodeKeyPair, CRYPTO_FUNCTION);
      IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class,
          () -> testKit.createBlockWithTransactions(message));
      RawTransaction rawTransaction = TestKit.toRawTransaction(message);
      String expectedMessage = String.format("Service (%s) with id=%s failed to convert"
          + " transaction (%s). Make sure that the submitted transaction is correctly serialized,"
          + " and the service's TransactionConverter implementation is correct and handles this"
          + " transaction as expected.",
          TestService.SERVICE_NAME, TestService.SERVICE_ID, rawTransaction);
      assertThat(thrownException).hasMessageContaining(expectedMessage);
    }
  }

  @Test
  void getValidatorEmulatedNode() {
    try (TestKit testKit = TestKit.forService(TestServiceModule.class)) {
      EmulatedNode node = testKit.getEmulatedNode();
      assertThat(node.getNodeType()).isEqualTo(EmulatedNodeType.VALIDATOR);
      assertThat(node.getValidatorId()).isNotEmpty();
      assertThat(node.getServiceKeyPair()).isNotNull();
    }
  }

  @Test
  void getAuditorEmulatedNode() {
    try (TestKit testKit = TestKit.builder(EmulatedNodeType.AUDITOR)
        .withService(TestServiceModule.class)
        .build()) {
      EmulatedNode node = testKit.getEmulatedNode();
      assertThat(node.getNodeType()).isEqualTo(EmulatedNodeType.AUDITOR);
      assertThat(node.getValidatorId()).isEmpty();
      assertThat(node.getServiceKeyPair()).isNotNull();
    }
  }

  @Test
  void timeServiceWorksInTestKit() {
    FakeTimeProvider timeProvider = FakeTimeProvider.create(TIME);
    try (TestKit testKit = TestKit.builder(EmulatedNodeType.VALIDATOR)
        .withService(TestServiceModule.class)
        .withTimeService(timeProvider)
        .build()) {
      // Commit two blocks for time oracle to prepare consolidated time. Two blocks are needed as
      // after the first block time transactions are generated and after the second one they are
      // processed
      testKit.createBlock();
      testKit.createBlock();
      testKit.withSnapshot((view) -> {
        TimeSchema timeSchema = TimeSchema.newInstance(view);
        Optional<ZonedDateTime> consolidatedTime = timeSchema.getTime().toOptional();
        assertThat(consolidatedTime).contains(TIME);

        // Check that validatorsTimes contains one exactly entry with TestKit emulated node's
        // public key and time provider's time
        checkValidatorsTimes(timeSchema, testKit, TIME);
        return null;
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
        TimeSchema timeSchema = TimeSchema.newInstance(view);
        Optional<ZonedDateTime> consolidatedTime = timeSchema.getTime().toOptional();
        assertThat(consolidatedTime).contains(newTime);
        return null;
      });
    }
  }

  @Test
  void createTestKitWithTimeServiceAndTooManyValidators() {
    TimeProvider timeProvider = FakeTimeProvider.create(TIME);
    Class<IllegalArgumentException> exceptionType = IllegalArgumentException.class;
    short invalidValidatorCount = TestKit.MAX_VALIDATOR_COUNT_WITH_ENABLED_TIME_SERVICE + 1;
    TestKit.Builder testKitBuilder = TestKit.builder(EmulatedNodeType.VALIDATOR)
        .withService(TestServiceModule.class)
        .withTimeService(timeProvider)
        .withValidators(invalidValidatorCount);
    IllegalArgumentException thrownException = assertThrows(exceptionType, testKitBuilder::build);
    String expectedMessage = String.format("Number of validators (%s) should be less than or equal"
        + " to %s when TimeService is enabled.",
        invalidValidatorCount, TestKit.MAX_VALIDATOR_COUNT_WITH_ENABLED_TIME_SERVICE);
    assertThat(thrownException).hasMessageContaining(expectedMessage);
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

  public static final class TestServiceModule2 extends AbstractServiceModule {

    private static final TransactionConverter THROWING_TX_CONVERTER = (tx) -> {
      throw new IllegalStateException("No transactions in this service: " + tx);
    };

    @Override
    protected void configure() {
      bind(Service.class).to(TestService2.class).in(Singleton.class);
      bind(TransactionConverter.class).toInstance(THROWING_TX_CONVERTER);
    }
  }

  static final class TestService2 implements Service {

    static final short SERVICE_ID = 48;
    static final String SERVICE_NAME = "Test service 2";

    private Node node;

    @Override
    public short getId() {
      return SERVICE_ID;
    }

    @Override
    public String getName() {
      return SERVICE_NAME;
    }

    Node getNode() {
      return node;
    }

    @Override
    public Transaction convertToTransaction(RawTransaction rawTransaction) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void createPublicApiHandlers(Node node, Router router) {
      this.node = node;
    }
  }
}
