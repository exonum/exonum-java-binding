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
import com.google.gson.annotations.SerializedName;
import com.ryanharter.auto.value.gson.GenerateTypeAdapter;
import java.util.List;

@AutoValue
@GenerateTypeAdapter
public abstract class StoredConfiguration {
  @SerializedName("previous_cfg_hash")
  public abstract HashCode previousCfgHash();

  @SerializedName("actual_from")
  public abstract long actualFrom();

  @SerializedName("validator_keys")
  public abstract List<ValidatorKey> validatorKeys();

  @SerializedName("consensus_config")
  public abstract ConsensusConfig consensusConfig();

  //TODO add majorityCount, services fields

  public static StoredConfiguration create(HashCode previousCfgHash, long actualFrom,
      List<ValidatorKey> validatorKeys, ConsensusConfig consensusConfig) {
    return new AutoValue_StoredConfiguration(previousCfgHash, actualFrom, validatorKeys,
        consensusConfig);
  }
}
