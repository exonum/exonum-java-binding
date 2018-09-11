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
import static com.exonum.binding.cryptocurrency.transactions.TransactionPreconditions.checkTransaction;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.transaction.AbstractTransaction;
import com.exonum.binding.transaction.Transaction;
import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;

/** A transaction that transfers cryptocurrency between two wallets. */
public final class TransferTx extends AbstractTransaction implements Transaction {

  static final short ID = 2;

  private final long seed;
  private final PublicKey fromWallet;
  private final PublicKey toWallet;
  private final long sum;

  @VisibleForTesting
  TransferTx(BinaryMessage message, long seed, PublicKey fromWallet, PublicKey toWallet,
             long sum) {
    super(message);
    this.seed = seed;
    this.fromWallet = fromWallet;
    this.toWallet = toWallet;
    this.sum = sum;
  }

  /**
   * Creates a new transfer transaction from the binary message.
   */
  public static TransferTx fromMessage(BinaryMessage message) {
    checkTransaction(message, ID);

    try {
      TxMessagesProtos.TransferTx messageBody =
          TxMessagesProtos.TransferTx.parseFrom(message.getBody());

      long seed = messageBody.getSeed();
      PublicKey fromWallet = toPublicKey(messageBody.getFromWallet());
      PublicKey toWallet = toPublicKey(messageBody.getToWallet());
      long sum = messageBody.getSum();

      return new TransferTx(message, seed, fromWallet, toWallet, sum);
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException("Invalid TxMessagesProtos.TransferTx buffer", e);
    }
  }

  private static PublicKey toPublicKey(ByteString s) {
    return PublicKey.fromBytes(s.toByteArray());
  }

  @Override
  public boolean isValid() {
    return getMessage().verify(CRYPTO_FUNCTION, fromWallet);
  }

  @Override
  public void execute(Fork view) {
    CryptocurrencySchema schema = new CryptocurrencySchema(view);
    ProofMapIndexProxy<PublicKey, Wallet> wallets = schema.wallets();
    if (wallets.containsKey(fromWallet) && wallets.containsKey(toWallet)) {
      Wallet from = wallets.get(fromWallet);
      Wallet to = wallets.get(toWallet);
      if (from.getBalance() < sum || fromWallet.equals(toWallet)) {
        return;
      }
      wallets.put(fromWallet, new Wallet(from.getBalance() - sum));
      wallets.put(toWallet, new Wallet(to.getBalance() + sum));
    }
  }

  @Override
  public String info() {
    return CryptocurrencyTransactionGson.instance().toJson(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TransferTx that = (TransferTx) o;
    return seed == that.seed
        && sum == that.sum
        && Objects.equals(fromWallet, that.fromWallet)
        && Objects.equals(toWallet, that.toWallet);
  }

  @Override
  public int hashCode() {
    return Objects.hash(seed, fromWallet, toWallet, sum);
  }
}
