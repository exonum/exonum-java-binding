package com.exonum.binding.cryptocurrency.transactions;

import com.exonum.binding.hash.HashCode;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Map;

/** A converter of transaction parameters of cryptocurrency service to JSON. */
public class CryptocurrencyTransactionGson {

  private static final Gson GSON = new GsonBuilder()
      .registerTypeHierarchyAdapter(HashCode.class, new HashCodeSerializer())
      .create();

  private static class HashCodeSerializer implements JsonSerializer<HashCode>,
      JsonDeserializer<HashCode> {

    @Override
    public JsonElement serialize(HashCode src, Type typeOfSrc, JsonSerializationContext context) {
      return new JsonPrimitive(src.toString());
    }

    @Override
    public HashCode deserialize(JsonElement json, Type typeOfT,
                                JsonDeserializationContext context) throws JsonParseException {
      return HashCode.fromString(json.getAsString());
    }
  }

  public String toJson(short txId, Map<String, ?> txBody) {
    AnyTransaction txParams = new AnyTransaction(txId, txBody);
    return GSON.toJson(txParams);
  }

  public static Gson instance() {
    return GSON;
  }
}
