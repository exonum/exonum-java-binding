package com.exonum.binding.qaservice.transactions;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.qaservice.PromoteToCore;
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

/** A converter of transaction parameters of QA service to JSON. */
@PromoteToCore("… in some form or another. You may add a constructor that accepts extra "
    + "type hierarchy adapters to create a proper GSON.")
public final class QaTransactionGson {

  private static final Gson GSON = new GsonBuilder()
      .registerTypeHierarchyAdapter(HashCode.class, new HashCodeSerializer())
      .setLongSerializationPolicy(LongSerializationPolicy.STRING)
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

  /** Returns a configured instance of Gson. */
  public static Gson instance() {
    return GSON;
  }

  /**
   * Converts a transaction of QA service with the given body into its JSON representation.
   *
   * @param txId a transaction id
   * @param txBody a body of the transaction
   * @return a transaction message serialized in JSON
   * @see AnyTransaction
   */
  public String toJson(short txId, Object txBody) {
    AnyTransaction txParams = new AnyTransaction<>(txId, txBody);
    return GSON.toJson(txParams);
  }
}
