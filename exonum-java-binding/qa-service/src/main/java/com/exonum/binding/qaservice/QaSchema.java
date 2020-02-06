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

import static com.exonum.binding.common.serialization.StandardSerializers.string;
import static com.exonum.binding.common.serialization.StandardSerializers.uint64;
import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.core.blockchain.BlockchainData;
import com.exonum.binding.core.service.Schema;
import com.exonum.binding.core.storage.database.Prefixed;
import com.exonum.binding.core.storage.indices.IndexAddress;
import com.exonum.binding.core.storage.indices.ProofEntryIndexProxy;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.time.TimeSchema;

/**
 * A schema of the QA service.
 */
public final class QaSchema implements Schema {

  private static final IndexAddress TIME_ORACLE_NAME_ADDRESS =
      IndexAddress.valueOf("time_oracle_name");
  private static final IndexAddress COUNTERS_ADDRESS = IndexAddress.valueOf("counters");

  private final BlockchainData blockchainData;
  private final Prefixed access;

  public QaSchema(BlockchainData blockchainData) {
    this.blockchainData = checkNotNull(blockchainData);
    this.access = blockchainData.getExecutingServiceData();
  }

  /**
   * Returns the index containing the name of the time oracle to use.
   */
  public ProofEntryIndexProxy<String> timeOracleName() {
    return access.getProofEntry(TIME_ORACLE_NAME_ADDRESS, string());
  }

  /**
   * Returns the time schema of the time oracle this qa service uses.
   * {@link #timeOracleName()} must be non-empty.
   */
  public TimeSchema timeSchema() {
    return TimeSchema.newInstance(blockchainData, timeOracleName().get());
  }

  /**
   * Returns a proof map of counter values. Note that this is a proof map that uses non-hashed keys.
   */
  public ProofMapIndexProxy<String, Long> counters() {
    return access.getProofMap(COUNTERS_ADDRESS, string(), uint64());
  }

  /** Clears all collections of the service. */
  public void clearAll() {
    counters().clear();
  }
}
