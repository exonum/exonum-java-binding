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
import static com.google.common.base.Preconditions.checkState;

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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
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
    ZonedDateTime time = response.getTime();
    Block block = toTimeBlock(response.getBlock(), time);

    return new BlockResponse(block, response.getTxs());
  }

  static BlocksResponse parseGetBlocksResponse(String json) {
    GetBlocksResponse response = JSON.fromJson(json, GetBlocksResponse.class);
    List<GetBlockResponseBlock> blocks = response.getBlocks();
    int blocksSize = blocks.size();
    List<ZonedDateTime> times = response.getTimes();
    if (times != null) {
      int timesSize = times.size();
      checkState(blocksSize == timesSize,
          "Blocks size %s doesn't equal to commit times size %s", blocksSize, timesSize);
    }

    List<Block> timeBlocks = new ArrayList<>(blocksSize);
    for (int i = 0; i < blocksSize; i++) {
      if (times == null) {
        timeBlocks.add(toTimeBlock(blocks.get(i)));
      } else {
        timeBlocks.add(toTimeBlock(blocks.get(i), times.get(i)));
      }
    }

    return new BlocksResponse(
        timeBlocks,
        response.getRange().getStart(),
        response.getRange().getEnd()
    );
  }

  private static Block toTimeBlock(GetBlockResponseBlock block) {
    return toTimeBlock(block, null);
  }

  private static Block toTimeBlock(GetBlockResponseBlock block, @Nullable ZonedDateTime time) {
    return Block.builder()
        .proposerId(block.getProposerId())
        .numTransactions(block.getTxCount())
        .height(block.getHeight())
        .previousBlockHash(block.getPrevHash())
        .stateHash(block.getStateHash())
        .txRootHash(block.getTxHash())
        .commitTime(time)
        .build();
  }

  private static TransactionResult getTransactionResult(
      GetTxResponseExecutionResult executionStatus) {
    if (executionStatus == null) {
      return null;
    } else if (executionStatus.getType() == GetTxResponseExecutionStatus.SUCCESS) {
      return TransactionResult.successful();
    } else if (executionStatus.getType() == GetTxResponseExecutionStatus.ERROR) {
      return TransactionResult.error(executionStatus.getCode(), executionStatus.getDescription());
    } else if (executionStatus.getType() == GetTxResponseExecutionStatus.PANIC) {
      return TransactionResult.unexpectedError(executionStatus.getDescription());
    } else {
      throw new IllegalArgumentException("Unexpected transaction execution status: "
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
  private static class GetBlockResponse {
    GetBlockResponseBlock block;
    JsonElement precommits; //TODO: in scope of LC P3
    List<HashCode> txs;
    ZonedDateTime time;
  }

  @Value
  @VisibleForTesting
  static class GetBlockResponseBlock {
    int proposerId;
    long height;
    int txCount;
    HashCode prevHash;
    HashCode txHash;
    HashCode stateHash;
  }

  @Value
  private static class GetBlocksResponse {
    List<GetBlockResponseBlock> blocks;
    GetBlocksResponseRange range;
    @Nullable
    List<ZonedDateTime> times;
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
