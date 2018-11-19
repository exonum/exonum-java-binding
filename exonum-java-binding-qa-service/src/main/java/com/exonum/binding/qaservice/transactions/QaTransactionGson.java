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

package com.exonum.binding.qaservice.transactions;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.json.HashCodeStringSerializer;
import com.exonum.binding.qaservice.PromoteToCore;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;

/**
 * A converter of transaction parameters of QA service to JSON.
 */
@PromoteToCore("… in some form or another. You may add a constructor that accepts extra "
    + "type hierarchy adapters to create a proper GSON.")
public final class QaTransactionGson {

  private static final Gson GSON = new GsonBuilder()
      .registerTypeHierarchyAdapter(HashCode.class, new HashCodeStringSerializer())
      .setLongSerializationPolicy(LongSerializationPolicy.STRING)
      .create();

  /**
   * Returns a configured instance of Gson.
   */
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
