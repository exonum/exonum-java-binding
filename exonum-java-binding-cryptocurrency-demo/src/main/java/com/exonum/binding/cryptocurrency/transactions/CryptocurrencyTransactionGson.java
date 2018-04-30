package com.exonum.binding.cryptocurrency.transactions;

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
}
