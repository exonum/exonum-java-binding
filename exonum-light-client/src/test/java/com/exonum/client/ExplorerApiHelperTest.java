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
import static com.exonum.client.Blocks.BLOCK_1_TIME;
import static com.exonum.client.Blocks.BLOCK_2;
import static com.exonum.client.Blocks.BLOCK_2_JSON;
import static com.exonum.client.Blocks.BLOCK_2_TIME;
import static com.exonum.client.Blocks.BLOCK_3;
import static com.exonum.client.Blocks.BLOCK_3_JSON;
import static com.exonum.client.Blocks.BLOCK_3_TIME;
import static com.exonum.client.TestUtils.createTransactionMessage;
import static com.exonum.client.TestUtils.toHex;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.blockchain.TransactionLocation;
import com.exonum.binding.common.blockchain.TransactionResult;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.client.response.BlockResponse;
import com.exonum.client.response.BlocksResponse;
import com.exonum.client.response.TransactionResponse;
import com.exonum.client.response.TransactionStatus;
import org.junit.jupiter.api.Test;

class ExplorerApiHelperTest {

  @Test
  void parseSubmitTxResponse() {
    String expected = "f128c720e04b8243";
    String json = "{'tx_hash':'" + expected + "'}";

    HashCode actual = ExplorerApiHelper.parseSubmitTxResponse(json);
    assertThat(actual).isEqualTo(HashCode.fromString(expected));
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

    // todo: Prototype custom Subjects on the response
    assertThat(transactionResponse.getStatus()).isEqualTo(TransactionStatus.IN_POOL);
    assertThat(transactionResponse.getMessage()).isEqualTo(expectedMessage);
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

    assertThat(transactionResponse.getStatus()).isEqualTo(TransactionStatus.COMMITTED);
    assertThat(transactionResponse.getMessage()).isEqualTo(expectedMessage);
    assertThat(transactionResponse.getExecutionResult()).isEqualTo(TransactionResult.successful());
    assertThat(transactionResponse.getLocation()).isEqualTo(TransactionLocation.valueOf(11L, 0L));
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

    assertThat(transactionResponse.getStatus()).isEqualTo(TransactionStatus.COMMITTED);
    assertThat(transactionResponse.getMessage()).isEqualTo(expectedMessage);
    assertThat(transactionResponse.getExecutionResult())
        .isEqualTo(TransactionResult.error(errorCode, errorDescription));
    assertThat(transactionResponse.getLocation()).isEqualTo(TransactionLocation.valueOf(1L, 0L));
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

    assertThat(transactionResponse.getStatus()).isEqualTo(TransactionStatus.COMMITTED);
    assertThat(transactionResponse.getMessage()).isEqualTo(expectedMessage);
    assertThat(transactionResponse.getExecutionResult())
        .isEqualTo(TransactionResult.unexpectedError(errorDescription));
    assertThat(transactionResponse.getLocation()).isEqualTo(TransactionLocation.valueOf(1L, 0L));
  }

  @Test
  void parseGetBlockResponse() {
    String tx1 = "336a4acbe2ff0dd18989316f4bc8d17a4bfe79985424fe483c45e8ac92963d13";
    String json = "{\n"
        + "    'block': " + BLOCK_1_JSON + ",\n"
        + "    'precommits': ['a410964c2c21199b48e2'],\n"
        + "    'txs': ['" + tx1 + "'],\n"
        + "    'time': '" + BLOCK_1_TIME + "'\n"
        + "}";

    BlockResponse response = ExplorerApiHelper.parseGetBlockResponse(json);

    assertThat(response.getBlock()).isEqualTo(BLOCK_1);
    assertThat(response.getTransactionHashes())
        .containsExactly(HashCode.fromString(tx1))
        .inOrder();
  }

  @Test
  void parseGetBlocksResponse() {
    String json = "{\n"
        + "    'range': {\n"
        + "        'start': 6,\n"
        + "        'end': 288\n"
        + "    },\n"
        + "    'blocks': [ " + BLOCK_1_JSON + "," + BLOCK_2_JSON + "," + BLOCK_3_JSON + "],\n"
        + "    'times': ['" + BLOCK_1_TIME + "','" + BLOCK_2_TIME + "','" + BLOCK_3_TIME
        + "']\n"
        + "}\n";

    BlocksResponse response = ExplorerApiHelper.parseGetBlocksResponse(json);

    assertThat(response.getBlocks())
        .containsExactly(BLOCK_1, BLOCK_2, BLOCK_3)
        .inOrder();
    assertThat(response.getBlocksRangeStart()).isEqualTo(6L);
    assertThat(response.getBlocksRangeEnd()).isEqualTo(288L);
  }

}
