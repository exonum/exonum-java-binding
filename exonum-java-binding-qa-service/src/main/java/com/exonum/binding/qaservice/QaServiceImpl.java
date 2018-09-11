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

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.qaservice.transactions.CreateCounterTx;
import com.exonum.binding.qaservice.transactions.IncrementCounterTx;
import com.exonum.binding.qaservice.transactions.InvalidThrowingTx;
import com.exonum.binding.qaservice.transactions.InvalidTx;
import com.exonum.binding.qaservice.transactions.UnknownTx;
import com.exonum.binding.qaservice.transactions.ValidErrorTx;
import com.exonum.binding.qaservice.transactions.ValidThrowingTx;
import com.exonum.binding.service.AbstractService;
import com.exonum.binding.service.InternalServerError;
import com.exonum.binding.service.InvalidTransactionException;
import com.exonum.binding.service.Node;
import com.exonum.binding.service.Schema;
import com.exonum.binding.service.TransactionConverter;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.transaction.Transaction;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import io.vertx.ext.web.Router;
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
    String defaultCounterName = "default";
    new CreateCounterTx(defaultCounterName)
        .execute(fork);

    return Optional.of(INITIAL_SERVICE_CONFIGURATION);
  }

  @Override
  public void createPublicApiHandlers(Node node, Router router) {
    this.node = node;

    ApiController controller = new ApiController(this);
    controller.mountApi(router);
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
