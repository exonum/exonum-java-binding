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
import com.google.common.base.Objects;
import com.google.protobuf.ByteString;

import static com.exonum.binding.common.serialization.StandardSerializers.protobuf;
import static com.exonum.binding.common.serialization.json.JsonSerializer.json;
import static com.exonum.binding.cryptocurrency.transactions.TransactionError.SAME_SIGNER_AND_TX_SENDER;
import static com.exonum.binding.cryptocurrency.transactions.TransactionError.SIGNER_NOT_EQUALS_TO_SENDER;
import static com.exonum.binding.cryptocurrency.transactions.TransactionPreconditions.checkExecution;
import static com.exonum.binding.cryptocurrency.transactions.TransactionPreconditions.checkTransaction;

public final class SignMultisignTransferTx implements Transaction {

  static final short ID = 4;
  private static final Serializer<TxMessageProtos.SignMultisignTransferTx> PROTO_SERIALIZER =
    protobuf(TxMessageProtos.SignMultisignTransferTx.class);

  private final long seed;
  private final HashCode txHash;

  @VisibleForTesting
  SignMultisignTransferTx(long seed, HashCode txHash) {
    this.seed = seed;
    this.txHash = txHash;
  }

  public static SignMultisignTransferTx fromRawTransaction(RawTransaction rawTransaction) {
    checkTransaction(rawTransaction, ID);

    TxMessageProtos.SignMultisignTransferTx body =
      PROTO_SERIALIZER.fromBytes(rawTransaction.getPayload());

    long seed = body.getSeed();
    HashCode txHash = toHashCode(body.getTxHash());
    return new SignMultisignTransferTx(seed, txHash);
  }

  private static HashCode toHashCode(ByteString s) {
    return HashCode.fromBytes(s.toByteArray());
  }

  @Override
  public void execute(TransactionContext context) throws TransactionExecutionException {
    PublicKey txSender = context.getAuthorPk();

    CryptocurrencySchema schema = new CryptocurrencySchema(context.getFork());
    HistoryEntity pendingTransaction = schema.pendingTxs().get(txHash);
    PublicKey fromWalletKey = pendingTransaction.getWalletFrom();
    PublicKey toWalletKey = pendingTransaction.getWalletTo();
    checkExecution(!txSender.equals(fromWalletKey), SAME_SIGNER_AND_TX_SENDER.errorCode);
    checkExecution(!txSender.equals(toWalletKey), SAME_SIGNER_AND_TX_SENDER.errorCode);

    ProofMapIndexProxy<PublicKey, Wallet> wallets = schema.wallets();
    Wallet fromWallet = wallets.get(fromWalletKey);
    checkExecution(fromWallet.getSigner().equals(txSender), SIGNER_NOT_EQUALS_TO_SENDER.errorCode);
    Wallet toWallet = wallets.get(toWalletKey);
    long sum = pendingTransaction.getAmount();
    wallets.put(fromWalletKey,
      new Wallet(
        fromWallet.getBalance() - sum,
        fromWallet.getPendingBalance() + sum,
        fromWallet.getSigner()));
    wallets.put(toWalletKey,
      new Wallet(
        toWallet.getBalance() + sum,
        toWallet.getPendingBalance(),
        toWallet.getSigner()));

    schema.pendingTxs().remove(txHash);

    HashCode messageHash = context.getTransactionMessageHash();
    schema.transactionsHistory(fromWalletKey).add(messageHash);
    schema.transactionsHistory(toWalletKey).add(messageHash);
    schema.transactionsHistory(txSender).add(messageHash);
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
    if (!(o instanceof SignMultisignTransferTx)) {
      return false;
    }
    SignMultisignTransferTx that = (SignMultisignTransferTx) o;
    return seed == that.seed &&
      Objects.equal(txHash, that.txHash);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(seed, txHash);
  }
}
