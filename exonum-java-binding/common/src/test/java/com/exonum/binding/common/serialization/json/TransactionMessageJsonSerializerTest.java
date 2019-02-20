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
 */

package com.exonum.binding.common.serialization.json;

import static com.exonum.binding.common.crypto.CryptoFunctions.ed25519;
import static com.exonum.binding.common.serialization.json.JsonSerializer.json;
import static com.exonum.binding.test.Bytes.bytes;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.message.TransactionMessage;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class TransactionMessageJsonSerializerTest {

  @ParameterizedTest
  @MethodSource("source")
  void roundTripTest(TransactionMessage msg) {
    String json = json().toJson(msg);
    TransactionMessage restoredMsg = json().fromJson(json, TransactionMessage.class);

    assertThat(restoredMsg, is(msg));
  }

  private static List<TransactionMessage> source() {
    KeyPair keys = ed25519().generateKeyPair();

    return ImmutableList.of(
        TransactionMessage.builder()
            .serviceId(Short.MIN_VALUE)
            .transactionId(Short.MAX_VALUE)
            .payload(bytes())
            .sign(keys, ed25519()),
        TransactionMessage.builder()
            .serviceId((short) 0)
            .transactionId((short) 127)
            .payload(bytes(0x00, 0x01, 0x02))
            .sign(keys, ed25519())
    );
  }

}
