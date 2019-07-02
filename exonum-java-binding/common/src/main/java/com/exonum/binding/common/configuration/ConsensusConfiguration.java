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
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

/**
 * Blockchain Consensus algorithm parameters.
 *
 * <p>See <a href="https://exonum.com/doc/version/0.11/architecture/configuration/">Exonum configuration</a> for
 * Consensus configuration details.
 */
@AutoValue
public abstract class ConsensusConfiguration {

  /**
   * Interval between first two rounds. This interval defines the time that passes
   * between the moment a new block is committed to the blockchain and the
   * time when second round starts, regardless of whether a new block has
   * been committed during this period or not.
   *
   * <p>Note that rounds in Exonum do not have a defined end time. Nodes in a new round can
   * continue to vote for proposals and process messages related to previous rounds.
   */
  @SerializedName("first_round_timeout")
  public abstract long firstRoundTimeout();

  /**
   * This parameter defines the frequency with which a node broadcasts its status message to the
   * network.
   */
  @SerializedName("status_timeout")
  public abstract long statusTimeout();

  /**
   * This parameter defines the frequency with which a node requests collected Connect messages
   * from a random peer node in the network.
   */
  @SerializedName("peers_timeout")
  public abstract long peersTimeout();

  /**
   * Maximum number of transactions per block.
   */
  @SerializedName("txs_block_limit")
  public abstract int txsBlockLimit();

  /**
   * This parameter determines the maximum size of both consensus messages and transactions.
   */
  @SerializedName("max_message_len")
  public abstract int maxMessageLen();

  /**
   * Minimal propose timeout.
   */
  @SerializedName("min_propose_timeout")
  public abstract long minProposeTimeout();

  /**
   * Maximal propose timeout.
   */
  @SerializedName("max_propose_timeout")
  public abstract long maxProposeTimeout();

  /**
   * Amount of transactions in pool to start use min_propose_timeout.
   */
  @SerializedName("propose_timeout_threshold")
  public abstract int proposeTimeoutThreshold();

  /**
   * Provides a Gson type adapter for this class.
   *
   * @see com.exonum.binding.common.serialization.json.CommonTypeAdapterFactory
   */
  public static TypeAdapter<ConsensusConfiguration> typeAdapter(Gson gson) {
    return new AutoValue_ConsensusConfiguration.GsonTypeAdapter(gson);
  }

  public static ConsensusConfiguration.Builder builder() {
    return new AutoValue_ConsensusConfiguration.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder firstRoundTimeout(long firstRoundTimeout);

    public abstract Builder statusTimeout(long statusTimeout);

    public abstract Builder peersTimeout(long peersTimeout);

    public abstract Builder txsBlockLimit(int txsBlockLimit);

    public abstract Builder maxMessageLen(int maxMessageLen);

    public abstract Builder minProposeTimeout(long minProposeTimeout);

    public abstract Builder maxProposeTimeout(long maxProposeTimeout);

    public abstract Builder proposeTimeoutThreshold(int proposeTimeoutThreshold);

    public abstract ConsensusConfiguration build();
  }
}
