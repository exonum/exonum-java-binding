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

class CreateWalletTransactionUtils {

  static final long DEFAULT_INITIAL_BALANCE = 100L;

  /**
   * Creates new raw create wallet transaction with a default initial balance.
   */
  static RawTransaction createRawTransaction() {
    return createRawTransaction(DEFAULT_INITIAL_BALANCE);
  }

  /**
   * Creates new raw create wallet transaction with a given initial balance.
   */
  static RawTransaction createRawTransaction(long initialBalance) {
    return RawTransaction.newBuilder()
        .serviceId(CryptocurrencyService.ID)
        .transactionId(CreateWalletTx.ID)
        .payload(TxMessageProtos.CreateWalletTx.newBuilder()
            .setInitialBalance(initialBalance)
            .build()
            .toByteArray())
        .build();
  }

  private CreateWalletTransactionUtils() {
    throw new AssertionError("Non-instantiable");
  }
}
