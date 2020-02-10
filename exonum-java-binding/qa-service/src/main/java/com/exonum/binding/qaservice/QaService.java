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

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.core.service.Configurable;
import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.service.Service;
import com.exonum.binding.core.transaction.ExecutionException;
import com.exonum.binding.core.transaction.RawTransaction;
import com.exonum.binding.core.transaction.TransactionContext;
import com.exonum.binding.qaservice.transactions.TxMessageProtos;
import com.exonum.messages.core.Blockchain.Config;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * A simple service for QA purposes.
 */
public interface QaService extends Service, Configurable {

  /**
   * Creates a new self-signed 'increment counter' transaction and submits
   * it through the {@link com.exonum.binding.core.service.Node}.
   * Enables testing of {@link Node#submitTransaction(RawTransaction)}.
   */
  HashCode submitIncrementCounter(long requestSeed, String counterName);

  /**
   * Creates a new self-signed 'unknown' transaction and submits
   * it through the {@link com.exonum.binding.core.service.Node}.
   * Enables testing of {@link Node#submitTransaction(RawTransaction)}.
   */
  HashCode submitUnknownTx();

  Optional<Counter> getValue(String counterName);

  Config getConsensusConfiguration();

  Optional<ZonedDateTime> getTime();

  Map<PublicKey, ZonedDateTime> getValidatorsTimes();

  /**
   * Creates a new named counter.
   *
   * <p>Parameters:
   *  - name counter name, must not be blank
   *
   * @throws ExecutionException if the counter already exists
   * @throws IllegalArgumentException if the counter name is empty
   */
  void createCounter(TxMessageProtos.CreateCounterTxBody arguments, TransactionContext context);

  /**
   * Increments an existing counter.
   *
   * <p>Parameters:
   *  - seed transaction seed
   *  - counterName the counter name
   * @throws ExecutionException if a counter with the given id does not exist
   */
  void incrementCounter(TxMessageProtos.IncrementCounterTxBody arguments,
      TransactionContext context);

  /**
   * Clears all collections of this service and throws an exception with the given arguments.
   *
   * <p>This transaction will always throw an {@link ExecutionException},
   * therefore, have "error" status in the blockchain.
   *
   * <p>Parameters:
   * - a seed to distinguish transaction with the same parameters;
   * - an error code to include in the exception, must be in range [0; 127];
   * - an optional description to include in the exception. May be empty.
   *
   * @throws ExecutionException always; includes the given
   *     error code and error message
   * @throws IllegalArgumentException if the error code is not in range [0; 127]
   */
  void error(TxMessageProtos.ErrorTxBody arguments, TransactionContext context);

  /**
   * Clears all collections of this service and throws a runtime exception.
   *
   * <p>This transaction will always throw an {@link IllegalStateException},
   * therefore, have "unexpected error" status in the blockchain.
   *
   * @throws IllegalStateException always
   */
  void throwing(TxMessageProtos.ThrowingTxBody arguments, TransactionContext context);
}
