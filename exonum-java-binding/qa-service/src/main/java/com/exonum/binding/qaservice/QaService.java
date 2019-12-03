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
import com.exonum.binding.core.transaction.RawTransaction;
import com.exonum.core.messages.Blockchain.Config;
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
  HashCode submitIncrementCounter(long requestSeed, HashCode counterId);

  /**
   * Creates a new self-signed 'unknown' transaction and submits
   * it through the {@link com.exonum.binding.core.service.Node}.
   * Enables testing of {@link Node#submitTransaction(RawTransaction)}.
   */
  HashCode submitUnknownTx();

  Optional<Counter> getValue(HashCode counterId);

  Config getConsensusConfiguration();

  Optional<ZonedDateTime> getTime();

  Map<PublicKey, ZonedDateTime> getValidatorsTimes();
}
