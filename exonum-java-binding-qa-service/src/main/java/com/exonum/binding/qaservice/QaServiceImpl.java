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

import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.blockchain.Block;
import com.exonum.binding.blockchain.Blockchain;
import com.exonum.binding.blockchain.TransactionLocation;
import com.exonum.binding.blockchain.TransactionResult;
import com.exonum.binding.common.configuration.StoredConfiguration;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.Hashing;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.qaservice.transactions.CreateCounterTx;
import com.exonum.binding.qaservice.transactions.IncrementCounterTx;
import com.exonum.binding.qaservice.transactions.InvalidThrowingTx;
import com.exonum.binding.qaservice.transactions.InvalidTx;
import com.exonum.binding.qaservice.transactions.UnknownTx;
import com.exonum.binding.qaservice.transactions.ValidErrorTx;
import com.exonum.binding.qaservice.transactions.ValidThrowingTx;
import com.exonum.binding.service.AbstractService;
import com.exonum.binding.service.BlockCommittedEvent;
import com.exonum.binding.service.InternalServerError;
import com.exonum.binding.service.InvalidTransactionException;
import com.exonum.binding.service.Node;
import com.exonum.binding.service.Schema;
import com.exonum.binding.service.TransactionConverter;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.indices.ListIndex;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.storage.indices.ProofListIndexProxy;
import com.exonum.binding.transaction.Transaction;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import io.vertx.ext.web.Router;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A simple QA service.
 *
 * @implNote This service is meant to be used to test integration of Exonum Java Binding with
 *     Exonum Core. It contains very little business-logic so the QA team can focus
 *     on testing the integration <em>through</em> this service, not the service itself.
 *
 *     <p>Such service is not meant to be illustrative, it breaks multiple recommendations
 *     on implementing Exonum Services, therefore, it shall NOT be used as an example
 *     of a user service.
 */
final class QaServiceImpl extends AbstractService implements QaService {

  private static final Logger logger = LogManager.getLogger(QaService.class);

  @VisibleForTesting
  static final String INITIAL_SERVICE_CONFIGURATION = "{ \"version\": 0.1 }";

  @VisibleForTesting
  static final String DEFAULT_COUNTER_NAME = "default";

  @VisibleForTesting
  static final String AFTER_COMMIT_COUNTER_NAME = "after_commit_counter";

  @Nullable
  private Node node;

  @Inject
  public QaServiceImpl(TransactionConverter transactionConverter) {
    super(ID, NAME, transactionConverter);
  }

  @Override
  protected Schema createDataSchema(View view) {
    return new QaSchema(view);
  }

  @Override
  public List<HashCode> getStateHashes(Snapshot snapshot) {
    List<HashCode> stateHashes = super.getStateHashes(snapshot);
    // Log the state hashes, so that the values passed to the native part of the framework
    // are known.
    logger.error("state hashes: {}", stateHashes);
    return stateHashes;
  }

  @Override
  public Optional<String> initialize(Fork fork) {
    // Add a default counter to the blockchain.
    createCounter(DEFAULT_COUNTER_NAME, fork);

    // Add an afterCommit counter that will be incremented after each block commited event.
    createCounter(AFTER_COMMIT_COUNTER_NAME, fork);

    return Optional.of(INITIAL_SERVICE_CONFIGURATION);
  }

  @Override
  public void createPublicApiHandlers(Node node, Router router) {
    this.node = node;

    ApiController controller = new ApiController(this);
    controller.mountApi(router);
  }

  /**
   * Increments the afterCommit counter so the number of times this method was invoked is stored
   * in it.
   */
  @Override
  public void afterCommit(BlockCommittedEvent event) {
    long seed = event.getHeight();
    HashCode counterId = Hashing.sha256()
        .hashString(AFTER_COMMIT_COUNTER_NAME, StandardCharsets.UTF_8);
    submitIncrementCounter(seed, counterId);
  }

  @Override
  public HashCode submitCreateCounter(String counterName) {
    CreateCounterTx tx = new CreateCounterTx(counterName);
    return submitTransaction(tx);
  }

  @Override
  public HashCode submitIncrementCounter(long requestSeed, HashCode counterId) {
    Transaction tx = new IncrementCounterTx(requestSeed, counterId);
    return submitTransaction(tx);
  }

  @Override
  public HashCode submitInvalidTx() {
    Transaction tx = new InvalidTx();
    return submitTransaction(tx);
  }

  @Override
  public HashCode submitInvalidThrowingTx() {
    Transaction tx = new InvalidThrowingTx();
    return submitTransaction(tx);
  }

  @Override
  public HashCode submitValidThrowingTx(long requestSeed) {
    Transaction tx = new ValidThrowingTx(requestSeed);
    return submitTransaction(tx);
  }

  @Override
  public HashCode submitValidErrorTx(long requestSeed, byte errorCode,
      @Nullable String description) {
    Transaction tx = new ValidErrorTx(requestSeed, errorCode, description);
    return submitTransaction(tx);
  }

  @Override
  public HashCode submitUnknownTx() {
    Transaction tx = new UnknownTx();
    return submitTransaction(tx);
  }

  @Override
  @SuppressWarnings("ConstantConditions")  // Node is not null.
  public Optional<Counter> getValue(HashCode counterId) {
    checkBlockchainInitialized();

    return node.withSnapshot((view) -> {
      QaSchema schema = new QaSchema(view);
      MapIndex<HashCode, Long> counters = schema.counters();
      if (!counters.containsKey(counterId)) {
        return Optional.empty();
      }

      MapIndex<HashCode, String> counterNames = schema.counterNames();
      String name = counterNames.get(counterId);
      Long value = counters.get(counterId);
      return Optional.of(new Counter(name, value));
    });
  }

  @Override
  public Height getHeight() {
    checkBlockchainInitialized();

    return node.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      long value = blockchain.getHeight();
      return new Height(value);
    });
  }

  @Override
  public List<HashCode> getBlockHashes() {
    return node.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      ListIndex<HashCode> hashes = blockchain.getBlockHashes();

      return Lists.newArrayList(hashes);
    });
  }

  @Override
  public List<HashCode> getBlockTransactions(long height) {
    return node.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      ProofListIndexProxy<HashCode> hashes = blockchain.getBlockTransactions(height);

      return Lists.newArrayList(hashes);
    });
  }

  @Override
  public List<HashCode> getBlockTransactions(HashCode blockId) {
    return node.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      ProofListIndexProxy<HashCode> hashes = blockchain.getBlockTransactions(blockId);

      return Lists.newArrayList(hashes);
    });
  }

  @Override
  public Map<HashCode, TransactionMessage> getTxMessages() {
    return node.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      MapIndex<HashCode, TransactionMessage> txMessages = blockchain.getTxMessages();

      return Maps.toMap(txMessages.keys(), txMessages::get);
    });
  }

  @Override
  public Map<HashCode, TransactionResult> getTxResults() {
    return node.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      MapIndex<HashCode, TransactionResult> txResults = blockchain.getTxResults();

      return Maps.toMap(txResults.keys(), txResults::get);
    });
  }

  @Override
  public Optional<TransactionResult> getTxResult(HashCode messageHash) {
    return node.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      return blockchain.getTxResult(messageHash);
    });
  }

  @Override
  public Map<HashCode, TransactionLocation> getTxLocations() {
    return node.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      MapIndex<HashCode, TransactionLocation> txLocations = blockchain.getTxLocations();

      return Maps.toMap(txLocations.keys(), txLocations::get);
    });
  }

  @Override
  public Optional<TransactionLocation> getTxLocation(HashCode messageHash) {
    return node.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      return blockchain.getTxLocation(messageHash);
    });
  }

  @Override
  public Map<HashCode, Block> getBlocks() {
    return node.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      MapIndex<HashCode, Block> blocks = blockchain.getBlocks();

      return Maps.toMap(blocks.keys(), blocks::get);
    });
  }

  @Override
  public Block getBlockByHeight(long height) {
    return node.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      return blockchain.getBlock(height);
    });
  }

  @Override
  public Optional<Block> getBlockById(HashCode blockId) {
    return node.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      return blockchain.findBlock(blockId);
    });
  }

  @Override
  public Block getLastBlock() {
    return node.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      return blockchain.getLastBlock();
    });
  }

  private void createCounter(String name, Fork fork) {
    new CreateCounterTx(name).execute(fork);
  }

  @Override
  public StoredConfiguration getActualConfiguration() {
    checkBlockchainInitialized();

    return node.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);

      return blockchain.getActualConfiguration();
    });
  }

  @SuppressWarnings("ConstantConditions") // Node is not null.
  private HashCode submitTransaction(Transaction tx) {
    checkBlockchainInitialized();
    try {
      node.submitTransaction(tx);
      return tx.hash();
    } catch (InvalidTransactionException | InternalServerError e) {
      throw new RuntimeException("Propagated transaction submission exception", e);
    }
  }

  private void checkBlockchainInitialized() {
    checkState(node != null, "Service has not been fully initialized yet");
  }
}
