package com.exonum.binding.cryptocurrency.transactions;

import static com.exonum.binding.cryptocurrency.transactions.CryptocurrencyTransactionTemplate.newCryptocurrencyTransactionBuilder;
import static com.exonum.binding.cryptocurrency.transactions.TransactionPreconditions.checkTransaction;

import com.exonum.binding.crypto.PublicKey;
import com.exonum.binding.crypto.PublicKeySerializer;
import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.cryptocurrency.CryptocurrencyService;
import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.cryptocurrency.transactions.converters.TransactionMessageConverter;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.storage.serialization.Serializer;
import com.google.common.base.Objects;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.nio.ByteBuffer;

/** A transaction that transfers cryptocurrency between two wallets. */
public final class TransferTx extends BaseTx implements Transaction {

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

      ByteBuffer buffer = txMessage.getBody();
      int numBytes = buffer.remaining();
      byte[] body = new byte[numBytes];
      buffer.get(body);

      TransferTx transferTx;
      try {
        TransferTxProtos.TransferTx copiedTransferTxProtos =
            TransferTxProtos.TransferTx.parseFrom(body);
        long seed = copiedTransferTxProtos.getSeed();
        PublicKey fromWallet =
            PublicKey.fromBytes((copiedTransferTxProtos.getFromWallet().getRawKey().toByteArray()));
        PublicKey toWallet =
            PublicKey.fromBytes((copiedTransferTxProtos.getToWallet().getRawKey().toByteArray()));
        long sum = copiedTransferTxProtos.getSum();
        transferTx = new TransferTx(seed, fromWallet, toWallet, sum);
      } catch (InvalidProtocolBufferException e) {
        throw new IllegalArgumentException(
            "Unable to instantiate TransferTxProtos.TransferTx instance from provided binary data",
            e);
      }
      return transferTx;
    }

    @Override
    public BinaryMessage toMessage(TransferTx transaction) {
      PublicKeyProtos.PublicKey fromWallet = PublicKeyProtos.PublicKey.newBuilder()
          .setRawKey(ByteString.copyFrom(publicKeySerializer.toBytes(transaction.fromWallet)))
          .build();
      PublicKeyProtos.PublicKey toWallet = PublicKeyProtos.PublicKey.newBuilder()
          .setRawKey(ByteString.copyFrom(publicKeySerializer.toBytes(transaction.toWallet)))
          .build();
      TransferTxProtos.TransferTx transferTx = TransferTxProtos.TransferTx.newBuilder()
          .setSeed(transaction.seed)
          .setFromWallet(fromWallet)
          .setToWallet(toWallet)
          .setSum(transaction.sum)
          .build();

      ByteBuffer buffer = ByteBuffer.wrap(transferTx.toByteArray());

      return newCryptocurrencyTransactionBuilder(ID).setBody(buffer).buildRaw();
    }
  }
}
