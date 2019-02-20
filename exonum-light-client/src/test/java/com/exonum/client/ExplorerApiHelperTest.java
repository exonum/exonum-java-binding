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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.transaction.TransactionLocation;
import com.exonum.binding.common.transaction.TransactionResult;
import com.exonum.client.response.TransactionResponse;
import com.exonum.client.response.TransactionStatus;
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
        + "        'message': 'ba76da2363c062f3dc25f5e353d0f8de2e3d02896b9c8f"
        + "01683dbde7e3194eb90000800000000a220a20f2279c2df559fc3a8fae58c44ce1d"
        + "fceb17d3f035a2480d54625f3e7ae8b5197100a18afc7b9838aa2bd8685019a6bf7a"
        + "720fdfdf27948330d0a451f35aef3eb64f80f568136be91d2667f75b7eb4b03ba279b"
        + "70c69bbf5b69db8d8edf5e1049f51c86e937c10d1408374faa08'\n"
        + "    }\n"
        + "}";
    TransactionResponse transactionResponse = ExplorerApiHelper.parseGetTxResponse(json);

    assertThat(transactionResponse.getStatus(), is(TransactionStatus.IN_POOL));
    assertThat(transactionResponse.getMessage(), notNullValue());
    assertThat(transactionResponse.getExecutionResult(), nullValue());
    assertThat(transactionResponse.getLocation(), nullValue());
  }

  @Test
  void parseGetTxResponseCommitted() {
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
        + "        'message': '41c453a7f45cb0dd73644aa376d3802bb7da4c6797bcf6749211fbcabb"
        + "5aa8710000800000000a220a20ae740320999e335dd4f5fdc0468f34eb46544aa1995b6cacfcedc"
        + "82428bd71dd100a1896f7fda8bf8c8af71cfccd80d4c0d5d6f82955cf7c081969282604d3f7e416"
        + "274a4319484ceea947981b8d337bd170210acd62508f3663acba395bd131456c0b6cd7f09690aec68a05'\n"
        + "    },\n"
        + "    'location': {\n"
        + "        'block_height': 11,\n"
        + "        'position_in_block': 0\n"
        + "    },\n"
        + "    'location_proof': {\n"
        + "        'val': '2f23541b10b258dfc80693ed1bf6ed6f53ccf8908047f7d33e0fec4f29a4a613'\n"
        + "    },\n"
        + "    'status': {\n"
        + "        'type': 'success'\n"
        + "    }\n"
        + "}";
    TransactionResponse transactionResponse = ExplorerApiHelper.parseGetTxResponse(json);

    assertThat(transactionResponse.getStatus(), is(TransactionStatus.COMMITTED));
    assertThat(transactionResponse.getMessage(), notNullValue());
    assertThat(transactionResponse.getExecutionResult(), is(TransactionResult.successful()));
    assertThat(transactionResponse.getLocation(), is(TransactionLocation.valueOf(11L, 0L)));
  }

  @Test
  void parseGetTxResponseCommittedWithError() {
    String json = "{\n"
        + "    'type': 'committed',\n"
        + "    'content': {\n"
        + "        'debug': {\n"
        + "            'amount': 1,\n"
        + "            'seed': 5019726028924803177\n"
        + "        },\n"
        + "        'message': '6e1168247c11f21349775bd688473de3a4618f0b8e139df6928e8839cd00fb"
        + "70000080000100080110e9b0e1bea4c7e9d44525b5924dfe534dceab2a39a37aeef91e2fa2a770ee48c"
        + "22b7c5be294cd1d53bbb70e8c9049d910fd758da8c22a799fd52fc0ede224f95fcda121c25dbd1af80d'\n"
        + "    },\n"
        + "    'location': {\n"
        + "        'block_height': 1,\n"
        + "        'position_in_block': 0\n"
        + "    },\n"
        + "    'location_proof': {\n"
        + "        'val': 'e8a00b3747d396be45dbea3bc31cdb0721b35b9432f0ab9430d90adff744d8d6'\n"
        + "    },\n"
        + "    'status': {\n"
        + "        'type': 'error',\n"
        + "        'code': 2,\n"
        + "        'description': \"Receiver doesn't exist\"\n"
        + "    }\n"
        + "}";
    TransactionResponse transactionResponse = ExplorerApiHelper.parseGetTxResponse(json);

    assertThat(transactionResponse.getStatus(), is(TransactionStatus.COMMITTED));
    assertThat(transactionResponse.getMessage(), notNullValue());
    assertThat(transactionResponse.getExecutionResult(),
        is(TransactionResult.error(2, "Receiver doesn't exist")));
    assertThat(transactionResponse.getLocation(), is(TransactionLocation.valueOf(1L, 0L)));
  }

}
