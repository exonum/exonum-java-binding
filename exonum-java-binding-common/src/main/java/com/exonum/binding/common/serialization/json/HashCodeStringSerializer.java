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

package com.exonum.binding.common.serialization.json;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.common.hash.HashCode;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;

/**
 * {@link HashCode} string serializer. Used to serialize/deserialize HashCode values from/to hex
 * strings.
 */
public final class HashCodeStringSerializer
    implements JsonSerializer<HashCode>, JsonDeserializer<HashCode> {

  @Override
  public JsonElement serialize(HashCode src, Type typeOfSrc, JsonSerializationContext context) {
    checkNotNull(src, "HashCode value is null");

    return new JsonPrimitive(src.toString());
  }

  @Override
  public HashCode deserialize(JsonElement json, Type typeOfT,
      JsonDeserializationContext context) throws JsonParseException {
    checkNotNull(json, "HashCode json input is null");

    return HashCode.fromString(json.getAsString());
  }
}
