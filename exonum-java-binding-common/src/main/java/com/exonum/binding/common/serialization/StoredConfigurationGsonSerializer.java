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

package com.exonum.binding.common.serialization;

import com.exonum.binding.common.configuration.StoredConfiguration;
import com.exonum.binding.common.hash.HashCode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ryanharter.auto.value.gson.GenerateTypeAdapter;

/**
 * A converter of {@link com.exonum.binding.common.configuration.StoredConfiguration}
 * instances to and from JSON representation.
 */
public final class StoredConfigurationGsonSerializer {

  private static final Gson GSON = new GsonBuilder()
      .registerTypeAdapter(HashCode.class, new HashCodeStringSerializer())
      .registerTypeAdapterFactory(GenerateTypeAdapter.FACTORY)
      .create();

  /**
   * Returns a configured instance of Gson.
   */
  public static Gson instance() {
    return GSON;
  }

  /**
   * Serializes instance of StoredConfiguration to its JSON representation.
   *
   * @return a JSON representation of StoredConfiguration object
   */
  public String toJson(StoredConfiguration configuration) {
    return GSON.toJson(configuration);
  }

  /**
   * Converts an instance of StoredConfiguration from its JSON representation.
   *
   * @return an instance of StoredConfiguration
   */
  public StoredConfiguration fromJson(String input) {
    return GSON.fromJson(input, StoredConfiguration.class);
  }
}
