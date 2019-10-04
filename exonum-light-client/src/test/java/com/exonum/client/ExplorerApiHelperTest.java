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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.blockchain.TransactionLocation;
import com.exonum.binding.common.blockchain.TransactionResult;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.client.response.Block;
import com.exonum.client.response.BlockResponse;
import com.exonum.client.response.BlocksResponse;
import com.exonum.client.response.TransactionResponse;
import com.exonum.client.response.TransactionStatus;
import java.time.ZonedDateTime;
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
    assertThat(transactionResponse.getExecutionResult(), is(TransactionResult.successful()));
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
        is(TransactionResult.error(errorCode, errorDescription)));
    assertThat(transactionResponse.getLocation(), is(TransactionLocation.valueOf(1L, 0L)));
  }

  @Test
  void parseGetTxResponseCommittedWithPanic() {
    TransactionMessage expectedMessage = createTransactionMessage();
    String errorDescription = "panic happens";
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
        + "        'type': 'panic',\n"
        + "        'description': '" + errorDescription + "'"
        + "    }\n"
        + "}";
    TransactionResponse transactionResponse = ExplorerApiHelper.parseGetTxResponse(json);

    assertThat(transactionResponse.getStatus(), is(TransactionStatus.COMMITTED));
    assertThat(transactionResponse.getMessage(), is(expectedMessage));
    assertThat(transactionResponse.getExecutionResult(),
        is(TransactionResult.unexpectedError(errorDescription)));
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
  void name() {
    String json = "{\n"
        + "    \"range\": {\n"
        + "        \"start\": 89,\n"
        + "        \"end\": 91\n"
        + "    },\n"
        + "    \"blocks\": [\n"
        + "        {\n"
        + "            \"proposer_id\": 0,\n"
        + "            \"height\": 90,\n"
        + "            \"tx_count\": 0,\n"
        + "            \"prev_hash\": \"fd54ca9fe9f99fa40d6ad2cd34007c391bd27db6423f278b0fbc65d16e08e12b\",\n"
        + "            \"tx_hash\": \"c6c0aa07f27493d2f2e5cff56c890a353a20086d6c25ec825128e12ae752b2d9\",\n"
        + "            \"state_hash\": \"e27636da3f03bc8193d81047415d7ac92bbfc2579675ba3e84feb1d3040366be\",\n"
        + "            \"time\": \"2019-10-03T11:27:54.473943Z\"\n"
        + "        },\n"
        + "        {\n"
        + "            \"proposer_id\": 0,\n"
        + "            \"height\": 89,\n"
        + "            \"tx_count\": 0,\n"
        + "            \"prev_hash\": \"8702e797a951fe387ae6762ed35d4d3b8af2d1a1628a157a621f713a4b516990\",\n"
        + "            \"tx_hash\": \"c6c0aa07f27493d2f2e5cff56c890a353a20086d6c25ec825128e12ae752b2d9\",\n"
        + "            \"state_hash\": \"e27636da3f03bc8193d81047415d7ac92bbfc2579675ba3e84feb1d3040366be\",\n"
        + "            \"time\": \"2019-10-03T11:27:54.258649Z\"\n"
        + "        }\n"
        + "    ]\n"
        + "}";

    BlocksResponse blocksResponse = ExplorerApiHelper.parseGetBlocksResponse(json);

    assertThat(blocksResponse.getBlocks(), hasSize(2));
  }

  @Test
  void name2() {
    ExplorerApiHelper.parseGetBlockResponse("{\n"
        + "    \"proposer_id\": 0,\n"
        + "    \"height\": 1,\n"
        + "    \"tx_count\": 0,\n"
        + "    \"prev_hash\": \"fa066008880b19831f70807552ae92f53f4769e3684172e75cb7f2cdee92a809\",\n"
        + "    \"tx_hash\": \"c6c0aa07f27493d2f2e5cff56c890a353a20086d6c25ec825128e12ae752b2d9\",\n"
        + "    \"state_hash\": \"d525e530f6cd26e31444dc73fe8880c776ab4bed202d8cdb2c23b5137bd9d6a0\",\n"
        + "    \"precommits\": [\n"
        + "        \"2785e9b30bdba7388f3a214de9c871e58674ad80158542b894cb04da48e9e5b001001001180222220a20c12c74e064bba5adfda114e4074092231ac953b0104cecf5f6283ed20f8f0e2e2a220a20b96e9c01796ca6b812b4965df5ad676b845d3276423e557fbe47464fe7bc66e8320c08c2bad7ec0510b8e39dda010fae966ae48bd35a6d01a20cc8d027d495af2ffd3fce06318fb9b66ef2f2c1047cd0d4fa30a26fddc245def145b12c49205a66508cc19e99def8bea94294fb0e\"\n"
        + "    ],\n"
        + "    \"txs\": [],\n"
        + "    \"time\": \"2019-10-03T11:36:34.457667Z\"\n"
        + "}");
  }
}
