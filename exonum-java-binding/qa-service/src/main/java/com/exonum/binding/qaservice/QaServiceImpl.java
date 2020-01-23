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

import static com.exonum.binding.qaservice.QaExecutionError.COUNTER_ALREADY_EXISTS;
import static com.exonum.binding.qaservice.QaExecutionError.EMPTY_TIME_ORACLE_NAME;
import static com.exonum.binding.qaservice.QaExecutionError.UNKNOWN_COUNTER;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
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
import com.exonum.binding.core.storage.database.Access;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.indices.MapIndex;
import com.exonum.binding.core.storage.indices.ProofEntryIndexProxy;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.core.transaction.ExecutionException;
import com.exonum.binding.core.transaction.RawTransaction;
import com.exonum.binding.core.transaction.Transaction;
import com.exonum.binding.core.transaction.TransactionContext;
import com.exonum.binding.qaservice.Config.QaConfiguration;
import com.exonum.binding.qaservice.transactions.TxMessageProtos;
import com.exonum.binding.time.TimeSchema;
import com.exonum.core.messages.Blockchain.Config;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import io.vertx.ext.web.Router;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * A simple QA service.
 *
 * @implNote This service is meant to be used to test integration of Exonum Java Binding with Exonum
 *     Core. It contains very little business-logic so the QA team can focus on testing the
 *     integration <em>through</em> this service, not the service itself.
 *     <p>Such service is not meant to be illustrative, it breaks multiple recommendations on
 *     implementing Exonum Services, therefore, it shall NOT be used as an example of a user
 *     service.
 */
public final class QaServiceImpl extends AbstractService implements QaService {

  static final int CREATE_COUNTER_TX_ID = 0;
  static final int INCREMENT_COUNTER_TX_ID = 1;
  static final int VALID_THROWING_TX_ID = 12;
  static final int VALID_ERROR_TX_ID = 13;

  @VisibleForTesting static final short UNKNOWN_TX_ID = 9999;

  @VisibleForTesting static final String DEFAULT_COUNTER_NAME = "default";

  @VisibleForTesting static final String AFTER_COMMIT_COUNTER_NAME = "after_commit_counter";

  @Nullable private Node node;

  @Inject
  public QaServiceImpl(ServiceInstanceSpec instanceSpec) {
    super(instanceSpec);
  }

  @Override
  protected QaSchema createDataSchema(Access access) {
    return new QaSchema(access, getName());
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

  @Override
  public void createPublicApiHandlers(Node node, Router router) {
    this.node = node;

    ApiController controller = new ApiController(this);
    controller.mountApi(router);
  }

  /**
   * Increments the afterCommit counter so the number of times this method was invoked is stored in
   * it.
   */
  @Override
  public void afterCommit(BlockCommittedEvent event) {
    long seed = event.getHeight();
    HashCode counterId =
        Hashing.sha256().hashString(AFTER_COMMIT_COUNTER_NAME, StandardCharsets.UTF_8);
    submitIncrementCounter(seed, counterId);
  }

  @Override
  public HashCode submitIncrementCounter(long requestSeed, HashCode counterId) {
    RawTransaction tx = newRawIncrementCounterTransaction(requestSeed, counterId, getId());

    return submitTransaction(tx);
  }

  /**
   * Creates a new raw transaction of this type with the given parameters.
   *
   * @param requestSeed transaction id
   * @param counterId counter id, a hash of the counter name
   * @param serviceId the id of QA service
   */
  private static RawTransaction newRawIncrementCounterTransaction(
      long requestSeed, HashCode counterId, int serviceId) {
    byte[] payload =
        TxMessageProtos.IncrementCounterTxBody.newBuilder()
            .setSeed(requestSeed)
            .setCounterId(ByteString.copyFrom(counterId.asBytes()))
            .build()
            .toByteArray();

    return RawTransaction.newBuilder()
        .serviceId(serviceId)
        .transactionId(INCREMENT_COUNTER_TX_ID)
        .payload(payload)
        .build();
  }

  /**
   * Submit a transaction that has QA service identifier, but an unknown transaction id. Such
   * transaction must be rejected when received by other nodes.
   *
   * <p>Only a single unknown transaction may be submitted to each node, as they have empty body (=
   * the same binary representation), and once it is added to the local pool of a certain node, it
   * will remain there. Other nodes must reject the message of this transaction once they receive it
   * as a message from this node. If multiple unknown transaction messages need to be submitted, a
   * seed might be added.
   */
  @Override
  public HashCode submitUnknownTx() {
    return submitTransaction(newRawUnknownTransaction(getId()));
  }

  /** Returns raw transaction. */
  private static RawTransaction newRawUnknownTransaction(int serviceId) {
    return RawTransaction.newBuilder()
        .serviceId(serviceId)
        .transactionId(UNKNOWN_TX_ID)
        .payload(new byte[0])
        .build();
  }

  @Override
  @SuppressWarnings("ConstantConditions") // Node is not null.
  public Optional<Counter> getValue(HashCode counterId) {
    checkBlockchainInitialized();

    return node.withSnapshot(
        (snapshot) -> {
          QaSchema schema = createDataSchema(snapshot);
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

    return node.withSnapshot(
        (snapshot) -> {
          Blockchain blockchain = Blockchain.newInstance(snapshot);

          return blockchain.getConsensusConfiguration();
        });
  }

  @Override
  @SuppressWarnings("ConstantConditions") // Node is not null.
  public Optional<ZonedDateTime> getTime() {
    return node.withSnapshot(
        s -> {
          TimeSchema timeOracle = createDataSchema(s).timeSchema();
          ProofEntryIndexProxy<ZonedDateTime> currentTime = timeOracle.getTime();
          return currentTime.toOptional();
        });
  }

  @Override
  @SuppressWarnings("ConstantConditions") // Node is not null.
  public Map<PublicKey, ZonedDateTime> getValidatorsTimes() {
    return node.withSnapshot(
        s -> {
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

  /**
   * Verifies the QA service configuration.
   *
   * @throws ExecutionException if time oracle name is empty
   */
  @Override
  public void verifyConfiguration(Fork fork, Configuration configuration) {
    QaConfiguration config = configuration.getAsMessage(QaConfiguration.class);
    checkConfiguration(config);
  }

  @Override
  public void applyConfiguration(Fork fork, Configuration configuration) {
    updateTimeOracle(fork, configuration);
  }

  @Override
  @Transaction(CREATE_COUNTER_TX_ID)
  public void createCounter(
      TxMessageProtos.CreateCounterTxBody arguments, TransactionContext context) {
    String name = arguments.getName();
    checkArgument(!name.trim().isEmpty(), "Name must not be blank: '%s'", name);

    createCounter(name, context.getFork());
  }

  private void createCounter(String counterName, Fork fork) {
    QaSchema schema = createDataSchema(fork);
    MapIndex<HashCode, Long> counters = schema.counters();
    MapIndex<HashCode, String> names = schema.counterNames();

    HashCode counterId = Hashing.defaultHashFunction().hashString(counterName, UTF_8);
    if (counters.containsKey(counterId)) {
      throw new ExecutionException(
          COUNTER_ALREADY_EXISTS.code, format("Counter %s already exists", counterName));
    }
    assert !names.containsKey(counterId) : "counterNames must not contain the id of " + counterName;

    counters.put(counterId, 0L);
    names.put(counterId, counterName);
  }

  @Override
  @Transaction(INCREMENT_COUNTER_TX_ID)
  public void incrementCounter(
      TxMessageProtos.IncrementCounterTxBody arguments, TransactionContext context) {
    byte[] rawCounterId = arguments.getCounterId().toByteArray();
    HashCode counterId = HashCode.fromBytes(rawCounterId);

    QaSchema schema = createDataSchema(context.getFork());
    ProofMapIndexProxy<HashCode, Long> counters = schema.counters();

    // Increment the counter if there is such.
    if (!counters.containsKey(counterId)) {
      throw new ExecutionException(UNKNOWN_COUNTER.code);
    }
    long newValue = counters.get(counterId) + 1;
    counters.put(counterId, newValue);
  }

  @Override
  @Transaction(VALID_THROWING_TX_ID)
  public void throwing(TxMessageProtos.ThrowingTxBody arguments, TransactionContext context) {
    QaSchema schema = createDataSchema(context.getFork());

    // Attempt to clear all service indices.
    schema.clearAll();

    throw new IllegalStateException(
        format(
            "#execute of this transaction always throws " + "(seed=%d, txHash=%s)",
            arguments.getSeed(), context.getTransactionMessageHash()));
  }

  @Override
  @Transaction(VALID_ERROR_TX_ID)
  public void error(TxMessageProtos.ErrorTxBody arguments, TransactionContext context) {
    int errorCode = arguments.getErrorCode();
    checkArgument(
        0 <= errorCode && errorCode <= 127, "error code (%s) must be in range [0; 127]", errorCode);
    QaSchema schema = createDataSchema(context.getFork());

    // Attempt to clear all service indices.
    schema.clearAll();

    // Throw an exception. Framework must revert the changes made above.
    String errorDescription = arguments.getErrorDescription();
    throw new ExecutionException((byte) errorCode, errorDescription);
  }

  private void checkConfiguration(QaConfiguration config) {
    String timeOracleName = config.getTimeOracleName();
    // Check the time oracle name is non-empty.
    // We do *not* check if the time oracle is active to (a) allow running this service with
    // reduced read functionality without time oracle; (b) testing time schema when it is not
    // active.
    if (Strings.isNullOrEmpty(timeOracleName)) {
      throw new ExecutionException(
          EMPTY_TIME_ORACLE_NAME.code, format("Empty time oracle name: %s", timeOracleName));
    }
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
