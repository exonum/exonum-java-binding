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

import static com.exonum.binding.cryptocurrency.CryptocurrencyServiceImpl.CRYPTO_FUNCTION;
import static com.exonum.binding.cryptocurrency.transactions.CryptocurrencyTransactionTemplate.newCryptocurrencyTransactionBuilder;

import com.exonum.binding.crypto.PrivateKey;
import com.exonum.binding.crypto.PublicKey;
import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.indices.MapIndex;
import com.google.protobuf.ByteString;


public class CreateTransferTransactionUtils {

  /**
   * Creates new signed binary transfer transaction message using provided keys and
   * provided amount.
   */
  public static BinaryMessage createSignedMessage(long seed, PublicKey senderId, PrivateKey senderSecret,
      PublicKey recipientId, long amount) {
    BinaryMessage packetUnsigned = createUnsignedMessage(seed, senderId, recipientId, amount);
    return packetUnsigned.sign(CRYPTO_FUNCTION, senderSecret);
  }

  /**
   * Creates new unsigned binary transfer transaction message using provided keys and
   * provided amount.
   */
  public static BinaryMessage createUnsignedMessage(long seed, PublicKey senderId, PublicKey recipientId,
      long amount) {
    return newCryptocurrencyTransactionBuilder(TransferTx.ID)
        .setBody(TxMessagesProtos.TransferTx.newBuilder()
            .setSeed(seed)
            .setFromWallet(fromPublicKey(senderId))
            .setToWallet(fromPublicKey(recipientId))
            .setSum(amount)
            .build()
            .toByteArray())
        .buildRaw();
  }

  /**
   * Returns key byte string.
   */
  public static ByteString fromPublicKey(PublicKey k) {
    return ByteString.copyFrom(k.toBytes());
  }

  /**
   * Creates wallets with initial balance and adds them to database view.
   */
  public static void createWallet(Fork view, PublicKey publicKey, Long initialBalance) {
    CryptocurrencySchema schema = new CryptocurrencySchema(view);
    MapIndex<PublicKey, Wallet> wallets = schema.wallets();
    wallets.put(publicKey, new Wallet(initialBalance));
  }
}
