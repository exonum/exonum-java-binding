package com.exonum.binding.cryptocurrency.transactions;

import static com.exonum.binding.cryptocurrency.transactions.CryptocurrencyTransactionTemplate.newCryptocurrencyTransactionBuilder;
import static com.exonum.binding.cryptocurrency.transactions.TransactionPreconditions.checkMessageSize;
import static com.exonum.binding.cryptocurrency.transactions.TransactionPreconditions.checkTransaction;

import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.cryptocurrency.CryptocurrencyService;
import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.cryptocurrency.transactions.converters.TransactionMessageConverter;
import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.Hashing;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** A transaction that transfers cryptocurrency between two wallets. */
public final class TransferTx extends BaseTx implements Transaction {

  @VisibleForTesting
  static final int BODY_SIZE = Long.BYTES * 2 + Hashing.DEFAULT_HASH_SIZE_BYTES * 2;

  private static final short ID = CryptocurrencyTransaction.TRANSFER.getId();
  private final long seed;
  private final HashCode fromWallet;
  private final HashCode toWallet;
  private final long sum;

  public TransferTx(long seed, HashCode fromWallet, HashCode toWallet, long sum) {
    super(CryptocurrencyService.ID, ID);
    this.seed = seed;
    this.fromWallet = fromWallet;
    this.toWallet = toWallet;
    this.sum = sum;
  }

  static TransactionMessageConverter<TransferTx> converter() {
    return TransferTx.TransactionConverter.INSTANCE;
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public void execute(Fork view) {
    CryptocurrencySchema schema = new CryptocurrencySchema(view);
    try (ProofMapIndexProxy<HashCode, Wallet> wallets = schema.wallets()) {
      if (wallets.containsKey(fromWallet) && wallets.containsKey(toWallet)) {
        Wallet from = wallets.get(fromWallet);
        Wallet to = wallets.get(toWallet);
        if (from.getBalance() < sum) {
          return;
        }
        wallets.put(fromWallet, new Wallet(from.getName(), from.getBalance() - sum));
        wallets.put(toWallet, new Wallet(to.getName(), to.getBalance() + sum));
      }
    }
  }

  @Override
  public String info() {
    return CryptocurrencyTransactionGson.instance().toJson(this);
  }

  @Override
  public BinaryMessage getMessage() {
    return converter().toMessage(this);
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
        && Objects.equal(fromWallet, that.fromWallet)
        && Objects.equal(toWallet, that.toWallet);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(seed, fromWallet, toWallet, sum);
  }

  private enum TransactionConverter implements TransactionMessageConverter<TransferTx> {
    INSTANCE;

    @Override
    public TransferTx fromMessage(Message txMessage) {
      checkTransaction(txMessage, ID);
      checkMessageSize(txMessage, BODY_SIZE);

      ByteBuffer buf = txMessage.getBody().order(ByteOrder.LITTLE_ENDIAN);
      assert buf.remaining() == BODY_SIZE;

      long seed = buf.getLong();

      byte[] fromHash = new byte[Hashing.DEFAULT_HASH_SIZE_BYTES];
      buf.get(fromHash);
      HashCode fromWallet = HashCode.fromBytes(fromHash);
      byte[] toHash = new byte[Hashing.DEFAULT_HASH_SIZE_BYTES];
      buf.get(toHash);
      HashCode toWallet = HashCode.fromBytes(toHash);
      long sum = buf.getLong();
      return new TransferTx(seed, fromWallet, toWallet, sum);
    }

    @Override
    public BinaryMessage toMessage(TransferTx transaction) {
      ByteBuffer body =
          ByteBuffer.allocate(BODY_SIZE)
              .order(ByteOrder.LITTLE_ENDIAN)
              .putLong(transaction.seed)
              .put(transaction.fromWallet.asBytes())
              .put(transaction.toWallet.asBytes())
              .putLong(transaction.sum);
      body.rewind();

      return newCryptocurrencyTransactionBuilder(ID).setBody(body).buildRaw();
    }
  }
}
