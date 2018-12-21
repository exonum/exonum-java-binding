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

import static com.exonum.binding.common.hash.Hashing.defaultHashFunction;
import static com.google.common.base.Preconditions.checkState;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.exonum.binding.blockchain.Blockchain;
import com.exonum.binding.common.configuration.StoredConfiguration;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.Hashing;
import com.exonum.binding.qaservice.transactions.CreateCounterTx;
import com.exonum.binding.qaservice.transactions.IncrementCounterTx;
import com.exonum.binding.qaservice.transactions.UnknownTx;
import com.exonum.binding.qaservice.transactions.ErrorTx;
import com.exonum.binding.qaservice.transactions.ThrowingTx;
import com.exonum.binding.service.AbstractService;
import com.exonum.binding.service.BlockCommittedEvent;
import com.exonum.binding.service.InternalServerError;
import com.exonum.binding.service.Node;
import com.exonum.binding.service.Schema;
import com.exonum.binding.service.TransactionConverter;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.indices.ListIndex;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.storage.indices.ProofListIndexProxy;
import com.exonum.binding.transaction.RawTransaction;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import io.vertx.ext.web.Router;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
    return submitTransaction(CreateCounterTx.converter().toRawTransaction(tx));
  }

  @Override
  public HashCode submitIncrementCounter(long requestSeed, HashCode counterId) {
    IncrementCounterTx tx = new IncrementCounterTx(requestSeed, counterId);

    return submitTransaction(IncrementCounterTx.converter().toRawTransaction(tx));
  }


  @Override
  public HashCode submitValidThrowingTx(long requestSeed) {
    ThrowingTx tx = new ThrowingTx(requestSeed);

    return submitTransaction(ThrowingTx.converter().toRawTransaction(tx));
  }

  @Override
  public HashCode submitValidErrorTx(long requestSeed, byte errorCode,
      @Nullable String description) {
    ErrorTx tx = new ErrorTx(requestSeed, errorCode, description);

    return submitTransaction(ErrorTx.converter().toRawTransaction(tx));
  }

  @Override
  public HashCode submitUnknownTx() {
    return submitTransaction(UnknownTx.createRawTransaction());
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
  public List<HashCode> getAllBlockHashes() {
    return node.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      ListIndex<HashCode> hashes = blockchain.getAllBlockHashes();

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

  private void createCounter(String name, Fork fork) {
    QaSchema schema = new QaSchema(fork);
    MapIndex<HashCode, Long> counters = schema.counters();
    MapIndex<HashCode, String> names = schema.counterNames();

    HashCode counterId = defaultHashFunction().hashString(name, UTF_8);
    if (counters.containsKey(counterId)) {
      return;
    }
    counters.put(counterId, 0L);
    names.put(counterId, name);
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
  private HashCode submitTransaction(RawTransaction rawTransaction) {
    checkBlockchainInitialized();
    try {
      node.submitTransaction(rawTransaction);
      // TODO: return message hash from the core
      return null;
    } catch (InternalServerError e) {
      throw new RuntimeException("Propagated transaction submission exception", e);
    }
  }

  private void checkBlockchainInitialized() {
    checkState(node != null, "Service has not been fully initialized yet");
  }
}
