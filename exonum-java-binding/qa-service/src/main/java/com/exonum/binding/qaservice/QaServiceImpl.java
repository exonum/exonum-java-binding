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

package com.exonum.binding.qaservice;

import static com.exonum.binding.common.hash.Hashing.defaultHashFunction;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.Hashing;
import com.exonum.binding.core.blockchain.Blockchain;
import com.exonum.binding.core.runtime.ServiceInstanceSpec;
import com.exonum.binding.core.service.AbstractService;
import com.exonum.binding.core.service.BlockCommittedEvent;
import com.exonum.binding.core.service.Configuration;
import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.storage.database.View;
import com.exonum.binding.core.storage.indices.EntryIndexProxy;
import com.exonum.binding.core.storage.indices.MapIndex;
import com.exonum.binding.core.transaction.RawTransaction;
import com.exonum.binding.qaservice.Config.QaConfiguration;
import com.exonum.binding.qaservice.transactions.IncrementCounterTx;
import com.exonum.binding.qaservice.transactions.UnknownTx;
import com.exonum.binding.time.TimeSchema;
import com.exonum.core.messages.Blockchain.Config;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import io.vertx.ext.web.Router;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
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
public final class QaServiceImpl extends AbstractService implements QaService {

  private static final Logger logger = LogManager.getLogger(QaService.class);

  @VisibleForTesting
  static final String DEFAULT_COUNTER_NAME = "default";

  @VisibleForTesting
  static final String AFTER_COMMIT_COUNTER_NAME = "after_commit_counter";

  @Nullable
  private Node node;

  @Inject
  public QaServiceImpl(ServiceInstanceSpec instanceSpec) {
    super(instanceSpec);
  }

  @Override
  protected QaSchema createDataSchema(View view) {
    return new QaSchema(view, getName());
  }

  @Override
  public List<HashCode> getStateHashes(Snapshot snapshot) {
    List<HashCode> stateHashes = super.getStateHashes(snapshot);
    // Log the state hashes, so that the values passed to the native part of the framework
    // are known.
    logger.info("state hashes: {}", stateHashes);
    return stateHashes;
  }

  @Override
  public void initialize(Fork fork, Configuration configuration) {
    // Init the time oracle
    updateTimeOracle(fork, configuration);

    // Add a default counter to the blockchain.
    createCounter(DEFAULT_COUNTER_NAME, fork);

    // Add an afterCommit counter that will be incremented after each block committed event.
    createCounter(AFTER_COMMIT_COUNTER_NAME, fork);
  }

  private void createCounter(String name, Fork fork) {
    QaSchema schema = createDataSchema(fork);
    MapIndex<HashCode, Long> counters = schema.counters();
    MapIndex<HashCode, String> names = schema.counterNames();

    HashCode counterId = defaultHashFunction().hashString(name, UTF_8);
    checkState(!counters.containsKey(counterId), "Counter %s already exists", name);

    counters.put(counterId, 0L);
    names.put(counterId, name);
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
  public HashCode submitIncrementCounter(long requestSeed, HashCode counterId) {
    RawTransaction tx = IncrementCounterTx.newRawTransaction(requestSeed, counterId, getId());

    return submitTransaction(tx);
  }

  @Override
  public HashCode submitUnknownTx() {
    return submitTransaction(UnknownTx.newRawTransaction(getId()));
  }

  @Override
  @SuppressWarnings("ConstantConditions")  // Node is not null.
  public Optional<Counter> getValue(HashCode counterId) {
    checkBlockchainInitialized();

    return node.withSnapshot((view) -> {
      QaSchema schema = createDataSchema(view);
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
  public Config getConsensusConfiguration() {
    checkBlockchainInitialized();

    return node.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);

      return blockchain.getConsensusConfiguration();
    });
  }

  @Override
  @SuppressWarnings("ConstantConditions")  // Node is not null.
  public Optional<ZonedDateTime> getTime() {
    return node.withSnapshot(s -> {
      TimeSchema timeOracle = createDataSchema(s).timeSchema();
      EntryIndexProxy<ZonedDateTime> currentTime = timeOracle.getTime();
      return currentTime.toOptional();
    });
  }

  @Override
  @SuppressWarnings("ConstantConditions")  // Node is not null.
  public Map<PublicKey, ZonedDateTime> getValidatorsTimes() {
    return node.withSnapshot(s -> {
      TimeSchema timeOracle = createDataSchema(s).timeSchema();
      MapIndex<PublicKey, ZonedDateTime> validatorsTimes = timeOracle.getValidatorsTimes();
      return toMap(validatorsTimes);
    });
  }

  private <K, V> Map<K, V> toMap(MapIndex<K, V> mapIndex) {
    return Maps.toMap(mapIndex.keys(), mapIndex::get);
  }

  @SuppressWarnings("ConstantConditions") // Node is not null.
  private HashCode submitTransaction(RawTransaction rawTransaction) {
    checkBlockchainInitialized();
    return node.submitTransaction(rawTransaction);
  }

  private void checkBlockchainInitialized() {
    checkState(node != null, "Service has not been fully initialized yet");
  }

  @Override
  public void verifyConfiguration(Fork fork, Configuration configuration) {
    QaConfiguration config = configuration.getAsMessage(QaConfiguration.class);
    checkConfiguration(config);
  }

  @Override
  public void applyConfiguration(Fork fork, Configuration configuration) {
    updateTimeOracle(fork, configuration);
  }

  private void checkConfiguration(QaConfiguration config) {
    String timeOracleName = config.getTimeOracleName();
    // Check the time oracle name is non-empty.
    // We do *not* check if the time oracle is active to (a) allow running this service with
    // reduced read functionality without time oracle; (b) testing time schema when it is not
    // active.
    checkArgument(!Strings.isNullOrEmpty(timeOracleName), "Empty time oracle name: %s",
        timeOracleName);
  }

  private void updateTimeOracle(Fork fork, Configuration configuration) {
    QaSchema schema = createDataSchema(fork);
    QaConfiguration config = configuration.getAsMessage(QaConfiguration.class);

    // Verify the configuration
    checkConfiguration(config);

    // Save the configuration
    String timeOracleName = config.getTimeOracleName();
    schema.timeOracleName().set(timeOracleName);
  }
}
