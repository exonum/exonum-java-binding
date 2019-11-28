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
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExplorerApiHelperTest {

  @Test
  void parseSubmitTxResponse() {
    String expected = "f128c720e04b8243";
    String json = "{'tx_hash':'" + expected + "'}";

    HashCode actual = ExplorerApiHelper.parseSubmitTxResponse(json);
    assertThat(actual, equalTo(HashCode.fromString(expected)));
  }

  @Test
  void parseGetTxResponseInPool() {
    TransactionMessage expectedMessage = createTransactionMessage();
    String json = "{\n"
        + "    'type': 'in-pool',\n"
        + "    'content': {\n"
        + "        'debug': {\n"
        + "            'to': {\n"
        + "                'data': []\n"
        + "            },\n"
        + "            'amount': 10,\n"
        + "            'seed': 9587307158524814255\n"
        + "        },\n"
        + "        'message': '" + toHex(expectedMessage) + "'\n"
        + "    }\n"
        + "}";
    TransactionResponse transactionResponse = ExplorerApiHelper.parseGetTxResponse(json);

    assertThat(transactionResponse.getStatus(), is(TransactionStatus.IN_POOL));
    assertThat(transactionResponse.getMessage(), is(expectedMessage));
    assertThrows(IllegalStateException.class, transactionResponse::getExecutionResult);
    assertThrows(IllegalStateException.class, transactionResponse::getLocation);
  }

  @Test
  void parseGetTxResponseCommitted() {
    TransactionMessage expectedMessage = createTransactionMessage();
    String json = "{\n"
        + "    'type': 'committed',\n"
        + "    'content': {\n"
        + "        'debug': {\n"
        + "            'to': {\n"
        + "                'data': []\n"
        + "            },\n"
        + "            'amount': 10,\n"
        + "            'seed': 2084648087298472854\n"
        + "        },\n"
        + "        'message': '" + toHex(expectedMessage) + "'\n"
        + "    },\n"
        + "    'location': {\n"
        + "        'block_height': 11,\n"
        + "        'position_in_block': 0\n"
        + "    },\n"
        + "    'location_proof': {\n"
        + "        'val': '2f23541b10b258dfc80693ed1bf6'\n"
        + "    },\n"
        + "    'status': {\n"
        + "        'type': 'success'\n"
        + "    }\n"
        + "}";
    TransactionResponse transactionResponse = ExplorerApiHelper.parseGetTxResponse(json);

    assertThat(transactionResponse.getStatus(), is(TransactionStatus.COMMITTED));
    assertThat(transactionResponse.getMessage(), is(expectedMessage));
    assertThat(transactionResponse.getExecutionResult(), is(ExecutionStatuses.success()));
    assertThat(transactionResponse.getLocation(), is(TransactionLocation.valueOf(11L, 0L)));
  }

  @Test
  void parseGetTxResponseCommittedWithError() {
    TransactionMessage expectedMessage = createTransactionMessage();
    int errorCode = 2;
    String errorDescription = "Receiver doesn't exist";
    String json = "{\n"
        + "    'type': 'committed',\n"
        + "    'content': {\n"
        + "        'debug': {\n"
        + "            'amount': 1,\n"
        + "            'seed': 5019726028924803177\n"
        + "        },\n"
        + "        'message': '" + toHex(expectedMessage) + "'\n"
        + "    },\n"
        + "    'location': {\n"
        + "        'block_height': 1,\n"
        + "        'position_in_block': 0\n"
        + "    },\n"
        + "    'location_proof': {\n"
        + "        'val': 'e8a00b3747d396be45dbea3bc31cdb072'\n"
        + "    },\n"
        + "    'status': {\n"
        + "        'type': 'error',\n"
        + "        'code': " + errorCode + ",\n"
        + "        'description': \"" + errorDescription + "\""
        + "    }\n"
        + "}";
    TransactionResponse transactionResponse = ExplorerApiHelper.parseGetTxResponse(json);

    assertThat(transactionResponse.getStatus(), is(TransactionStatus.COMMITTED));
    assertThat(transactionResponse.getMessage(), is(expectedMessage));
    assertThat(transactionResponse.getExecutionResult(),
        is(ExecutionStatuses.serviceError(errorCode, errorDescription)));
    assertThat(transactionResponse.getLocation(), is(TransactionLocation.valueOf(1L, 0L)));
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
    List<ServiceInfo> expected = List.of(serviceInfo1, serviceInfo2);
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
}
