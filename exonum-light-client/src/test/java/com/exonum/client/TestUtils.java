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

package com.exonum.client;

import static com.exonum.binding.common.crypto.CryptoFunctions.ed25519;

import com.exonum.binding.common.message.TransactionMessage;
import com.google.common.io.BaseEncoding;

final class TestUtils {
  private static final BaseEncoding HEX_ENCODER = BaseEncoding.base16().lowerCase();

  static TransactionMessage createTransactionMessage() {
    return TransactionMessage.builder()
        .serviceId((short) 10)
        .transactionId((short) 15)
        .payload(new byte[]{0x01, 0x02, 0x03})
        .sign(ed25519().generateKeyPair(), ed25519());
  }

  static String toHex(TransactionMessage message) {
    return HEX_ENCODER.encode(message.toBytes());
  }

}
