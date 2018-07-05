/*
 * Copyright 2018 The Exonum Team
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

package com.exonum.binding.cryptocurrency.transactions;

import com.exonum.binding.cryptocurrency.CryptocurrencyService;
import com.exonum.binding.messages.Message;
import java.nio.ByteBuffer;

final class CryptocurrencyTransactionTemplate {

  private static final short INVALID_MESSAGE_TYPE = 0;

  /**
   * A template of any transaction of cryptocurrency service. It has an ID of cryptocurrency
   * service, empty body and all-zero signature.
   */
  private static final Message CRYPTOCURRENCY_TRANSACTION_TEMPLATE =
      new Message.Builder()
          .setServiceId(CryptocurrencyService.ID)
          .setMessageType(INVALID_MESSAGE_TYPE)
          .setBody(allocateReadOnly(0))
          .setSignature(new byte[Message.SIGNATURE_SIZE])
          .build();

  /**
   * Creates a new builder of cryptocurrency service transaction.
   *
   * @param transactionId a message type of transaction
   * @return a message builder that has an ID of cryptocurrency service, the given transaction ID,
   *     empty body and all-zero signature
   */
  static Message.Builder newCryptocurrencyTransactionBuilder(short transactionId) {
    return new Message.Builder()
        .mergeFrom(CRYPTOCURRENCY_TRANSACTION_TEMPLATE)
        .setMessageType(transactionId);
  }

  private static ByteBuffer allocateReadOnly(int size) {
    return ByteBuffer.allocate(size).asReadOnlyBuffer();
  }

  private CryptocurrencyTransactionTemplate() {}
}
