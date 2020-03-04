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

import static com.exonum.binding.common.serialization.StandardSerializers.protobuf;
import static com.exonum.binding.core.transaction.ExecutionPreconditions.checkExecution;
import static com.exonum.binding.qaservice.QaExecutionError.COUNTER_ALREADY_EXISTS;
import static com.exonum.binding.qaservice.QaExecutionError.EMPTY_TIME_ORACLE_NAME;
import static com.exonum.binding.qaservice.QaExecutionError.RESUME_SERVICE_ERROR;
import static com.exonum.binding.qaservice.QaExecutionError.UNKNOWN_COUNTER;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.core.blockchain.Blockchain;
import com.exonum.binding.core.blockchain.BlockchainData;
import com.exonum.binding.core.runtime.ServiceInstanceSpec;
import com.exonum.binding.core.service.AbstractService;
import com.exonum.binding.core.service.BlockCommittedEvent;
import com.exonum.binding.core.service.Configuration;
import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.storage.indices.MapIndex;
import com.exonum.binding.core.storage.indices.ProofEntryIndex;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.core.transaction.ExecutionException;
import com.exonum.binding.core.transaction.RawTransaction;
import com.exonum.binding.core.transaction.Transaction;
import com.exonum.binding.core.transaction.TransactionContext;
import com.exonum.binding.qaservice.Config.QaConfiguration;
import com.exonum.binding.qaservice.Config.QaResumeArguments;
import com.exonum.binding.qaservice.transactions.TxMessageProtos;
import com.exonum.binding.time.TimeSchema;
import com.exonum.messages.core.Blockchain.Config;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import io.vertx.ext.web.Router;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;

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

  static final int CREATE_COUNTER_TX_ID = 0;
  static final int INCREMENT_COUNTER_TX_ID = 1;
  static final int VALID_THROWING_TX_ID = 12;
  static final int VALID_ERROR_TX_ID = 13;

  @VisibleForTesting
  static final short UNKNOWN_TX_ID = 9999;

  @VisibleForTesting
  static final String DEFAULT_COUNTER_NAME = "default";

  @VisibleForTesting
  static final String AFTER_COMMIT_COUNTER_NAME = "after_commit_counter";

  @VisibleForTesting
  static final String BEFORE_TXS_COUNTER_NAME = "before_txs";

  @VisibleForTesting
  static final String AFTER_TXS_COUNTER_NAME = "after_txs";

  @Nullable
  private Node node;

  @Inject
  public QaServiceImpl(ServiceInstanceSpec instanceSpec) {
    super(instanceSpec);
  }

  private QaSchema createDataSchema(BlockchainData blockchainData) {
    return new QaSchema(blockchainData);
  }

  @Override
  public void initialize(TransactionContext context, Configuration configuration) {
    // Init the time oracle
    updateTimeOracle(context, configuration);

    // Create the default counters:
    Stream.of(
        DEFAULT_COUNTER_NAME,
        BEFORE_TXS_COUNTER_NAME,
        AFTER_TXS_COUNTER_NAME,
        AFTER_COMMIT_COUNTER_NAME)
        .forEach(name -> createCounter(name, context));
  }

  @Override
  public void resume(TransactionContext context, byte[] arguments) {
    QaResumeArguments resumeArguments = parseResumeArguments(arguments);

    checkExecution(!resumeArguments.getShouldThrowException(), RESUME_SERVICE_ERROR.code);

    createCounter(resumeArguments.getCounterName(), context);
  }

  @Override
  public void createPublicApiHandlers(Node node, Router router) {
    this.node = node;

    ApiController controller = new ApiController(this);
    controller.mountApi(router);
  }

  @Override
  public void beforeTransactions(TransactionContext context) {
    incrementCounter(BEFORE_TXS_COUNTER_NAME, context);
  }

  @Override
  public void afterTransactions(TransactionContext context) {
    incrementCounter(AFTER_TXS_COUNTER_NAME, context);
  }

  /**
   * Increments the afterCommit counter so the number of times this method was invoked is stored
   * in it.
   */
  @Override
  public void afterCommit(BlockCommittedEvent event) {
    long seed = event.getHeight();
    submitIncrementCounter(seed, AFTER_COMMIT_COUNTER_NAME);
  }

  @Override
  public HashCode submitIncrementCounter(long requestSeed, String counterName) {
    RawTransaction tx = newRawIncrementCounterTransaction(requestSeed, counterName, getId());

    return submitTransaction(tx);
  }

  /**
   * Creates a new raw transaction of this type with the given parameters.
   *
   * @param requestSeed transaction id
   * @param counterName the counter name
   * @param serviceId the id of QA service
   */
  private static RawTransaction newRawIncrementCounterTransaction(long requestSeed,
      String counterName, int serviceId) {
    byte[] payload = TxMessageProtos.IncrementCounterTxBody.newBuilder()
        .setSeed(requestSeed)
        .setCounterName(counterName)
        .build().toByteArray();

    return RawTransaction.newBuilder()
        .serviceId(serviceId)
        .transactionId(INCREMENT_COUNTER_TX_ID)
        .payload(payload)
        .build();
  }

  /**
   * Submit a transaction that has QA service identifier, but an unknown transaction id.
   * Such transaction must be rejected when received by other nodes.
   *
   * <p>Only a single unknown transaction may be submitted to each node,
   * as they have empty body (= the same binary representation),
   * and once it is added to the local pool of a certain node,
   * it will remain there. Other nodes must reject the message of this transaction
   * once they receive it as a message from this node. If multiple unknown transaction messages
   * need to be submitted, a seed might be added.
   */
  @Override
  public HashCode submitUnknownTx() {
    return submitTransaction(newRawUnknownTransaction(getId()));
  }

  /**
   * Returns raw transaction.
   */
  private static RawTransaction newRawUnknownTransaction(int serviceId) {
    return RawTransaction.newBuilder()
        .serviceId(serviceId)
        .transactionId(UNKNOWN_TX_ID)
        .payload(new byte[0])
        .build();
  }

  @Override
  @SuppressWarnings("ConstantConditions")  // Node is not null.
  public Optional<Counter> getValue(String counterName) {
    checkBlockchainInitialized();

    return node.withBlockchainData((snapshot) -> {
      QaSchema schema = createDataSchema(snapshot);
      MapIndex<String, Long> counters = schema.counters();
      return Optional.ofNullable(counters.get(counterName))
          .map(value -> new Counter(counterName, value));
    });
  }

  @Override
  public Config getConsensusConfiguration() {
    checkBlockchainInitialized();

    return node.withBlockchainData((blockchainData) -> {
      Blockchain blockchain = blockchainData.getBlockchain();

      return blockchain.getConsensusConfiguration();
    });
  }

  @Override
  @SuppressWarnings("ConstantConditions")  // Node is not null.
  public Optional<ZonedDateTime> getTime() {
    return node.withBlockchainData(s -> {
      TimeSchema timeOracle = createDataSchema(s).timeSchema();
      ProofEntryIndex<ZonedDateTime> currentTime = timeOracle.getTime();
      return currentTime.toOptional();
    });
  }

  @Override
  @SuppressWarnings("ConstantConditions")  // Node is not null.
  public Map<PublicKey, ZonedDateTime> getValidatorsTimes() {
    return node.withBlockchainData(s -> {
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
   * @throws ExecutionException if time oracle name is empty
   */
  @Override
  public void verifyConfiguration(TransactionContext context, Configuration configuration) {
    QaConfiguration config = configuration.getAsMessage(QaConfiguration.class);
    checkConfiguration(config);
  }

  @Override
  public void applyConfiguration(TransactionContext context, Configuration configuration) {
    updateTimeOracle(context, configuration);
  }

  @Override
  @Transaction(CREATE_COUNTER_TX_ID)
  public void createCounter(TxMessageProtos.CreateCounterTxBody arguments,
      TransactionContext context) {
    String counterName = arguments.getName();
    createCounter(counterName, context);
  }

  private void createCounter(String counterName, TransactionContext context) {
    checkArgument(!counterName.trim().isEmpty(), "Name must not be blank: '%s'", counterName);

    QaSchema schema = createDataSchema(context.getBlockchainData());
    MapIndex<String, Long> counters = schema.counters();

    checkExecution(!counters.containsKey(counterName),
        COUNTER_ALREADY_EXISTS.code, "Counter %s already exists", counterName);

    counters.put(counterName, 0L);
  }

  @Override
  @Transaction(INCREMENT_COUNTER_TX_ID)
  public void incrementCounter(TxMessageProtos.IncrementCounterTxBody arguments,
      TransactionContext context) {
    String counterName = arguments.getCounterName();
    incrementCounter(counterName, context);
  }

  private void incrementCounter(String counterName, TransactionContext context) {
    QaSchema schema = createDataSchema(context.getBlockchainData());
    ProofMapIndexProxy<String, Long> counters = schema.counters();

    // Increment the counter if there is such.
    checkExecution(counters.containsKey(counterName), UNKNOWN_COUNTER.code);

    long newValue = counters.get(counterName) + 1;
    counters.put(counterName, newValue);
  }

  @Override
  @Transaction(VALID_THROWING_TX_ID)
  public void throwing(TxMessageProtos.ThrowingTxBody arguments, TransactionContext context) {
    QaSchema schema = createDataSchema(context.getBlockchainData());

    // Attempt to clear all service indices.
    schema.clearAll();

    throw new IllegalStateException(format("#execute of this transaction always throws "
            + "(seed=%d, txHash=%s)", arguments.getSeed(), context.getTransactionMessageHash()));
  }

  @Override
  @Transaction(VALID_ERROR_TX_ID)
  public void error(TxMessageProtos.ErrorTxBody arguments, TransactionContext context) {
    int errorCode = arguments.getErrorCode();
    checkArgument(0 <= errorCode && errorCode <= 127,
        "error code (%s) must be in range [0; 127]", errorCode);
    QaSchema schema = createDataSchema(context.getBlockchainData());

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
    checkExecution(!Strings.isNullOrEmpty(timeOracleName), EMPTY_TIME_ORACLE_NAME.code,
        "Empty time oracle name: %s", timeOracleName);
  }

  private void updateTimeOracle(TransactionContext context, Configuration configuration) {
    QaSchema schema = createDataSchema(context.getBlockchainData());
    QaConfiguration config = configuration.getAsMessage(QaConfiguration.class);

    // Verify the configuration
    checkConfiguration(config);

    // Save the configuration
    String timeOracleName = config.getTimeOracleName();
    schema.timeOracleName().set(timeOracleName);
  }

  private QaResumeArguments parseResumeArguments(byte[] arguments) {
    Serializer<QaResumeArguments> serializer = protobuf(QaResumeArguments.class);
    return serializer.fromBytes(arguments);
  }
}
