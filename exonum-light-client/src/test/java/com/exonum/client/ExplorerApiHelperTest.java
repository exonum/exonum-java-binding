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
    String json = "{\"tx_hash\":\"" + expected + "\"}";

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
        + "        'description': \"" + errorDescription + "\""
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
    String previousHash = "fd510fc923683a4bb77af8278cd51676fbd0fcb25e2437bd69513d468b874bbb";
    String txHash = "336a4acbe2ff0dd18989316f4bc8d17a4bfe79985424fe483c45e8ac92963d13";
    String stateHash = "79a6f0fa233cc2d7d2e96855ec14bdcc4c0e0bb1a99ccaa912a555441e3b7512";
    String tx1 = "336a4acbe2ff0dd18989316f4bc8d17a4bfe79985424fe483c45e8ac92963d13";
    String time = "2019-02-14T14:12:52.037255Z";
    String json = "{\n"
        + "    \"block\": {\n"
        + "        \"proposer_id\": 3,\n"
        + "        \"height\": 1,\n"
        + "        \"tx_count\": 1,\n"
        + "        \"prev_hash\": \"" + previousHash + "\",\n"
        + "        \"tx_hash\": \"" + txHash + "\",\n"
        + "        \"state_hash\": \"" + stateHash + "\"\n"
        + "    },\n"
        + "    \"precommits\": [\"a410964c2c21199b48e2\"],\n"
        + "    \"txs\": [\"" + tx1 + "\"],\n"
        + "    \"time\": \"" + time + "\"\n"
        + "}";

    BlockResponse response = ExplorerApiHelper.parseGetBlockResponse(json);

    assertThat(response.getBlock().getHeight(), is(1L));
    assertThat(response.getBlock().getProposerId(), is(3));
    assertThat(response.getBlock().getNumTransactions(), is(1));
    assertThat(response.getBlock().getPreviousBlockHash(), is(HashCode.fromString(previousHash)));
    assertThat(response.getBlock().getStateHash(), is(HashCode.fromString(stateHash)));
    assertThat(response.getBlock().getTxRootHash(), is(HashCode.fromString(txHash)));
    assertThat(response.getTime(), is(ZonedDateTime.parse(time)));
    assertThat(response.getTransactionHashes(), contains(HashCode.fromString(tx1)));
  }


  @Test
  void parseGetBlocksResponse() {
    String json = "{\n"
        + "    \"range\": {\n"
        + "        \"start\": 6,\n"
        + "        \"end\": 288\n"
        + "    },\n"
        + "    \"blocks\": [{\n"
        + "        \"proposer_id\": 3,\n"
        + "        \"height\": 26,\n"
        + "        \"tx_count\": 1,\n"
        + "        \"prev_hash\": \"932470a22d37a5a995519e01c50eab7db9e0e978f5b1\",\n"
        + "        \"tx_hash\": \"5cc41a2a7cf7c0d3a15ab6ca775b601208dba7d506e2\",\n"
        + "        \"state_hash\": \"4d7bb34d7913e0784c24a1e440532e72900eb3801290\"\n"
        + "    }, {\n"
        + "        \"proposer_id\": 2,\n"
        + "        \"height\": 21,\n"
        + "        \"tx_count\": 1,\n"
        + "        \"prev_hash\": \"aa4ec89740a4ec380e8bcab0aedd0f5449184eb33b65\",\n"
        + "        \"tx_hash\": \"dcb05a3bd61f9b637335472802d8ab6026c8486dae3b\",\n"
        + "        \"state_hash\": \"e4ea2c6118326c6b00cd14ec7b8fb4cbf198eb4e6514\"\n"
        + "    }, {\n"
        + "        \"proposer_id\": 1,\n"
        + "        \"height\": 16,\n"
        + "        \"tx_count\": 1,\n"
        + "        \"prev_hash\": \"7183517c34e94ecc10a3e13269da2bfadb6e87eea864\",\n"
        + "        \"tx_hash\": \"362bc50ed56d33944a0d33fbac2a25fc08ceb8dc1ace\",\n"
        + "        \"state_hash\": \"00cca5682b677d4b4ac644d2ddae09ca5e260fb67c73\"\n"
        + "    }, {\n"
        + "        \"proposer_id\": 0,\n"
        + "        \"height\": 11,\n"
        + "        \"tx_count\": 1,\n"
        + "        \"prev_hash\": \"9297ef66d1d9ec286c00aec779f2dc273b3371e792bb\",\n"
        + "        \"tx_hash\": \"c7aa20695380846e3f274d3d51c68e864e66e46f2618\",\n"
        + "        \"state_hash\": \"deb57ff0f82c9d2514dc51785675544e27b3054512ea\"\n"
        + "    }, {\n"
        + "        \"proposer_id\": 3,\n"
        + "        \"height\": 6,\n"
        + "        \"tx_count\": 1,\n"
        + "        \"prev_hash\": \"dbec8f64a85ab56985c7ab7e63a191764f4d5c373c67\",\n"
        + "        \"tx_hash\": \"ffee3d630f137aecff95aece36cfe4dc1b42f688d474\",\n"
        + "        \"state_hash\": \"8ac9f2af6266b8e9b61fa7f3fcdd170375fb1bf8cc8d\"\n"
        + "    }],\n"
        + "    \"times\": [\"2019-02-21T13:01:44.321051Z\", \n"
        + "             \"2019-02-21T13:01:43.287648Z\", \n"
        + "             \"2019-02-21T13:01:42.251382Z\", \n"
        + "             \"2019-02-21T13:01:41.228900Z\", \n"
        + "             \"2019-02-21T13:01:40.199265Z\"]\n"
        + "}\n";

    BlocksResponse response = ExplorerApiHelper.parseGetBlocksResponse(json);

    assertThat(response.getBlocks(), hasSize(5));
    assertThat(response.getTimes(), hasSize(5));
    assertThat(response.getBlocksRangeStart(), is(6L));
    assertThat(response.getBlocksRangeEnd(), is(288L));
  }

}
