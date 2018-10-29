/*
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.cryptocurrency.transactions;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TransactionJsonMessageTest {

  @Test
  void fromJson() {
    String message = "{ "
        + "\"protocol_version\": 1, "
        + "\"service_id\": 2, "
        + "\"message_id\": 3, "
        + "\"body\": {"
        + "  \"owner_public_key\": \"ab\""
        + "}, "
        + "\"signature\": \"cd\""
        + " }";
    Gson gson = CryptocurrencyTransactionGson.instance();

    TransactionJsonMessage tx = gson.fromJson(message,
        new TypeToken<TransactionJsonMessage<Map<String, String>>>() {}.getType());

    assertThat(tx.getProtocolVersion()).isEqualTo((byte) 1);
    assertThat(tx.getServiceId()).isEqualTo((short) 2);
    assertThat(tx.getMessageId()).isEqualTo((short) 3);
    assertThat(tx.getBody()).isEqualTo(ImmutableMap.of("owner_public_key", "ab"));
    assertThat(tx.getSignature()).isEqualTo("cd");
  }
}
