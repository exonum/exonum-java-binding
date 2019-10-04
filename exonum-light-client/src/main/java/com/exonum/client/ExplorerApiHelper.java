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

import static com.exonum.client.ExonumApi.JSON;
import static java.util.stream.Collectors.toList;

import com.exonum.binding.common.blockchain.TransactionLocation;
import com.exonum.binding.common.blockchain.TransactionResult;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.client.response.Block;
import com.exonum.client.response.BlockResponse;
import com.exonum.client.response.BlocksResponse;
import com.exonum.client.response.TransactionResponse;
import com.exonum.client.response.TransactionStatus;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

/**
 * Utility class for Exonum Explorer API.
 */
final class ExplorerApiHelper {

  static String createSubmitTxBody(TransactionMessage message) {
    SubmitTxRequest request = new SubmitTxRequest(message);
    return JSON.toJson(request);
  }

  static HashCode parseSubmitTxResponse(String json) {
    SubmitTxResponse response = JSON.fromJson(json, SubmitTxResponse.class);
    return response.getTxHash();
  }

  static TransactionResponse parseGetTxResponse(String json) {
    GetTxResponse response = JSON.fromJson(json, GetTxResponse.class);
    TransactionResult executionResult = getTransactionResult(response.getStatus());

    return new TransactionResponse(
        response.getType(),
        response.getContent().getMessage(),
        executionResult,
        response.getLocation()
    );
  }

  static BlockResponse parseGetBlockResponse(String json) {
    GetBlockResponse response = JSON.fromJson(json, GetBlockResponse.class);

    List<HashCode> txs = response.getTxs().stream()
        .map(IndexedTxHash::getTxHash)
        .collect(toList());
    return new BlockResponse(response.getAsBlock(), txs);
  }

  static BlocksResponse parseGetBlocksResponse(String json) {
    GetBlocksResponse response = JSON.fromJson(json, GetBlocksResponse.class);

    return new BlocksResponse(
        response.getBlocks(),
        response.getRange().getStart(),
        response.getRange().getEnd()
    );
  }

  private static TransactionResult getTransactionResult(
      GetTxResponseExecutionResult executionStatus) {
    if (executionStatus == null) {
      return null;
    }
    switch (executionStatus.getType()) {
      case SUCCESS:
        return TransactionResult.successful();
      case ERROR:
        return TransactionResult.error(executionStatus.getCode(),
            executionStatus.getDescription());
      case PANIC:
        return TransactionResult.unexpectedError(executionStatus.getDescription());
      default:
        throw new IllegalStateException("Unexpected transaction execution status: "
            + executionStatus.getType());
    }
  }

  /**
   * Json object wrapper for submit transaction request.
   */
  @Value
  static class SubmitTxRequest {
    TransactionMessage txBody;
  }

  /**
   * Json object wrapper for submit transaction response.
   */
  @Value
  private static class SubmitTxResponse {
    HashCode txHash;
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
    ERROR,
    @SerializedName("panic")
    PANIC
  }

  @Value
  @AllArgsConstructor
  @VisibleForTesting
  static class GetBlockResponse {
    int proposerId;
    long height;
    int txCount;
    HashCode prevHash;
    HashCode txHash;
    HashCode stateHash;
    JsonElement precommits; //TODO: in scope of LC P3
    List<IndexedTxHash> txs;
    ZonedDateTime time;

    GetBlockResponse(Block block, List<IndexedTxHash> txs) {
      this(block.getProposerId(), block.getHeight(),
          block.getNumTransactions(), block.getPreviousBlockHash(), block.getTxRootHash(),
          block.getStateHash(), new JsonArray(), ImmutableList.copyOf(txs),
          block.getCommitTime().orElse(null));
    }

    Block getAsBlock() {
      return Block.builder()
          .proposerId(proposerId)
          .height(height)
          .numTransactions(txCount)
          .previousBlockHash(prevHash)
          .txRootHash(txHash)
          .stateHash(stateHash)
          .commitTime(time)
          .build();
    }
  }

  @Value
  @VisibleForTesting
  static class IndexedTxHash {
    int serviceId;
    HashCode txHash;
  }

  @Value
  private static class GetBlocksResponse {
    List<Block> blocks;
    GetBlocksResponseRange range;
  }

  @Value
  private static class GetBlocksResponseRange {
    long start;
    long end;
  }

  private ExplorerApiHelper() {
    throw new UnsupportedOperationException("Not instantiable");
  }
}
