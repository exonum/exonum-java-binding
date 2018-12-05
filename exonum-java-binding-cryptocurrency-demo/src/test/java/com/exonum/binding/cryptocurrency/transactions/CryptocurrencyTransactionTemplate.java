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
import com.exonum.binding.transaction.RawTransaction;
import java.nio.ByteBuffer;

public final class CryptocurrencyTransactionTemplate {

  /**
   * Creates a new builder of cryptocurrency service raw transaction.
   *
   * @param transactionId a message type of transaction
   * @return a message builder that has an ID of cryptocurrency service, the given transaction ID,
   *     empty body
   */
  public static RawTransaction.Builder newCryptocurrencyTransactionBuilder(short transactionId) {
    return new RawTransaction.Builder()
        .serviceId(CryptocurrencyService.ID)
        .transactionId(transactionId)
        .payload(allocateReadOnly(0).array());
  }

  private static ByteBuffer allocateReadOnly(int size) {
    return ByteBuffer.allocate(size);
  }

  private CryptocurrencyTransactionTemplate() {}
}
