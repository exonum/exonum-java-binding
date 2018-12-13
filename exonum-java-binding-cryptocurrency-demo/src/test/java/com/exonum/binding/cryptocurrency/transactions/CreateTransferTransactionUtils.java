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
import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.cryptocurrency.CryptocurrencyService;
import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.transaction.RawTransaction;
import com.google.protobuf.ByteString;

class CreateTransferTransactionUtils {

  /**
   * Creates new raw transfer transaction message using provided keys and provided amount.
   */
  static RawTransaction createRawTransaction(long seed, PublicKey senderId,
      PublicKey recipientId, long amount) {
    return RawTransaction.newBuilder()
        .serviceId(CryptocurrencyService.ID)
        .transactionId(TransferTx.ID)
        .payload(TxMessageProtos.TransferTx.newBuilder()
            .setSeed(seed)
            .setFromWallet(fromPublicKey(senderId))
            .setToWallet(fromPublicKey(recipientId))
            .setSum(amount)
            .build()
            .toByteArray())
        .build();
  }

  /**
   * Returns key byte string.
   */
  private static ByteString fromPublicKey(PublicKey k) {
    return ByteString.copyFrom(k.toBytes());
  }

  /**
   * Creates wallets with initial balance and adds them to database view.
   */
  static void createWallet(Fork view, PublicKey publicKey, Long initialBalance) {
    CryptocurrencySchema schema = new CryptocurrencySchema(view);
    MapIndex<PublicKey, Wallet> wallets = schema.wallets();
    wallets.put(publicKey, new Wallet(initialBalance));
  }

  private CreateTransferTransactionUtils() {
    throw new AssertionError("Non-instantiable");
  }
}
