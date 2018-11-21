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

/**
 * Public keys of validator nodes.
 * <ul>
 *   <li>consensus_key - validator’s public key for use with consensus messages</li>
 *   <li>service_key - validator’s public key for use with service transactions</li>
 * </ul>
 */
@AutoValue
@GenerateTypeAdapter
public abstract class ValidatorKey {
  @SerializedName("consensus_key")
  public abstract HashCode consensusKey();

  @SerializedName("service_key")
  public abstract HashCode serviceKey();

  /**
   * Creates a new ValidatorKey from the given parameters.
   */
  public static ValidatorKey create(HashCode consensusKey, HashCode serviceKey) {
    return new AutoValue_ValidatorKey(consensusKey, serviceKey);
  }
}
