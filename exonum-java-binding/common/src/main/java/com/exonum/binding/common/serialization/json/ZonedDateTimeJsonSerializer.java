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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * {@link ZonedDateTime} string serializer. Used to serialize/deserialize ZonedDateTime values
 * from/to strings in 'yyyy-MM-dd HH:mm:ss Z' format.
 *
 * <p>All method arguments are non-null by default.
 */
final class ZonedDateTimeJsonSerializer implements JsonSerializer<ZonedDateTime>,
    JsonDeserializer<ZonedDateTime> {

  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
      .ofPattern("yyyy-MM-dd HH:mm:ss Z");

  /**
   * Serialize ZonedDateTime to JsonElement.
   *
   * @throws NullPointerException in case of src is null
   */
  @Override
  public JsonElement serialize(ZonedDateTime src, Type typeOfSrc,
      JsonSerializationContext context) {
    checkNotNull(src, "ZonedDateTime value is null");

    return new JsonPrimitive(DATE_TIME_FORMATTER.format(src));
  }

  /**
   * Deserialize ZonedDateTime from JsonElement.
   *
   * @throws NullPointerException in case of json element is null
   */
  @Override
  public ZonedDateTime deserialize(JsonElement json, Type typeOfT,
      JsonDeserializationContext context) throws JsonParseException {
    checkNotNull(json, "ZonedDateTime json input is null");

    return ZonedDateTime.parse(json.getAsString(), DATE_TIME_FORMATTER);
  }
}
