package com.exonum.binding.cryptocurrency.transactions;

import static com.exonum.binding.cryptocurrency.transactions.CryptocurrencyTransactionTemplate.newCryptocurrencyTransactionBuilder;
import static com.exonum.binding.cryptocurrency.transactions.TransactionPreconditions.checkMessageSize;
import static com.exonum.binding.cryptocurrency.transactions.TransactionPreconditions.checkTransaction;

import com.exonum.binding.crypto.PublicKey;
import com.exonum.binding.crypto.PublicKeySerializer;
import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.cryptocurrency.CryptocurrencyService;
import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.cryptocurrency.transactions.converters.TransactionMessageConverter;
import com.exonum.binding.hash.Hashing;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.storage.serialization.Serializer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** A transaction that transfers cryptocurrency between two wallets. */
public final class TransferTx extends BaseTx implements Transaction {

  @VisibleForTesting
  static final int BODY_SIZE = Long.BYTES * 2 + Hashing.DEFAULT_HASH_SIZE_BYTES * 2;

  private static final Serializer<PublicKey> publicKeySerializer = PublicKeySerializer.INSTANCE;

  private static final short ID = CryptocurrencyTransaction.TRANSFER.getId();
  private final long seed;
  private final PublicKey fromWallet;
  private final PublicKey toWallet;
  private final long sum;

  /**
   * Creates a new transfer transaction with given seed, fromWallet and toWallet HashCode and sum of
   * the transfer.
   */
  public TransferTx(long seed, PublicKey fromWallet, PublicKey toWallet, long sum) {
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
    return service_id == that.service_id
        && message_id == that.message_id
        && seed == that.seed
        && sum == that.sum
        && Objects.equal(fromWallet, that.fromWallet)
        && Objects.equal(toWallet, that.toWallet);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(service_id, message_id, seed, fromWallet, toWallet, sum);
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
      PublicKey fromWallet = publicKeySerializer.fromBytes(fromHash);
      byte[] toHash = new byte[Hashing.DEFAULT_HASH_SIZE_BYTES];
      buf.get(toHash);
      PublicKey toWallet = publicKeySerializer.fromBytes(toHash);
      long sum = buf.getLong();
      return new TransferTx(seed, fromWallet, toWallet, sum);
    }

    @Override
    public BinaryMessage toMessage(TransferTx transaction) {
      ByteBuffer body =
          ByteBuffer.allocate(BODY_SIZE)
              .order(ByteOrder.LITTLE_ENDIAN)
              .putLong(transaction.seed)
              .put(publicKeySerializer.toBytes(transaction.fromWallet))
              .put(publicKeySerializer.toBytes(transaction.toWallet))
              .putLong(transaction.sum);
      body.rewind();

      return newCryptocurrencyTransactionBuilder(ID).setBody(body).buildRaw();
    }
  }
}
