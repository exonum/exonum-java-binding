package com.exonum.binding.qaservice.transactions;

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
import java.lang.reflect.Type;
import java.util.Map;

/** A converter of transaction parameters of QA service to JSON. */
class QaTransactionJsonWriter {

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
}
