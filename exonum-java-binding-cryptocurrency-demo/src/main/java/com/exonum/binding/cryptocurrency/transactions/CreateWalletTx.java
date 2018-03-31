package com.exonum.binding.cryptocurrency.transactions;

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
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.storage.serialization.StandardSerializers;
import com.google.common.base.Objects;

import java.nio.ByteBuffer;

import static com.exonum.binding.cryptocurrency.transactions.CryptocurrencyTransactionTemplate.newCryptocurrencyTransactionBuilder;
import static com.exonum.binding.cryptocurrency.transactions.TransactionPreconditions.checkTransaction;
import static com.google.common.base.Preconditions.checkArgument;
import static java.nio.charset.StandardCharsets.UTF_8;

/** A transaction that creates a new named wallet with zero balance. */
public final class CreateWalletTx extends BaseTx implements Transaction {

  private static final short ID = CryptocurrencyTransaction.CREATE_WALLET.getId();

  private final String name;

  public CreateWalletTx(String name) {
    super(CryptocurrencyService.ID, ID);
    checkArgument(!name.trim().isEmpty(), "Name must not be blank: '%s'", name);
    this.name = name;
  }

  static TransactionMessageConverter<CreateWalletTx> converter() {
    return TransactionConverter.INSTANCE;
  }

  @Override
  public boolean isValid() {
    return !name.trim().isEmpty();
  }

  @Override
  public void execute(Fork view) {
    CryptocurrencySchema schema = new CryptocurrencySchema(view);
    try (MapIndex<HashCode, Wallet> wallets = schema.wallets()) {

      HashCode walletId = Hashing.defaultHashFunction().hashString(name, UTF_8);
      if (wallets.containsKey(walletId)) {
        return;
      }

      Wallet wallet = new Wallet(name, 0L);

      wallets.put(walletId, wallet);
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
    CreateWalletTx that = (CreateWalletTx) o;
    return Objects.equal(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name);
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
      byte[] nameBytes = StandardSerializers.string().toBytes(tx.name);
      return ByteBuffer.wrap(nameBytes);
    }

    @Override
    public CreateWalletTx fromMessage(Message txMessage) {
      checkTransaction(txMessage, ID);
      ByteBuffer body = txMessage.getBody();
      String name = getUtf8String(body);
      return new CreateWalletTx(name);
    }

    @Override
    public BinaryMessage toMessage(CreateWalletTx transaction) {
      return newCryptocurrencyTransactionBuilder(ID).setBody(serialize(transaction)).buildRaw();
    }
  }
}
