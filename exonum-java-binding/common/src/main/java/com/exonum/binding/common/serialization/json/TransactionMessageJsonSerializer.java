/*
 * Copyright 2019 The Exonum Team
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

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.common.message.TransactionMessage;
import com.google.common.io.BaseEncoding;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;

final class TransactionMessageJsonSerializer implements JsonSerializer<TransactionMessage>,
    JsonDeserializer<TransactionMessage> {
  private static final BaseEncoding HEX_ENCODER = BaseEncoding.base16().lowerCase();

  @Override
  public JsonElement serialize(TransactionMessage src, Type typeOfSrc,
      JsonSerializationContext context) {
    checkNotNull(src, "Transaction message value is null");
    byte[] bytes = src.toBytes();
    String hex = HEX_ENCODER.encode(bytes);

    return new JsonPrimitive(hex);
  }

  @Override
  public TransactionMessage deserialize(JsonElement json, Type typeOfT,
      JsonDeserializationContext context) throws JsonParseException {
    checkNotNull(json, "Transaction message json input is null");
    String hex = json.getAsString();
    byte[] bytes = HEX_ENCODER.decode(hex);

    return TransactionMessage.fromBytes(bytes);
  }

}
