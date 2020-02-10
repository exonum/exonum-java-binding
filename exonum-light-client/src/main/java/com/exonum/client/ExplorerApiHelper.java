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

import com.exonum.binding.common.blockchain.ExecutionStatuses;
import com.exonum.binding.common.blockchain.TransactionLocation;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.client.response.Block;
import com.exonum.client.response.BlockResponse;
import com.exonum.client.response.BlocksResponse;
import com.exonum.client.response.ServiceInstanceInfo;
import com.exonum.client.response.ServiceInstanceState;
import com.exonum.client.response.ServicesResponse;
import com.exonum.client.response.TransactionResponse;
import com.exonum.client.response.TransactionStatus;
import com.exonum.core.messages.Runtime.CallSite;
import com.exonum.core.messages.Runtime.CallSite.Type;
import com.exonum.core.messages.Runtime.ErrorKind;
import com.exonum.core.messages.Runtime.ExecutionError;
import com.exonum.core.messages.Runtime.ExecutionError.Builder;
import com.exonum.core.messages.Runtime.ExecutionStatus;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.google.protobuf.Empty;
import java.time.ZonedDateTime;
import java.util.List;
import javax.annotation.Nullable;
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
    ExecutionStatus executionResult = getExecutionStatus(response.getStatus());

    return new TransactionResponse(
        response.getType(),
        response.getMessage(),
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

  static List<ServiceInstanceInfo> parseServicesResponse(String json) {
    ServicesResponse servicesResponse = JSON.fromJson(json, ServicesResponse.class);
    return servicesResponse.getServices().stream()
        .map(ServiceInstanceState::getSpec)
        .collect(toList());
  }

  private static @Nullable ExecutionStatus getExecutionStatus(
      GetTxResponseExecutionStatus executionStatus) {
    if (executionStatus == null) {
      return null;
    } else {
      return executionStatus.toProto();
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
    TransactionMessage message;
    TransactionLocation location;
    JsonObject locationProof; // TODO: in scope of LC P3
    GetTxResponseExecutionStatus status;
  }

  /**
   * Json object wrapper for transaction execution result, i.e.,
   * {@code "$.status"}.
   *
   * <p>See serde::ExecutionStatus: https://github.com/exonum/exonum/blob/v1.0.0-rc.1/exonum/src/runtime/error/execution_status.rs#L102
   */
  @Value
  private static class GetTxResponseExecutionStatus {
    GetTxResponseExecutionType type;
    @Nullable String description;
    @Nullable Integer code;
    @Nullable Integer runtimeId;
    @Nullable GetTxResponseCallSite callSite;

    ExecutionStatus toProto() {
      if (type == GetTxResponseExecutionType.SUCCESS) {
        return ExecutionStatuses.SUCCESS;
      }

      ExecutionError error = asError();
      return ExecutionStatus.newBuilder()
          .setError(error)
          .build();
    }

    ExecutionError asError() {
      Builder errorBuilder = ExecutionError.newBuilder()
          .setKind(type.toProto());

      if (description != null) {
        errorBuilder.setDescription(description);
      }
      if (code != null) {
        errorBuilder.setCode(code);
      }
      if (runtimeId != null) {
        errorBuilder.setRuntimeId(runtimeId);
      } else {
        errorBuilder.setNoRuntimeId(Empty.getDefaultInstance());
      }
      if (callSite != null) {
        errorBuilder.setCallSite(callSite.toProto());
      } else {
        errorBuilder.setNoCallSite(Empty.getDefaultInstance());
      }

      return errorBuilder.build();
    }
  }

  /**
   * Json object wrapper for transaction execution status, i.e.,
   * {@code "$.status.type"}.
   *
   * <p>See serde::ExecutionType: https://github.com/exonum/exonum/blob/v1.0.0-rc.1/exonum/src/runtime/error/execution_status.rs#L90</p>
   */
  private enum GetTxResponseExecutionType {
    @SerializedName("success")
    SUCCESS,
    @SerializedName("unexpected_error")
    UNEXPECTED_ERROR,
    @SerializedName("common_error")
    COMMON_ERROR,
    @SerializedName("core_error")
    CORE_ERROR,
    @SerializedName("runtime_error")
    RUNTIME_ERROR,
    @SerializedName("service_error")
    SERVICE_ERROR;

    ErrorKind toProto() {
      switch (this) {
        case UNEXPECTED_ERROR:
          return ErrorKind.UNEXPECTED;
        case COMMON_ERROR:
          return ErrorKind.COMMON;
        case CORE_ERROR:
          return ErrorKind.CORE;
        case RUNTIME_ERROR:
          return ErrorKind.RUNTIME;
        case SERVICE_ERROR:
          return ErrorKind.SERVICE;
        default:
          throw new IllegalStateException("Unsupported type: " + this);
      }
    }
  }

  @Value
  private static class GetTxResponseCallSite {
    int instanceId;
    GetTxResponseCallType callType;
    @SerializedName("interface") @Nullable String interfaceId;
    @Nullable Integer methodId;

    CallSite toProto() {
      return CallSite.newBuilder()
          .setCallType(callType.toProto())
          .setInstanceId(instanceId)
          .setMethodId((methodId == null) ? 0 : methodId)
          .setInterface(Strings.nullToEmpty(interfaceId))
          .build();
    }
  }

  private enum GetTxResponseCallType {
    @SerializedName("constructor")
    CONSTRUCTOR,
    @SerializedName("resume")
    RESUME,
    @SerializedName("method")
    METHOD,
    @SerializedName("before_transactions")
    BEFORE_TRANSACTIONS,
    @SerializedName("after_transactions")
    AFTER_TRANSACTIONS;

    CallSite.Type toProto() {
      switch (this) {
        case CONSTRUCTOR:
          return Type.CONSTRUCTOR;
        case RESUME:
          return Type.RESUME;
        case METHOD:
          return Type.METHOD;
        case BEFORE_TRANSACTIONS:
          return Type.BEFORE_TRANSACTIONS;
        case AFTER_TRANSACTIONS:
          return Type.AFTER_TRANSACTIONS;
        default:
          throw new IllegalStateException("Unsupported type: " + this);
      }
    }
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
