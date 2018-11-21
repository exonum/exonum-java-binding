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

package com.exonum.binding.common.configuration;

import com.google.auto.value.AutoValue;
import com.google.gson.annotations.SerializedName;
import com.ryanharter.auto.value.gson.GenerateTypeAdapter;

/**
 * Blockchain Consensus algorithm parameters.
 */
@AutoValue
@GenerateTypeAdapter
public abstract class ConsensusConfiguration {
  //TODO update to "first_round_timeout" after exonum 0.1.0 release
  //https://github.com/exonum/exonum/commit/b7c4bc471ddce70ec0de085cc82901271ec1544e
  @SerializedName("round_timeout")
  public abstract long roundTimeout();

  @SerializedName("status_timeout")
  public abstract long statusTimeout();

  @SerializedName("peers_timeout")
  public abstract long peersTimeout();

  @SerializedName("txs_block_limit")
  public abstract int txsBlockLimit();

  @SerializedName("max_message_len")
  public abstract int maxMessageLen();

  @SerializedName("min_propose_timeout")
  public abstract long minProposeTimeout();

  @SerializedName("max_propose_timeout")
  public abstract long maxProposeTimeout();

  @SerializedName("propose_timeout_threshold")
  public abstract int proposeTimeoutThreshold();

  /**
   * Creates a new ConsensusConfiguration from the given parameters.
   */
  public static ConsensusConfiguration create(long roundTimeout, long statusTimeout,
      long peersTimeout, int txsBlockLimit, int maxMessageLen, long minProposeTimeout,
      long maxProposeTimeout, int proposeTimeoutThreshold) {
    return new AutoValue_ConsensusConfiguration(roundTimeout, statusTimeout, peersTimeout,
        txsBlockLimit,  maxMessageLen, minProposeTimeout, maxProposeTimeout,
        proposeTimeoutThreshold);
  }
}
