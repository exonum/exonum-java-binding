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

import com.exonum.binding.common.hash.HashCode;
import com.exonum.client.response.TransactionResponse;
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
  void parseGetTxResponse() {
    String json = "{\n"
        + "    'type': 'in-pool',\n"
        + "    'content': {\n"
        + "        'debug': {\n"
        + "            'to': {\n"
        + "                'data': [242, 39, 156, 45, 245, 89, 252, 58, 143, 174, 88, 196, 76, 225, 223, 206, 177, 125, 63, 3, 90, 36, 128, 213, 70, 37, 243, 231, 174, 139, 81, 151]\n"
        + "            },\n"
        + "            'amount': 10,\n"
        + "            'seed': 9587307158524814255\n"
        + "        },\n"
        + "        'message': 'ba76da2363c062f3dc25f5e353d0f8de2e3d02896b9c8f01683dbde7e3194eb90000800000000a220a20f2279c2df559fc3a8fae58c44ce1dfceb17d3f035a2480d54625f3e7ae8b5197100a18afc7b9838aa2bd8685019a6bf7a720fdfdf27948330d0a451f35aef3eb64f80f568136be91d2667f75b7eb4b03ba279b70c69bbf5b69db8d8edf5e1049f51c86e937c10d1408374faa08'\n"
        + "    }\n"
        + "}";
    TransactionResponse transactionResponse = ExplorerApiHelper.parseTxResponse(json);

    System.out.println(transactionResponse);

  }

}
