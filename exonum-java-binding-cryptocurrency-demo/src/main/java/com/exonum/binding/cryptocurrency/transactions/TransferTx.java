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
import com.exonum.binding.common.message.BinaryMessage;
import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.transaction.AbstractTransaction;
import com.exonum.binding.transaction.Transaction;
import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;

/**
 * A transaction that transfers cryptocurrency between two wallets.
 */
public final class TransferTx extends AbstractTransaction implements Transaction {

  static final short ID = 2;

  private final TransferTxData data;

  @VisibleForTesting
  TransferTx(BinaryMessage message, long seed, PublicKey fromWallet, PublicKey toWallet,
      long sum) {
    super(message);
    data = new TransferTxData(seed, fromWallet, toWallet, sum);
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
    return getMessage().verify(CRYPTO_FUNCTION, data.getSenderId());
  }

  @Override
  public void execute(Fork view) {
    CryptocurrencySchema schema = new CryptocurrencySchema(view);
    ProofMapIndexProxy<PublicKey, Wallet> wallets = schema.wallets();
    if (wallets.containsKey(data.getRecipientId()) && wallets.containsKey(data.getSenderId())) {
      Wallet from = wallets.get(data.getSenderId());
      Wallet to = wallets.get(data.getRecipientId());
      if (from.getBalance() < data.getAmount()
          || data.getSenderId().equals(data.getRecipientId())) {
        return;
      }
      wallets.put(data.getSenderId(), new Wallet(from.getBalance() - data.getAmount()));
      schema.walletHistory(data.getSenderId()).add(data);
      wallets.put(data.getRecipientId(), new Wallet(to.getBalance() + data.getAmount()));
      schema.walletHistory(data.getRecipientId()).add(data);
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
    return Objects.equals(this.data, that.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(data);
  }
}
