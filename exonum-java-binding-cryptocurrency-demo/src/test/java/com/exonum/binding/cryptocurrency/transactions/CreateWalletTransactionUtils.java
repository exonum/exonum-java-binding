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

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.cryptocurrency.CryptocurrencyService;
import com.exonum.binding.transaction.RawTransaction;
import com.google.protobuf.ByteString;

class CreateWalletTransactionUtils {

  static final long DEFAULT_BALANCE = 100L;

  /**
   * Creates new raw create wallet transaction using provided owner key and default balance.
   */
  static RawTransaction createRawTransaction(PublicKey ownerKey) {
    return createRawTransaction(ownerKey, DEFAULT_BALANCE);
  }

  /**
   * Creates new raw create wallet transaction using provided owner key and provided balance.
   */
  static RawTransaction createRawTransaction(PublicKey ownerKey, long initialBalance) {
    return RawTransaction.newBuilder()
        .serviceId(CryptocurrencyService.ID)
        .transactionId(CreateWalletTx.ID)
        .payload(TxMessageProtos.CreateWalletTx.newBuilder()
            .setOwnerPublicKey(ByteString.copyFrom(ownerKey.toBytes()))
            .setInitialBalance(initialBalance)
            .build()
            .toByteArray())
        .build();
  }

  private CreateWalletTransactionUtils() {
    throw new AssertionError("Non-instantiable");
  }
}
