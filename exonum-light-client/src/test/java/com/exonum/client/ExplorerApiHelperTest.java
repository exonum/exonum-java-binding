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

import static com.exonum.client.Blocks.BLOCK_1;
import static com.exonum.client.Blocks.BLOCK_1_JSON;
import static com.exonum.client.Blocks.BLOCK_2;
import static com.exonum.client.Blocks.BLOCK_2_JSON;
import static com.exonum.client.Blocks.BLOCK_3;
import static com.exonum.client.Blocks.BLOCK_3_JSON;
import static com.exonum.client.TestUtils.createTransactionMessage;
import static com.exonum.client.TestUtils.toHex;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.exonum.binding.common.blockchain.ExecutionStatuses;
import com.exonum.binding.common.blockchain.TransactionLocation;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.client.response.Block;
import com.exonum.client.response.BlockResponse;
import com.exonum.client.response.BlocksResponse;
import com.exonum.client.response.ServiceInstanceInfo;
import com.exonum.client.response.TransactionResponse;
import com.exonum.client.response.TransactionStatus;
import com.exonum.messages.core.runtime.Errors.CallSite;
import com.exonum.messages.core.runtime.Errors.CallSite.Type;
import com.exonum.messages.core.runtime.Errors.ErrorKind;
import com.exonum.messages.core.runtime.Errors.ExecutionError;
import com.exonum.messages.core.runtime.Errors.ExecutionStatus;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Empty;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import lombok.Value;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ExplorerApiHelperTest {

  private static final TransactionMessage TRANSACTION_MESSAGE = createTransactionMessage();
  private static final long BLOCK_HEIGHT = 1L;
  private static final int INDEX_IN_BLOCK = 0;

  private static final String TEMPLATE_TRANSACTION_MESSAGE_JSON = "{\n"
      + "    'type': 'committed',\n"
      + "    'message': '" + toHex(TRANSACTION_MESSAGE) + "',\n"
      + "    'location': {\n"
      + "        'block_height': " + BLOCK_HEIGHT + ",\n"
      + "        'position_in_block': " + INDEX_IN_BLOCK + "\n"
      + "    },\n"
      + "    'location_proof': {\n"
      + "        'entries': [\n"
      + "            [\n"
      + "                0,\n"
      + "                'd27f4ae6692fc00caf4e51ca7c072bab35487bb0d56272e08b6069ebadb52100'\n"
      + "            ]\n"
      + "        ],\n"
      + "        'length': 1,\n"
      + "        'proof': []\n"
      + "    },\n"
      + "    %s,\n"
      + "    'time': '2019-12-02T21:51:36.439431Z'"
      + "}";

  @Test
  void parseSubmitTxResponse() {
    String expected = "f128c720e04b8243";
    String json = "{'tx_hash':'" + expected + "'}";

    HashCode actual = ExplorerApiHelper.parseSubmitTxResponse(json);
    assertThat(actual, equalTo(HashCode.fromString(expected)));
  }

  @Test
  void parseGetTxResponseInPool() {
    String json = "{\n"
        + "    'type': 'in-pool',\n"
        + "    'message': '" + toHex(TRANSACTION_MESSAGE) + "'\n"
        + "}";
    TransactionResponse transactionResponse = ExplorerApiHelper.parseGetTxResponse(json);

    assertThat(transactionResponse.getStatus(), is(TransactionStatus.IN_POOL));
    assertThat(transactionResponse.getMessage(), is(TRANSACTION_MESSAGE));
    assertThrows(IllegalStateException.class, transactionResponse::getExecutionResult);
    assertThrows(IllegalStateException.class, transactionResponse::getLocation);
  }

  @ParameterizedTest
  @MethodSource("txResponseTestData")
  void parseGetTxResponseCommitted(ExecutionStatus executionStatus, String statusJson) {
    String json = String.format(TEMPLATE_TRANSACTION_MESSAGE_JSON, statusJson);
    TransactionResponse transactionResponse = ExplorerApiHelper.parseGetTxResponse(json);

    assertThat(transactionResponse.getStatus(), is(TransactionStatus.COMMITTED));
    assertThat(transactionResponse.getMessage(), is(TRANSACTION_MESSAGE));
    assertThat(transactionResponse.getExecutionResult(), is(executionStatus));
    assertThat(transactionResponse.getLocation(),
        is(TransactionLocation.valueOf(BLOCK_HEIGHT, INDEX_IN_BLOCK)));
  }

  @Test
  void parseGetBlockResponse() {
    String prevHash = "81abde95";
    String txHash = "c6c0aa07";
    String stateHash = "2eab5971";
    String tx1 = "336a4acb";
    String tx2 = "12345678";
    String commitTime = "2019-10-01T17:07:45.663021Z";
    String json = "{\n"
        + "    \"proposer_id\": 0,\n"
        + "    \"height\": 2,\n"
        + "    \"tx_count\": 1,\n"
        + "    \"prev_hash\": \"" + prevHash + "\",\n"
        + "    \"tx_hash\": \"" + txHash + "\",\n"
        + "    \"state_hash\": \"" + stateHash + "\",\n"
        + "    \"precommits\": [\n"
        + "        \"bc13da11\"\n"
        + "    ],\n"
        + "    \"txs\": [\n"
        + "        {\"service_id\": 0, \"tx_hash\": \"" + tx1 + "\"},\n"
        + "        {\"service_id\": 128, \"tx_hash\": \"" + tx2 + "\"}\n"
        + "    ],\n"
        + "    \"time\": \"" + commitTime + "\"\n"
        + "}";

    BlockResponse response = ExplorerApiHelper.parseGetBlockResponse(json);

    Block expectedBlock = Block.builder()
        .proposerId(0)
        .height(2)
        .numTransactions(1)
        .previousBlockHash(HashCode.fromString(prevHash))
        .txRootHash(HashCode.fromString(txHash))
        .stateHash(HashCode.fromString(stateHash))
        .commitTime(ZonedDateTime.parse(commitTime))
        .build();
    assertThat(response.getBlock(), is(expectedBlock));
    assertThat(response.getTransactionHashes(), contains(HashCode.fromString(tx1),
        HashCode.fromString(tx2)));
  }

  @Test
  void parseGetBlocksResponse() {
    String json = "{\n"
        + "    'range': {\n"
        + "        'start': 6,\n"
        + "        'end': 288\n"
        + "    },\n"
        + "    'blocks': [ " + BLOCK_1_JSON + "," + BLOCK_2_JSON + "," + BLOCK_3_JSON + "]\n"
        + "}\n";

    BlocksResponse response = ExplorerApiHelper.parseGetBlocksResponse(json);

    assertThat(response.getBlocks(), contains(BLOCK_1, BLOCK_2, BLOCK_3));
    assertThat(response.getBlocksRangeStart(), is(6L));
    assertThat(response.getBlocksRangeEnd(), is(288L));
  }

  @Test
  void parseServicesResponse() {
    String serviceName1 = "service-name-1";
    String serviceName2 = "service-name-2";
    int serviceId1 = 1;
    int serviceId2 = 2;
    ServiceInstanceInfo serviceInstanceInfo1 = new ServiceInstanceInfo(serviceName1, serviceId1);
    ServiceInstanceInfo serviceInstanceInfo2 = new ServiceInstanceInfo(serviceName2, serviceId2);
    List<ServiceInstanceInfo> expected = Arrays.asList(serviceInstanceInfo1, serviceInstanceInfo2);
    String json = "{\n"
        + "    \"services\": [{\n"
        + "        \"spec\": {\n"
        + "            \"name\": \"" + serviceName1 + "\",\n"
        + "            \"id\": " + serviceId1 + "\n"
        + "            },\n"
        + "            \"status\": \"Active\"\n"
        + "        },\n"
        + "        {\n"
        + "        \"spec\": {\n"
        + "            \"name\": \"" + serviceName2 + "\",\n"
        + "            \"id\": " + serviceId2 + "\n"
        + "            },\n"
        + "            \"status\": \"Active\"\n"
        + "        }\n"
        + "    ]\n"
        + "}";

    List<ServiceInstanceInfo> actual = ExplorerApiHelper.parseServicesResponse(json);
    assertThat(actual, contains(expected.toArray()));
  }

  private static Collection<Arguments> txResponseTestData() {
    List<Arguments> arguments = new ArrayList<>();

    // Add success status
    String successStatus = "'status': {\n"
        + "    'type': 'success'\n"
        + "}\n";
    arguments.add(arguments(ExecutionStatuses.SUCCESS, successStatus));

    // Add error statuses of various types from various call site types:
    int errorCode = 1;
    String errorDescription = "Some error";
    int runtimeId = 2;
    int instanceId = 3;
    int methodId = 4;
    String interfaceId = "exonum.Configure";

    ExecutionError errorTemplate = ExecutionError.newBuilder()
        .setCode(errorCode)
        .setDescription(errorDescription)
        .setRuntimeId(runtimeId)
        .buildPartial();
    CallSite callSiteTemplate = CallSite.newBuilder()
        .setInstanceId(instanceId)
        .setMethodId(methodId)
        .setInterface(interfaceId)
        .buildPartial();

    String errorStatusTemplate = "'status': {\n"
        + "    'type': '%s',\n"
        + "    'code': " + errorCode + ",\n"
        + "    'description': \"" + errorDescription + "\","
        + "    \"runtime_id\": " + runtimeId + ",\n"
        + "    \"call_site\": {\n"
        + "            \"instance_id\": " + instanceId + ",\n"
        + "            \"call_type\": \"%s\",\n"
        + "            \"method_id\": " + methodId + ",\n"
        + "            \"interface\": \"" + interfaceId + "\"\n"
        + "    }\n"
        + "}\n";

    @Value
    class StatusParameters {
      ErrorKind errorKind;
      String errorKindStr;
      CallSite.Type callSiteType;
      String callSiteTypeStr;
    }

    List<StatusParameters> combinations = ImmutableList.of(
        new StatusParameters(ErrorKind.UNEXPECTED, "unexpected_error",
            Type.CONSTRUCTOR, "constructor"),
        new StatusParameters(ErrorKind.CORE, "core_error", Type.METHOD, "method"),
        new StatusParameters(ErrorKind.RUNTIME, "runtime_error",
            Type.BEFORE_TRANSACTIONS, "before_transactions"),
        new StatusParameters(ErrorKind.SERVICE, "service_error",
            Type.AFTER_TRANSACTIONS, "after_transactions"),
        new StatusParameters(ErrorKind.COMMON, "common_error", Type.RESUME, "resume")
    );

    for (StatusParameters params: combinations) {
      arguments.add(
          arguments(
              ExecutionStatus.newBuilder()
                  .setError(
                      ExecutionError.newBuilder(errorTemplate)
                          .setKind(params.errorKind)
                          .setCallSite(
                              CallSite.newBuilder(callSiteTemplate)
                                  .setCallType(params.callSiteType)))
                  .build(),
              String.format(errorStatusTemplate, params.errorKindStr, params.callSiteTypeStr)));
    }

    // Add an error with no optional properties: no code, no runtime id, no call site.
    arguments.add(arguments(
        ExecutionStatus.newBuilder()
          .setError(
              ExecutionError.newBuilder()
                  .setKind(ErrorKind.UNEXPECTED)
                  .setDescription(errorDescription)
                  .setNoRuntimeId(Empty.getDefaultInstance())
                  .setNoCallSite(Empty.getDefaultInstance())
                  .build())
          .build(),
        "'status': {\n"
            + "    'type': 'unexpected_error',\n"
            + "    'description': \"" + errorDescription + "\""
            + "}\n"
    ));

    return arguments;
  }
}
