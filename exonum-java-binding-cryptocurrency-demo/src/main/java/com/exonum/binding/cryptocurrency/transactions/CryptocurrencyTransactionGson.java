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

package com.exonum.binding.cryptocurrency.transactions;

import com.exonum.binding.crypto.PublicKey;
import com.exonum.binding.hash.HashCode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.LongSerializationPolicy;
import java.lang.reflect.Type;

/** A converter of transaction parameters of cryptocurrency service to JSON. */
public final class CryptocurrencyTransactionGson {

  private static final Gson GSON =
      new GsonBuilder()
          .registerTypeHierarchyAdapter(HashCode.class, new HashCodeSerializer())
          .registerTypeAdapter(PublicKey.class, new PublicKeyJsonSerializer())
          .setLongSerializationPolicy(LongSerializationPolicy.STRING)
          .create();

  /** Returns a configured instance of Gson. */
  public static Gson instance() {
    return GSON;
  }

  private static class HashCodeSerializer
      implements JsonSerializer<HashCode>, JsonDeserializer<HashCode> {

    @Override
    public JsonElement serialize(HashCode src, Type typeOfSrc, JsonSerializationContext context) {
      return new JsonPrimitive(src.toString());
    }

    @Override
    public HashCode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      return HashCode.fromString(json.getAsString());
    }
  }

  private static class PublicKeyJsonSerializer
      implements JsonSerializer<PublicKey>, JsonDeserializer<PublicKey> {

    @Override
    public JsonElement serialize(PublicKey src, Type typeOfSrc, JsonSerializationContext context) {
      return new JsonPrimitive(src.toString());
    }

    @Override
    public PublicKey deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      return PublicKey.fromHexString(json.getAsString());
    }
  }
}
