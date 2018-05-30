package com.exonum.binding.cryptocurrency.transactions;

import static com.exonum.binding.cryptocurrency.transactions.CryptocurrencyTransactionTemplate.newCryptocurrencyTransactionBuilder;
import static com.exonum.binding.cryptocurrency.transactions.TransactionPreconditions.checkTransaction;
import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.crypto.PublicKey;
import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.cryptocurrency.CryptocurrencyService;
import com.exonum.binding.cryptocurrency.PublicKeySerializer;
import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.cryptocurrency.transactions.converters.TransactionMessageConverter;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.storage.serialization.StandardSerializers;
import com.google.common.base.Objects;
import java.nio.ByteBuffer;

/** A transaction that creates a new named wallet with zero balance. */
public final class CreateWalletTx extends BaseTx implements Transaction {

  private static final short ID = CryptocurrencyTransaction.CREATE_WALLET.getId();

  private final PublicKey publicKey;

  /**
   * Creates a new wallet creation transaction with given name.
   */
  public CreateWalletTx(PublicKey publicKey) {
    super(CryptocurrencyService.ID, ID);
    checkArgument(publicKey.toBytes().length > 0, "Public key must not empty");
    this.publicKey = publicKey;
  }

  static TransactionMessageConverter<CreateWalletTx> converter() {
    return TransactionConverter.INSTANCE;
  }

  @Override
  public boolean isValid() {
    return publicKey.toBytes().length > 0;
  }

  @Override
  public void execute(Fork view) {
    CryptocurrencySchema schema = new CryptocurrencySchema(view);
    MapIndex<PublicKey, Wallet> wallets = schema.wallets();

    if (wallets.containsKey(publicKey)) {
      return;
    }

    Wallet wallet = new Wallet(publicKey, 0L);

    wallets.put(publicKey, wallet);
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
    CreateWalletTx that = (CreateWalletTx) o;
    return service_id == that.service_id
        && message_id == that.message_id
        && Objects.equal(publicKey, that.publicKey);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(service_id, message_id, publicKey);
  }

  private enum TransactionConverter implements TransactionMessageConverter<CreateWalletTx> {
    INSTANCE;

    private static String getUtf8String(ByteBuffer buffer) {
      byte[] s = getRemainingBytes(buffer);

      return StandardSerializers.string().fromBytes(s);
    }

    private static byte[] getRemainingBytes(ByteBuffer buffer) {
      int numBytes = buffer.remaining();
      byte[] dst = new byte[numBytes];
      buffer.get(dst);
      return dst;
    }

    private static ByteBuffer serialize(CreateWalletTx tx) {
      byte[] nameBytes = PublicKeySerializer.INSTANCE.toBytes(tx.publicKey);
      return ByteBuffer.wrap(nameBytes);
    }

    @Override
    public CreateWalletTx fromMessage(Message txMessage) {
      checkTransaction(txMessage, ID);
      ByteBuffer body = txMessage.getBody();
      PublicKey publicKey = PublicKey.fromBytes(getRemainingBytes(body));
      return new CreateWalletTx(publicKey);
    }

    @Override
    public BinaryMessage toMessage(CreateWalletTx transaction) {
      return newCryptocurrencyTransactionBuilder(ID)
          .setBody(serialize(transaction))
          .buildRaw();
    }
  }
}
