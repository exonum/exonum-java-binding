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

import static com.exonum.binding.common.serialization.StandardSerializers.protobuf;
import static com.exonum.binding.cryptocurrency.transactions.TransactionError.INSUFFICIENT_FUNDS;
import static com.exonum.binding.cryptocurrency.transactions.TransactionError.UNKNOWN_RECEIVER;
import static com.exonum.binding.cryptocurrency.transactions.TransactionError.UNKNOWN_SENDER;
import static com.exonum.binding.cryptocurrency.transactions.TransactionPreconditions.checkTransaction;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.cryptocurrency.HistoryEntity;
import com.exonum.binding.cryptocurrency.HistoryEntity.Builder;
import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.transaction.Transaction;
import com.exonum.binding.transaction.TransactionContext;
import com.exonum.binding.transaction.TransactionExecutionException;
import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ByteString;
import java.util.Objects;

/**
 * A transaction that transfers cryptocurrency between two wallets.
 */
public final class TransferTx implements Transaction {

  static final short ID = 2;
  private static final Serializer<TxMessageProtos.TransferTx> PROTO_SERIALIZER =
      protobuf(TxMessageProtos.TransferTx.class);

  private final long seed;
  private final PublicKey toWallet;
  private final long sum;

  @VisibleForTesting
  TransferTx(long seed, PublicKey toWallet,
      long sum) {
    checkArgument(0 < sum, "Non-positive transfer amount: %s", sum);
    this.seed = seed;
    this.toWallet = toWallet;
    this.sum = sum;
  }

  /**
   * Creates a new transfer transaction from the serialized transaction data.
   */
  public static TransferTx fromRawTransaction(RawTransaction rawTransaction) {
    checkTransaction(rawTransaction, ID);

    TxMessageProtos.TransferTx body =
        PROTO_SERIALIZER.fromBytes(rawTransaction.getPayload());

    long seed = body.getSeed();
    PublicKey toWallet = toPublicKey(body.getToWallet());
    long sum = body.getSum();

    return new TransferTx(seed, toWallet, sum);
  }

  private static PublicKey toPublicKey(ByteString s) {
    return PublicKey.fromBytes(s.toByteArray());
  }

  @Override
  public void execute(TransactionContext context) throws TransactionExecutionException {
    PublicKey fromWallet = context.getAuthorPk();
    checkState(!fromWallet.equals(toWallet), "Same sender and receiver: %s", fromWallet);
    CryptocurrencySchema schema = new CryptocurrencySchema(context.getFork());
    ProofMapIndexProxy<PublicKey, Wallet> wallets = schema.wallets();
    checkExecution(wallets.containsKey(fromWallet), UNKNOWN_SENDER.errorCode);
    checkExecution(wallets.containsKey(toWallet), UNKNOWN_RECEIVER.errorCode);

    Wallet from = wallets.get(fromWallet);
    Wallet to = wallets.get(toWallet);
    checkExecution(sum <= from.getBalance(), INSUFFICIENT_FUNDS.errorCode);

    wallets.put(fromWallet, new Wallet(from.getBalance() - sum));
    wallets.put(toWallet, new Wallet(to.getBalance() + sum));

    HistoryEntity historyEntity = Builder.newBuilder()
        .setSeed(seed)
        .setWalletFrom(fromWallet)
        .setWalletTo(toWallet)
        .setAmount(sum)
        .setTransactionHash(context.getTransactionMessageHash())
        .build();
    schema.walletHistory(fromWallet).add(historyEntity);
    schema.walletHistory(toWallet).add(historyEntity);
  }

  // todo: consider extracting in a TransactionPreconditions or
  //   TransactionExecutionException: ECR-2746.

  /** Checks a transaction execution precondition, throwing if it is false. */
  private static void checkExecution(boolean precondition, byte errorCode)
      throws TransactionExecutionException {
    if (!precondition) {
      throw new TransactionExecutionException(errorCode);
    }
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
        && Objects.equals(toWallet, that.toWallet);
  }

  @Override
  public int hashCode() {
    return Objects.hash(seed, toWallet, sum);
  }
}
