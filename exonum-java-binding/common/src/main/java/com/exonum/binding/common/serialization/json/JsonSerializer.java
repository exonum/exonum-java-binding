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
 *
 */

package com.exonum.binding.common.serialization.json;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;
import java.time.ZonedDateTime;

/**
 * Provides {@link Gson} serializer for converting Java objects to Json and vice versa.
 * It is configured to serialize Exonum objects in a format, compatible with the core framework
 * and light clients (e.g., {@link HashCode} as a hex string). If needed, a new serializer
 * with adapters for service-specific types can be {@linkplain #builder() created}, with
 * Exonum types support already included.
 */
public final class JsonSerializer {

  private static final Gson INSTANCE = builder().create();

  /**
   * Returns preconfigured {@link Gson} builder instance. Can be useful in cases when
   * some customization is required. For example, type adapters should be extended or replaced.
   */
  public static GsonBuilder builder() {
    return new GsonBuilder()
        .registerTypeHierarchyAdapter(TransactionMessage.class,
            new TransactionMessageJsonSerializer())
        .registerTypeHierarchyAdapter(HashCode.class, new HashCodeJsonSerializer())
        .registerTypeAdapter(PublicKey.class, new PublicKeyJsonSerializer())
        .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeJsonSerializer())
        .registerTypeAdapterFactory(StoredConfigurationAdapterFactory.create())
        .setLongSerializationPolicy(LongSerializationPolicy.STRING);
  }

  /**
   * Returns preconfigured {@link Gson} instance. Helpful in cases when no additional
   * {@linkplain #builder() configuration} of the Json serializer is required.
   */
  public static Gson json() {
    return INSTANCE;
  }

  private JsonSerializer() {
  }
}
