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

package com.exonum.client;

import static com.exonum.binding.common.serialization.json.JsonSerializer.json;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.client.response.TransactionLocation;
import com.exonum.client.response.TransactionResponse;
import com.exonum.client.response.TransactionStatus;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.NonNull;
import lombok.Value;

/**
 * Utility class for Exonum Explorer API.
 */
final class ExplorerApiHelper {

  static HashCode parseSubmitTxResponse(String json) {
    SubmitTxResponse response = json().fromJson(json, SubmitTxResponse.class);
    return response.getHash();
  }

  static TransactionResponse parseTxResponse(String json) {
    GetTxResponse response = json().fromJson(json, GetTxResponse.class);

    return new TransactionResponse(
        response.getType(),
        response.getContent().getMessage(),
        "ok", //TODO
        response.getLocation()
    );
  }

  /**
   * Json object wrapper for submit transaction request.
   */
  @Value
  static class SubmitTxRequest {
    @SerializedName("tx_body")
    TransactionMessage body;
  }

  /**
   * Json object wrapper for submit transaction response.
   */
  @Value
  private static class SubmitTxResponse {
    @SerializedName("tx_hash")
    HashCode hash;
  }

  @Value
  private static class GetTxResponse {
    @NonNull
    TransactionStatus type;
    @NonNull
    GetTxResponseContent content;
    TransactionLocation location;
    @SerializedName("location_proof")
    JsonObject locationProof;
    JsonObject status;
  }

  @Value
  private static class GetTxResponseContent {
    JsonObject debug; // contains executable tx in json. currently not supported
    @NonNull
    TransactionMessage message;
  }

  private ExplorerApiHelper() {
    throw new UnsupportedOperationException("Not instantiable");
  }
}
