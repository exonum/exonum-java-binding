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

import com.exonum.binding.common.crypto.PublicKey;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

/**
 * Public keys of validator nodes.
 */
@AutoValue
public abstract class ValidatorKey {
  /**
   * Consensus key is used for messages related to the consensus algorithm.
   */
  @SerializedName("consensus_key")
  public abstract PublicKey consensusKey();

  /**
   * Service key is used for services, for example, the configuration updater service.
   */
  @SerializedName("service_key")
  public abstract PublicKey serviceKey();

  /**
   * Provides a Gson type adapter for this class.
   *
   * @see com.exonum.binding.common.serialization.json.StoredConfigurationAdapterFactory
   */
  public static TypeAdapter<ValidatorKey> typeAdapter(Gson gson) {
    return new AutoValue_ValidatorKey.GsonTypeAdapter(gson);
  }

  public static ValidatorKey.Builder builder() {
    return new AutoValue_ValidatorKey.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder consensusKey(PublicKey consensusKey);

    public abstract Builder serviceKey(PublicKey serviceKey);

    public abstract ValidatorKey build();
  }
}
