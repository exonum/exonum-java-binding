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
import com.exonum.binding.common.transaction.TransactionLocation;
import com.exonum.binding.common.transaction.TransactionResult;
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

  static String createSubmitTxBody(TransactionMessage message) {
    SubmitTxRequest request = new SubmitTxRequest(message);
    return json().toJson(request);
  }

  static HashCode parseSubmitTxResponse(String json) {
    SubmitTxResponse response = json().fromJson(json, SubmitTxResponse.class);
    return response.getHash();
  }

  static TransactionResponse parseGetTxResponse(String json) {
    GetTxResponse response = json().fromJson(json, GetTxResponse.class);
    TransactionResult executionResult = getTransactionResult(response.getStatus());

    return new TransactionResponse(
        response.getType(),
        response.getContent().getMessage(),
        executionResult,
        response.getLocation()
    );
  }

  private static TransactionResult getTransactionResult(
      GetTxResponseExecutionResult executionStatus) {
    if (executionStatus == null) {
      return null;
    } else if (executionStatus.getType() == GetTxResponseExecutionStatus.SUCCESS) {
      return TransactionResult.successful();
    } else if (executionStatus.getType() == GetTxResponseExecutionStatus.ERROR) {
      return TransactionResult.error(executionStatus.getCode(), executionStatus.getDescription());
    } else {
      throw new IllegalArgumentException("Unexpected transaction execution status: "
          + executionStatus.getType());
    }
  }

  /**
   * Json object wrapper for submit transaction request.
   */
  @Value
  private static class SubmitTxRequest {
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

  /**
   * Json object wrapper for get transaction response.
   */
  @Value
  private static class GetTxResponse {
    @NonNull
    TransactionStatus type;
    @NonNull
    GetTxResponseContent content;
    TransactionLocation location;
    @SerializedName("location_proof")
    JsonObject locationProof; //TODO: in scope of LC P3
    GetTxResponseExecutionResult status;
  }

  /**
   * Json object wrapper for get transaction response content i.e.
   * {@code "$.content"}.
   */
  @Value
  private static class GetTxResponseContent {
    JsonObject debug; // contains executable tx in json. currently not supported
    @NonNull
    TransactionMessage message;
  }

  /**
   * Json object wrapper for transaction execution result i.e.
   * {@code "$.status"}.
   */
  @Value
  private static class GetTxResponseExecutionResult {
    GetTxResponseExecutionStatus type;
    int code;
    String description;
  }

  /**
   * Json object wrapper for transaction execution status i.e.
   * {@code "$.status.type"}.
   */
  private enum GetTxResponseExecutionStatus {
    @SerializedName("success")
    SUCCESS,
    @SerializedName("error")
    ERROR
  }

  private ExplorerApiHelper() {
    throw new UnsupportedOperationException("Not instantiable");
  }
}
