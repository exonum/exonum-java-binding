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

@AutoValue
@GenerateTypeAdapter
public abstract class ConsensusConfig {
  @SerializedName("round_timeout")
  public abstract long roundTimeout();

  @SerializedName("status_timeout")
  public abstract long statusTimeout();

  @SerializedName("peers_timeout")
  public abstract long peersTimeout();

  @SerializedName("txs_block_limit")
  public abstract long txsBlockLimit();

  @SerializedName("max_message_len")
  public abstract long maxMessageLen();

  @SerializedName("min_propose_timeout")
  public abstract long minProposeTimeout();

  @SerializedName("max_propose_timeout")
  public abstract long maxProposeTimeout();

  @SerializedName("propose_timeout_threshold")
  public abstract long proposeTimeoutThreshold();

  public static ConsensusConfig create(long roundTimeout, long statusTimeout, long peersTimeout,
      long txsBlockLimit, long maxMessageLen, long minProposeTimeout, long maxProposeTimeout,
      long proposeTimeoutThreshold) {
    return new AutoValue_ConsensusConfig(roundTimeout, statusTimeout, peersTimeout, txsBlockLimit,
        maxMessageLen, minProposeTimeout, maxProposeTimeout, proposeTimeoutThreshold);
  }
}
