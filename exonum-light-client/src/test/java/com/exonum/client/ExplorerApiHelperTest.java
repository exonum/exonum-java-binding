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
import com.exonum.client.response.ServiceInfo;
import com.exonum.client.response.TransactionResponse;
import com.exonum.client.response.TransactionStatus;
import com.exonum.core.messages.Runtime.ErrorKind;
import com.exonum.core.messages.Runtime.ExecutionStatus;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ExplorerApiHelperTest {

  private static TransactionMessage TRANSACTION_MESSAGE = createTransactionMessage();
  private static long BLOCK_HEIGHT = 1L;
  private static long INDEX_IN_BLOCK = 0L;

  private static String TEMPLATE_TRANSACTION_MESSAGE_JSON = "{\n"
      + "    'type': 'committed',\n"
      + "    'content': '" + toHex(TRANSACTION_MESSAGE) + "',\n"
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
        + "    'content': '" + toHex(TRANSACTION_MESSAGE) + "'\n"
        + "}";
    TransactionResponse transactionResponse = ExplorerApiHelper.parseGetTxResponse(json);

    assertThat(transactionResponse.getStatus(), is(TransactionStatus.IN_POOL));
    assertThat(transactionResponse.getMessage(), is(TRANSACTION_MESSAGE));
    assertThrows(IllegalStateException.class, transactionResponse::getExecutionResult);
    assertThrows(IllegalStateException.class, transactionResponse::getLocation);
  }

  @ParameterizedTest
  @MethodSource("testData")
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
    ServiceInfo serviceInfo1 = new ServiceInfo(serviceName1, serviceId1);
    ServiceInfo serviceInfo2 = new ServiceInfo(serviceName2, serviceId2);
    List<ServiceInfo> expected = Arrays.asList(serviceInfo1, serviceInfo2);
    String json = "{\n"
        + "    \"services\": [\n"
        + "      {\n"
        + "          \"name\": \"" + serviceName1 + "\",\n"
        + "          \"id\": " + serviceId1 + "\n"
        + "      },\n"
        + "      {\n"
        + "          \"name\": \"" + serviceName2 + "\",\n"
        + "          \"id\": " + serviceId2 + "\n"
        + "      }\n"
        + "    ]\n"
        + "}";

    List<ServiceInfo> actual = ExplorerApiHelper.parseServicesResponse(json);
    assertThat(actual, contains(expected.toArray()));
  }

  private static Stream<Arguments> testData() {
    String successStatus = "'status': {\n"
        + "    'type': 'success'\n"
        + "}\n";

    int errorCode = 1;
    String errorDescription = "Some error";
    String errorStatusTemplate = "'status': {\n"
        + "    'type': '%s',\n"
        + "    'code': " + errorCode + ",\n"
        + "    'description': \"" + errorDescription + "\""
        + "}\n";

    String serviceErrorStatus = String.format(errorStatusTemplate, "service_error");
    String dispatcherErrorStatus = String.format(errorStatusTemplate, "dispatcher_error");
    String runtimeErrorStatus = String.format(errorStatusTemplate, "runtime_error");

    String panicStatus = "'status': {\n"
        + "    'type': 'panic',\n"
        + "    'description': \"" + errorDescription + "\""
        + "}\n";

    return Stream.of(
        arguments(ExecutionStatuses.success(), successStatus),
        arguments(ExecutionStatuses.serviceError(errorCode, errorDescription), serviceErrorStatus),
        arguments(ExplorerApiHelper.buildExecutionStatus(ErrorKind.DISPATCHER, errorCode,
            errorDescription),
            dispatcherErrorStatus),
        arguments(ExplorerApiHelper.buildExecutionStatus(ErrorKind.RUNTIME, errorCode,
            errorDescription),
            runtimeErrorStatus),
        arguments(ExplorerApiHelper.buildPanicExecutionStatus(errorDescription), panicStatus)
    );
  }
}
