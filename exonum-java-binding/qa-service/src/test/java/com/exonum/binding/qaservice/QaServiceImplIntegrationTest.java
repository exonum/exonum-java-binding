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
import static com.exonum.binding.qaservice.QaServiceImpl.AFTER_COMMIT_COUNTER_NAME;
import static com.exonum.binding.qaservice.QaServiceImpl.DEFAULT_COUNTER_NAME;
import static com.exonum.binding.qaservice.QaServiceImpl.INITIAL_SERVICE_CONFIGURATION;
import static com.exonum.binding.qaservice.transactions.ContextUtils.newContext;
import static com.exonum.binding.test.Bytes.bytes;
import static com.exonum.binding.test.Bytes.createPrefixed;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exonum.binding.blockchain.Block;
import com.exonum.binding.blockchain.TransactionLocation;
import com.exonum.binding.blockchain.TransactionResult;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.qaservice.transactions.CreateCounterTx;
import com.exonum.binding.qaservice.transactions.IncrementCounterTx;
import com.exonum.binding.qaservice.transactions.ThrowingTx;
import com.exonum.binding.service.BlockCommittedEvent;
import com.exonum.binding.service.BlockCommittedEventImpl;
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
import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.transaction.TransactionContext;
import com.exonum.binding.util.LibraryLoader;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.junit5.VertxExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
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

  private static final String NO_GENESIS_BLOCK_ERROR_MESSAGE =
      "An attempt to get the actual `height` during creating the genesis block";
  private static final HashCode DEFAULT_TX_MESSAGE_HASH = HashCode.fromString("a0a0a0a0");

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
      int numMerkelizedCollections = 1;
      assertThat(stateHashes).hasSize(numMerkelizedCollections);

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
      assertThat(initialConfiguration).hasValue(INITIAL_SERVICE_CONFIGURATION);

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
    }
  }

  @Test
  void submitCreateCounter() throws Exception {
    setServiceNode(node);

    when(node.submitTransaction(any(RawTransaction.class))).thenReturn(DEFAULT_TX_MESSAGE_HASH);

    String counterName = "bids";
    HashCode txHash = service.submitCreateCounter(counterName);

    CreateCounterTx expectedTx = new CreateCounterTx(counterName);
    RawTransaction expectedRawTx = CreateCounterTx.converter().toRawTransaction(expectedTx);

    assertThat(txHash).isEqualTo(DEFAULT_TX_MESSAGE_HASH);
    verify(node).submitTransaction(eq(expectedRawTx));
  }

  @Test
  void submitIncrementCounter() throws Exception {
    setServiceNode(node);
    when(node.submitTransaction(any(RawTransaction.class))).thenReturn(DEFAULT_TX_MESSAGE_HASH);

    long seed = 1L;
    HashCode counterId = sha256()
        .hashString("Cats counter", UTF_8);
    HashCode txHash = service.submitIncrementCounter(seed, counterId);

    IncrementCounterTx expectedTx = new IncrementCounterTx(seed, counterId);
    RawTransaction expectedRawTx = IncrementCounterTx.converter().toRawTransaction(expectedTx);

    assertThat(txHash).isEqualTo(DEFAULT_TX_MESSAGE_HASH);
    verify(node).submitTransaction(eq(expectedRawTx));
  }

  @Test
  void submitValidThrowingTx() throws Exception {
    setServiceNode(node);
    when(node.submitTransaction(any(RawTransaction.class))).thenReturn(DEFAULT_TX_MESSAGE_HASH);

    long seed = 1L;
    HashCode txHash = service.submitValidThrowingTx(seed);

    ThrowingTx expectedTx = new ThrowingTx(seed);
    RawTransaction expectedRawTx = ThrowingTx.converter().toRawTransaction(expectedTx);

    assertThat(txHash).isEqualTo(DEFAULT_TX_MESSAGE_HASH);
    verify(node).submitTransaction(eq(expectedRawTx));
  }

  @Test
  void submitUnknownTx() throws Exception {
    setServiceNode(node);
    when(node.submitTransaction(any(RawTransaction.class))).thenReturn(DEFAULT_TX_MESSAGE_HASH);

    HashCode txHash = service.submitUnknownTx();

    assertThat(txHash).isEqualTo(DEFAULT_TX_MESSAGE_HASH);
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

        // Execute the transaction
        TransactionContext context = newContext(view);
        new CreateCounterTx(counterName)
            .execute(context);

        db.merge(view);
      }

      // Check that the service returns expected value
      HashCode counterId = sha256().hashString(counterName, UTF_8);
      Optional<Counter> counterValueOpt = service.getValue(counterId);
      Counter expectedCounter = new Counter(counterName, 0L);
      assertThat(counterValueOpt).hasValue(expectedCounter);
    }
  }

  @Test
  @RequiresNativeLibrary
  void getValueNoSuchCounter() {
    withNodeFake(() -> {
      HashCode counterId = sha256().hashString("Unknown counter", UTF_8);
      // Check there is no such counter
      assertThat(service.getValue(counterId)).isEmpty();
    });
  }

  @Test
  void getValueBeforeInit() {
    assertThrows(IllegalStateException.class,
        () -> service.getValue(HashCode.fromInt(1))
    );
  }

  @Test
  @RequiresNativeLibrary
  void getHeight() {
    withNodeFake(() -> {
      Exception e = assertThrows(RuntimeException.class, () -> service.getHeight());
      assertThat(e).hasMessageContaining(NO_GENESIS_BLOCK_ERROR_MESSAGE);
    });
  }

  @Test
  @RequiresNativeLibrary
  void getBlockHashes() {
    withNodeFake(() -> {
      List<HashCode> hashes = service.getBlockHashes();
      assertThat(hashes).isEmpty();
    });
  }

  @Test
  @RequiresNativeLibrary
  void getBlockTransactionsByHeight() {
    withNodeFake(() -> {
      Exception e = assertThrows(RuntimeException.class, () -> service.getBlockTransactions(0L));
      assertThat(e).hasMessageContaining(NO_GENESIS_BLOCK_ERROR_MESSAGE);
    });
  }

  @Test
  @RequiresNativeLibrary
  void afterCommit() throws Exception {
    try (MemoryDb db = MemoryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      Fork fork = db.createFork(cleaner);
      setServiceNode(node);
      when(node.submitTransaction(any(RawTransaction.class))).thenReturn(DEFAULT_TX_MESSAGE_HASH);
      service.initialize(fork);

      Snapshot snapshot = db.createSnapshot(cleaner);
      long height = 0L;
      BlockCommittedEvent event =
          BlockCommittedEventImpl.valueOf(snapshot, OptionalInt.of(1), height);
      service.afterCommit(event);

      HashCode counterId = sha256().hashString(AFTER_COMMIT_COUNTER_NAME, UTF_8);
      IncrementCounterTx expectedTx = new IncrementCounterTx(height, counterId);
      RawTransaction expectedRawTx = IncrementCounterTx.converter().toRawTransaction(expectedTx);

      verify(node).submitTransaction(eq(expectedRawTx));
    }
  }

  @Test
  void getActualConfigurationBeforeInit() {
    assertThrows(IllegalStateException.class,
        () -> service.getActualConfiguration());
  }

  @Test
  @RequiresNativeLibrary
  void getActualConfiguration() {
    withNodeFake(() -> {
      Throwable t = assertThrows(RuntimeException.class, () -> service.getActualConfiguration());
      assertThat(t.getMessage()).contains("Couldn't not find any config for"
          + " height 0, that means that genesis block was created incorrectly.");
    });
  }

  @Test
  @RequiresNativeLibrary
  void getBlockTransactionsByBlockId() {
    withNodeFake(() -> {
      Throwable t = assertThrows(RuntimeException.class, () -> service.getBlockTransactions(0L));
      assertThat(t.getMessage()).contains(NO_GENESIS_BLOCK_ERROR_MESSAGE);
    });
  }

  @Test
  @RequiresNativeLibrary
  void getTxMessages() {
    withNodeFake(() -> {
      Map<HashCode, TransactionMessage> txMessages = service.getTxMessages();
      assertThat(txMessages).isEmpty();
    });
  }

  @Test
  @RequiresNativeLibrary
  void getTxResults() {
    withNodeFake(() -> {
      Map<HashCode, TransactionResult> txResults = service.getTxResults();
      assertThat(txResults).isEmpty();
    });
  }

  @Test
  @RequiresNativeLibrary
  void getTxResult() {
    withNodeFake(() -> {
      HashCode messageHash = HashCode.fromBytes(createPrefixed(bytes(0x00), 32));
      Optional<TransactionResult> txResult = service.getTxResult(messageHash);
      assertThat(txResult).isEmpty();
    });
  }

  @Test
  @RequiresNativeLibrary
  void getTxLocations() {
    withNodeFake(() -> {
      Map<HashCode, TransactionLocation> txLocations = service.getTxLocations();
      assertThat(txLocations).isEmpty();
    });
  }

  @Test
  @RequiresNativeLibrary
  void getTxLocation() {
    withNodeFake(() -> {
      HashCode messageHash = HashCode.fromString("ab");
      Optional<TransactionLocation> txLocation = service.getTxLocation(messageHash);
      assertThat(txLocation).isEmpty();
    });
  }

  @Test
  @RequiresNativeLibrary
  void getBlocks() {
    withNodeFake(() -> {
      Map<HashCode, Block> blocks = service.getBlocks();
      assertThat(blocks).isEmpty();
    });
  }

  @Test
  @RequiresNativeLibrary
  void getBlockByHeight() {
    withNodeFake(() -> {
      long blockHeight = 0L;
      Throwable t =
          assertThrows(RuntimeException.class, () -> service.getBlockByHeight(blockHeight));
      assertThat(t.getMessage()).contains(NO_GENESIS_BLOCK_ERROR_MESSAGE);
    });
  }

  @Test
  @RequiresNativeLibrary
  void getBlockById() {
    withNodeFake(() -> {
      HashCode blockId = HashCode.fromString("ab");
      assertThat(service.getBlockById(blockId)).isEmpty();
    });
  }

  @Test
  @RequiresNativeLibrary
  void getLastBlock() {
    withNodeFake(() -> {
      Exception e = assertThrows(RuntimeException.class, () -> service.getLastBlock());
      assertThat(e).hasMessageContaining(
          "An attempt to get the `last_block` during creating the"
          + " genesis block.");
    });
  }

  @Test
  @RequiresNativeLibrary
  void getTime() {
    withNodeFake(() -> assertThat(service.getTime()).isEmpty());
  }

  @Test
  @RequiresNativeLibrary
  void getValidatorsTime() {
    withNodeFake(() -> assertThat(service.getValidatorsTimes()).isEmpty());
  }

  /** Runs a test with a service with a node fake set. */
  private void withNodeFake(Runnable test) {
    try (MemoryDb db = MemoryDb.newInstance()) {
      node = new NodeFake(db);
      setServiceNode(node);

      test.run();
    }
  }

  private void setServiceNode(Node node) {
    Router router = Router.router(vertx);
    service.createPublicApiHandlers(node, router);
  }
}
