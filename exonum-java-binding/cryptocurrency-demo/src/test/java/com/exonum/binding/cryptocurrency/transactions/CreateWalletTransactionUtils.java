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

final class CreateWalletTransactionUtils {

  static final long DEFAULT_INITIAL_BALANCE = 100L;
  static final long DEFAULT_INITIAL_PENDING_BALANCE = 0L;
  static final PublicKey DEFAULT_INITIAL_SIGNER = PublicKey.fromHexString("abcd");

  /**
   * Creates new raw create wallet transaction with a default initial balance.
   */
  static RawTransaction createRawTransaction() {
    return createRawTransaction(DEFAULT_INITIAL_BALANCE, DEFAULT_INITIAL_SIGNER);
  }

  /**
   * Creates new raw create wallet transaction with a given initial balance.
   */
  static RawTransaction createRawTransaction(long initialBalance, PublicKey signer) {
    return RawTransaction.newBuilder()
        .serviceId(CryptocurrencyService.ID)
        .transactionId(CreateWalletTx.ID)
        .payload(TxMessageProtos.CreateWalletTx.newBuilder()
            .setInitialBalance(initialBalance)
            .setPendingBalance(DEFAULT_INITIAL_PENDING_BALANCE)
            .setSigner(fromPublicKey(signer))
            .build()
            .toByteArray())
        .build();
  }

  private static ByteString fromPublicKey(PublicKey k) {
    if (k == null)
      return null;
    return ByteString.copyFrom(k.toBytes());
  }

  private CreateWalletTransactionUtils() {
    throw new AssertionError("Non-instantiable");
  }
}
