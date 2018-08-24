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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.Hashing;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.qaservice.transactions.CreateCounterTx;
import com.exonum.binding.qaservice.transactions.IncrementCounterTx;
import com.exonum.binding.qaservice.transactions.InvalidThrowingTx;
import com.exonum.binding.qaservice.transactions.InvalidTx;
import com.exonum.binding.qaservice.transactions.UnknownTx;
import com.exonum.binding.qaservice.transactions.ValidThrowingTx;
import com.exonum.binding.service.Node;
import com.exonum.binding.service.NodeFake;
import com.exonum.binding.service.Schema;
import com.exonum.binding.service.TransactionConverter;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.util.LibraryLoader;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.junit5.VertxExtension;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class QaServiceImplIntegrationTest {

  static {
    LibraryLoader.load();
  }

  private QaServiceImpl service;
  private Node node;
  private Vertx vertx;
  private ListAppender logAppender;

  @BeforeEach
  void setUp(Vertx vertx) {
    TransactionConverter transactionConverter = mock(TransactionConverter.class);
    service = new QaServiceImpl(transactionConverter);
    node = mock(Node.class);
    this.vertx = vertx;
    logAppender = getCapturingLogAppender();
  }

  private static ListAppender getCapturingLogAppender() {
    LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
    Configuration config = ctx.getConfiguration();
    return (ListAppender) config.getAppenders().get("ListAppender");
  }

  @AfterEach
  void tearDown() {
    logAppender.clear();
  }

  @Test
  void createDataSchema() {
    View view = mock(View.class);
    Schema dataSchema = service.createDataSchema(view);

    assertThat(dataSchema).isInstanceOf(QaSchema.class);
  }

  @Test
  @RequiresNativeLibrary
  void getStateHashesLogsThem() throws CloseFailuresException {
    try (MemoryDb db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Snapshot view = db.createSnapshot(cleaner);

      List<HashCode> stateHashes = service.getStateHashes(view);
      int numMerklizedCollections = 1;
      assertThat(stateHashes).hasSize(numMerklizedCollections);

      List<String> logMessages = logAppender.getMessages();
      int expectedNumMessages = 1;
      assertThat(logMessages).hasSize(expectedNumMessages);

      assertThat(logMessages.get(0))
          .contains("ERROR")
          .contains(stateHashes.get(0).toString());
    }
  }

  @Test
  @RequiresNativeLibrary
  void initialize() throws CloseFailuresException {
    try (MemoryDb db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);
      Optional<String> initialConfiguration = service.initialize(view);

      // Check the configuration.
      assertThat(initialConfiguration)
          .hasValue(QaServiceImpl.INITIAL_SERVICE_CONFIGURATION);

      // Check the changes made to the database.
      QaSchema schema = new QaSchema(view);
      MapIndex<HashCode, Long> counters = schema.counters();
      MapIndex<HashCode, String> counterNames = schema.counterNames();

      String counterName = "default";
      HashCode counterId = Hashing.sha256()
          .hashString(counterName, UTF_8);

      assertThat(counters.get(counterId)).isEqualTo(0L);
      assertThat(counterNames.get(counterId)).isEqualTo(counterName);
    }
  }

  @Test
  void submitCreateCounter() throws Exception {
    setServiceNode(node);

    String counterName = "bids";
    HashCode txHash = service.submitCreateCounter(counterName);

    Transaction expectedTx = new CreateCounterTx(counterName);

    assertThat(txHash).isEqualTo(expectedTx.hash());
    verify(node).submitTransaction(eq(expectedTx));
  }

  @Test
  void submitIncrementCounter() throws Exception {
    setServiceNode(node);

    long seed = 1L;
    HashCode counterId = Hashing.sha256()
        .hashString("Cats counter", StandardCharsets.UTF_8);
    HashCode txHash = service.submitIncrementCounter(seed, counterId);

    Transaction expectedTx = new IncrementCounterTx(seed, counterId);

    assertThat(txHash).isEqualTo(expectedTx.hash());
    verify(node).submitTransaction(eq(expectedTx));
  }

  @Test
  void submitInvalidTx() throws Exception {
    setServiceNode(node);

    service.submitInvalidTx();
    verify(node).submitTransaction(any(InvalidTx.class));
  }

  @Test
  void submitInvalidThrowingTx() throws Exception {
    setServiceNode(node);

    service.submitInvalidThrowingTx();
    verify(node).submitTransaction(any(InvalidThrowingTx.class));
  }

  @Test
  void submitValidThrowingTx() throws Exception {
    setServiceNode(node);

    long seed = 1L;
    HashCode txHash = service.submitValidThrowingTx(seed);

    Transaction expectedTx = new ValidThrowingTx(seed);

    assertThat(txHash).isEqualTo(expectedTx.hash());
    verify(node).submitTransaction(eq(expectedTx));
  }

  @Test
  void submitUnknownTx() throws Exception {
    setServiceNode(node);
    
    HashCode txHash = service.submitUnknownTx();
    
    Transaction expectedTx = new UnknownTx();
    
    assertThat(txHash).isEqualTo(expectedTx.hash());
    verify(node).submitTransaction(any(UnknownTx.class));
  }

  @Test
  void submitUnknownTxBeforeNodeIsSet() {
    // Do not set the node: try to submit transaction with a null node.
    assertThrows(IllegalStateException.class,
        () -> service.submitUnknownTx());
  }

  @Test
  @RequiresNativeLibrary
  void getValue() throws CloseFailuresException {
    try (MemoryDb db = MemoryDb.newInstance()) {
      node = new NodeFake(db);
      setServiceNode(node);

      // Create a counter with the given name
      String counterName = "bids";
      try (Cleaner cleaner = new Cleaner()) {
        Fork view = db.createFork(cleaner);
        new CreateCounterTx(counterName)
            .execute(view);

        db.merge(view);
      }

      // Check that the service returns expected value
      HashCode counterId = Hashing.sha256().hashString(counterName, UTF_8);
      Optional<Counter> counterValueOpt = service.getValue(counterId);
      Counter expectedCounter = new Counter(counterName, 0L);
      assertThat(counterValueOpt).hasValue(expectedCounter);
    }
  }

  @Test
  @RequiresNativeLibrary
  void getValueNoSuchCounter() {
    try (MemoryDb db = MemoryDb.newInstance()) {
      node = new NodeFake(db);
      setServiceNode(node);

      HashCode counterId = Hashing.sha256().hashString("Unknown counter", UTF_8);
      // Check there is no such counter
      assertThat(service.getValue(counterId)).isEmpty();
    }
  }

  @Test
  void getValueBeforeInit() {
    assertThrows(IllegalStateException.class,
        () -> service.getValue(HashCode.fromInt(1))
    );
  }

  private void setServiceNode(Node node) {
    Router router = Router.router(vertx);
    service.createPublicApiHandlers(node, router);
  }
}
