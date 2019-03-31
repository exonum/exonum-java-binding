package com.exonum.binding.cryptocurrency.transactions;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.cryptocurrency.HistoryEntity;
import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.transaction.Transaction;
import com.exonum.binding.transaction.TransactionContext;
import com.exonum.binding.transaction.TransactionExecutionException;
import com.google.common.annotations.VisibleForTesting;

import java.util.Objects;

import static com.exonum.binding.common.serialization.StandardSerializers.protobuf;
import static com.exonum.binding.common.serialization.json.JsonSerializer.json;
import static com.exonum.binding.cryptocurrency.transactions.TransactionError.INSUFFICIENT_FUNDS;
import static com.exonum.binding.cryptocurrency.transactions.TransactionError.SAME_SENDER_AND_RECEIVER;
import static com.exonum.binding.cryptocurrency.transactions.TransactionError.UNKNOWN_RECEIVER;
import static com.exonum.binding.cryptocurrency.transactions.TransactionError.UNKNOWN_SENDER;
import static com.exonum.binding.cryptocurrency.transactions.TransactionPreconditions.checkExecution;
import static com.exonum.binding.cryptocurrency.transactions.TransactionPreconditions.checkTransaction;
import static com.google.common.base.Preconditions.checkArgument;

public final class MultisignTransferTx implements Transaction {

    static final short ID = 3;
    private static final Serializer<TxMessageProtos.TransferTx> PROTO_SERIALIZER =
      protobuf(TxMessageProtos.TransferTx.class);

    private final long seed;
    private final PublicKey toWallet;
    private final long sum;

    @VisibleForTesting
    MultisignTransferTx(long seed, PublicKey toWallet, long sum) {
      checkArgument(0 < sum, "Non-positive transfer amount: %s", sum);
      this.seed = seed;
      this.toWallet = toWallet;
      this.sum = sum;
    }

    /**
     * Creates a new transfer transaction from the serialized transaction data.
     */
    public static MultisignTransferTx fromRawTransaction(RawTransaction rawTransaction) {
      checkTransaction(rawTransaction, ID);

      TxMessageProtos.TransferTx body =
        PROTO_SERIALIZER.fromBytes(rawTransaction.getPayload());

      long seed = body.getSeed();
      PublicKey toWallet = Wallet.toPublicKey(body.getToWallet());
      long sum = body.getSum();

      return new MultisignTransferTx(seed, toWallet, sum);
    }

    @Override
    public void execute(TransactionContext context) throws TransactionExecutionException {
      PublicKey fromWallet = context.getAuthorPk();
      checkExecution(!fromWallet.equals(toWallet), SAME_SENDER_AND_RECEIVER.errorCode);

      CryptocurrencySchema schema = new CryptocurrencySchema(context.getFork());
      ProofMapIndexProxy<PublicKey, Wallet> wallets = schema.wallets();
      checkExecution(wallets.containsKey(fromWallet), UNKNOWN_SENDER.errorCode);
      checkExecution(wallets.containsKey(toWallet), UNKNOWN_RECEIVER.errorCode);

      Wallet from = wallets.get(fromWallet);
      checkExecution(sum <= from.getBalance() + from.getPendingBalance(), INSUFFICIENT_FUNDS.errorCode);

      wallets.put(fromWallet, new Wallet(from.getBalance(), from.getPendingBalance() - sum, from.getSigner()));

      HashCode messageHash = context.getTransactionMessageHash();
      schema.transactionsHistory(fromWallet).add(messageHash);
      schema.transactionsHistory(toWallet).add(messageHash);
      HistoryEntity pendingTransaction = HistoryEntity.newBuilder()
        .setSeed(seed)
        .setWalletFrom(fromWallet)
        .setWalletTo(toWallet)
        .setAmount(sum)
        .setTxMessageHash(messageHash)
        .build();
      schema.pendingTxs().put(messageHash, pendingTransaction);
    }

    @Override
    public String info() {
      return json().toJson(this);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      MultisignTransferTx that = (MultisignTransferTx) o;
      return seed == that.seed
        && sum == that.sum
        && Objects.equals(toWallet, that.toWallet);
    }

    @Override
    public int hashCode() {
      return Objects.hash(seed, toWallet, sum);
    }
}
