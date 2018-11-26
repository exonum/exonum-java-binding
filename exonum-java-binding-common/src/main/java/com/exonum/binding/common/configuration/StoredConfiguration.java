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

import com.exonum.binding.common.hash.HashCode;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Represents a blockchain configuration which is a set of values that determine
 * the network access parameters of a node and behavior of the node while operating in the network.
 *
 * <p>See <a href="https://exonum.com/doc/architecture/configuration/">Exonum configuration</a> for
 * configuration details.
 *
 * <p>Services configuration parameters would be available after
 * (https://jira.bf.local/browse/ECR-2683) would be implemented.
 */
@AutoValue
public abstract class StoredConfiguration {

  /**
   * Hash of the previous configuration, which can be used to find that configuration.
   */
  @SerializedName("previous_cfg_hash")
  public abstract HashCode previousCfgHash();

  /**
   * The height, starting from which this configuration becomes actual.
   */
  @SerializedName("actual_from")
  public abstract long actualFrom();

  /**
   * List of validators consensus and service public keys.
   */
  @SerializedName("validator_keys")
  public abstract List<ValidatorKey> validatorKeys();

  /**
   * Consensus algorithm parameters.
   */
  @SerializedName("consensus")
  public abstract ConsensusConfiguration consensusConfiguration();

  //TODO add majorityCount, services fields (https://jira.bf.local/browse/ECR-2683)

  /**
   * Provides a Gson type adapter for this class.
   *
   * @see com.exonum.binding.common.serialization.json.StoredConfigurationAdapterFactory
   */
  public static TypeAdapter<StoredConfiguration> typeAdapter(Gson gson) {
    return new AutoValue_StoredConfiguration.GsonTypeAdapter(gson);
  }

  public static Builder builder() {
    return new AutoValue_StoredConfiguration.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder previousCfgHash(HashCode previousCfgHash);

    public abstract Builder actualFrom(long actualFrom);

    public abstract Builder validatorKeys(List<ValidatorKey> validatorKeys);

    public abstract Builder consensusConfiguration(ConsensusConfiguration consensusConfiguration);

    public abstract StoredConfiguration build();
  }
}
