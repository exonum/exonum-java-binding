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

import com.exonum.binding.blockchain.Block;
import com.exonum.binding.blockchain.Blockchain;
import com.exonum.binding.common.blockchain.TransactionResult;
import com.exonum.binding.common.crypto.CryptoFunction;
import com.exonum.binding.common.crypto.CryptoFunctions;
import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.service.AbstractServiceModule;
import com.exonum.binding.service.Node;
import com.exonum.binding.service.Service;
import com.exonum.binding.service.ServiceModule;
import com.exonum.binding.service.TransactionConverter;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.transaction.Transaction;
import com.exonum.binding.util.LibraryLoader;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.inject.Singleton;
import io.vertx.ext.web.Router;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TestKitTest {

  private static final CryptoFunction CRYPTO_FUNCTION = CryptoFunctions.ed25519();
  private static final KeyPair KEY_PAIR = CRYPTO_FUNCTION.generateKeyPair();

  static {
    LibraryLoader.load();
  }

  @Test
  void createTestKitForSingleService() {
    TestKit testKit = TestKit.forService(TestServiceModule.class);
    Service service = testKit.getService(TestService.SERVICE_ID, TestService.class);
    assertThat(service.getId()).isEqualTo(TestService.SERVICE_ID);
    assertThat(service.getName()).isEqualTo(TestService.SERVICE_NAME);
  }

  @Test
  void createTestKitWithBuilderForSingleService() {
    TestKit testKit = TestKit.builder(EmulatedNodeType.VALIDATOR)
        .withService(TestServiceModule.class)
        .build();
    Service service = testKit.getService(TestService.SERVICE_ID, TestService.class);
    assertThat(service.getId()).isEqualTo(TestService.SERVICE_ID);
    assertThat(service.getName()).isEqualTo(TestService.SERVICE_NAME);
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
    TestKit testKit = TestKit.builder(EmulatedNodeType.VALIDATOR)
        .withService(TestServiceModule.class)
        .withService(TestServiceModule2.class)
        .build();
    Service service = testKit.getService(TestService.SERVICE_ID, TestService.class);
    Service service2 = testKit.getService(TestService2.SERVICE_ID, TestService2.class);
    assertThat(service.getId()).isEqualTo(TestService.SERVICE_ID);
    assertThat(service.getName()).isEqualTo(TestService.SERVICE_NAME);
    assertThat(service2.getId()).isEqualTo(TestService2.SERVICE_ID);
    assertThat(service2.getName()).isEqualTo(TestService2.SERVICE_NAME);
  }

  @Test
  void createTestKitWithBuilderForMultipleDifferentServicesVarargs() {
    TestKit testKit = TestKit.builder(EmulatedNodeType.VALIDATOR)
        .withServices(TestServiceModule.class, TestServiceModule2.class)
        .build();
    Service service = testKit.getService(TestService.SERVICE_ID, TestService.class);
    Service service2 = testKit.getService(TestService2.SERVICE_ID, TestService2.class);
    assertThat(service.getId()).isEqualTo(TestService.SERVICE_ID);
    assertThat(service.getName()).isEqualTo(TestService.SERVICE_NAME);
    assertThat(service2.getId()).isEqualTo(TestService2.SERVICE_ID);
    assertThat(service2.getName()).isEqualTo(TestService2.SERVICE_NAME);
  }

  @Test
  void requestWrongServiceClass() {
    Class<IllegalArgumentException> exceptionType = IllegalArgumentException.class;
    TestKit testKit = TestKit.builder(EmulatedNodeType.VALIDATOR)
        .withService(TestServiceModule.class)
        .build();
    assertThrows(exceptionType,
        () -> testKit.getService(TestService.SERVICE_ID, TestService2.class));
  }

  @Test
  void requestWrongServiceId() {
    Class<IllegalArgumentException> exceptionType = IllegalArgumentException.class;
    TestKit testKit = TestKit.forService(TestServiceModule.class);
    assertThrows(exceptionType, () -> testKit.getService((short) -1, TestService2.class));
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
  void initializationChangesState() {
    TestKit testKit = TestKit.forService(TestServiceModule.class);
    Map<HashCode, String> map = testKit.withSnapshot((view) -> {
      TestService testService = testKit.getService(TestService.SERVICE_ID, TestService.class);
      TestSchema testSchema = testService.createDataSchema(view);
      ProofMapIndexProxy<HashCode, String> proofMapIndexProxy = testSchema.testMap();
      return toMap(proofMapIndexProxy);
    });
    assertThat(map).hasSize(1);
    String initialValue = map.get(TestService.INITIAL_ENTRY_KEY);
    assertThat(initialValue).isEqualTo(TestService.INITIAL_ENTRY_VALUE);
  }

  @Test
  void createEmptyBlock() {
    TestKit testKit = TestKit.forService(TestServiceModule.class);
    Block block = testKit.createBlock();
    assertThat(block.getNumTransactions()).isEqualTo(0);

    testKit.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      assertThat(blockchain.getHeight()).isEqualTo(1);
      assertThat(block).isEqualTo(blockchain.getBlock(1));
      return null;
    });
  }

  @Test
  void afterCommitSubmitsTransaction() {
    TestKit testKit = TestKit.forService(TestServiceModule.class);

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

  @Test
  void createBlockWithTransactionIgnoresInPoolTransactions() {
    TestKit testKit = TestKit.forService(TestServiceModule.class);

    testKit.createBlock();

    TransactionMessage message = constructTestTransactionMessage("Test message");
    Block block = testKit.createBlockWithTransaction(message);
    assertThat(block.getNumTransactions()).isEqualTo(1);

    // Two blocks were created, so two afterCommit transactions should be submitted into pool
    List<TransactionMessage> inPoolTransactions = testKit
        .findTransactionsInPool(tx -> tx.getServiceId() == TestService.SERVICE_ID);
    assertThat(inPoolTransactions).hasSize(2);
  }

  @Test
  void createBlockWithTransaction() {
    TestKit testKit = TestKit.forService(TestServiceModule.class);

    TransactionMessage message = constructTestTransactionMessage("Test message");
    Block block = testKit.createBlockWithTransaction(message);
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

  @Test
  void createBlockWithTransactions() {
    TestKit testKit = TestKit.forService(TestServiceModule.class);

    TransactionMessage message = constructTestTransactionMessage("Test message");
    TransactionMessage message2 = constructTestTransactionMessage("Test message 2");

    Block block = testKit.createBlockWithTransactions(ImmutableList.of(message, message2));
    assertThat(block.getNumTransactions()).isEqualTo(2);

    testKit.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      assertThat(blockchain.getHeight()).isEqualTo(1);
      assertThat(block).isEqualTo(blockchain.getBlock(1));
      Map<HashCode, TransactionResult> transactionResults = toMap(blockchain.getTxResults());
      assertThat(transactionResults).hasSize(2);

      TransactionResult transactionResult = transactionResults.get(message.hash());
      assertThat(transactionResult).isEqualTo(TransactionResult.successful());
      TransactionResult transactionResult2 = transactionResults.get(message2.hash());
      assertThat(transactionResult2).isEqualTo(TransactionResult.successful());
      return null;
    });
  }

  @Test
  void createBlockWithTransactionsVarargs() {
    TestKit testKit = TestKit.forService(TestServiceModule.class);

    TransactionMessage message = constructTestTransactionMessage("Test message");
    TransactionMessage message2 = constructTestTransactionMessage("Test message 2");

    Block block = testKit.createBlockWithTransactions(message, message2);
    assertThat(block.getNumTransactions()).isEqualTo(2);

    testKit.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      assertThat(blockchain.getHeight()).isEqualTo(1);
      assertThat(block).isEqualTo(blockchain.getBlock(1));
      Map<HashCode, TransactionResult> transactionResults = toMap(blockchain.getTxResults());
      assertThat(transactionResults).hasSize(2);

      TransactionResult transactionResult = transactionResults.get(message.hash());
      assertThat(transactionResult).isEqualTo(TransactionResult.successful());
      TransactionResult transactionResult2 = transactionResults.get(message2.hash());
      assertThat(transactionResult2).isEqualTo(TransactionResult.successful());
      return null;
    });
  }

  @Test
  void getValidatorEmulatedNode() {
    TestKit testKit = TestKit.forService(TestServiceModule.class);
    EmulatedNode node = testKit.getEmulatedNode();
    assertThat(node.getNodeType()).isEqualTo(EmulatedNodeType.VALIDATOR);
    assertThat(node.getValidatorId()).isNotEmpty();
    assertThat(node.getServiceKeyPair()).isNotNull();
  }

  @Test
  void getAuditorEmulatedNode() {
    TestKit testKit = TestKit.builder(EmulatedNodeType.AUDITOR)
        .withService(TestServiceModule.class)
        .build();
    EmulatedNode node = testKit.getEmulatedNode();
    assertThat(node.getNodeType()).isEqualTo(EmulatedNodeType.AUDITOR);
    assertThat(node.getValidatorId()).isEmpty();
    assertThat(node.getServiceKeyPair()).isNotNull();
  }

  private TransactionMessage constructTestTransactionMessage(String payload) {
    return TransactionMessage.builder()
        .serviceId(TestService.SERVICE_ID)
        .transactionId(TestTransaction.ID)
        .payload(payload.getBytes(BODY_CHARSET))
        .sign(KEY_PAIR, CRYPTO_FUNCTION);
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

    static short SERVICE_ID = 48;
    static String SERVICE_NAME = "Test service 2";

    @Override
    public short getId() {
      return SERVICE_ID;
    }

    @Override
    public String getName() {
      return SERVICE_NAME;
    }

    @Override
    public Transaction convertToTransaction(RawTransaction rawTransaction) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void createPublicApiHandlers(Node node, Router router) {
      // No-op: no handlers.
    }
  }
}
