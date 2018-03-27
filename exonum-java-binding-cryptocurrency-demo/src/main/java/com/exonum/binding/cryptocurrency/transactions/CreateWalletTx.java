package com.exonum.binding.cryptocurrency.transactions;

import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.cryptocurrency.transactions.converters.TransactionMessageConverter;
import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.Hashing;
import com.exonum.binding.messages.AbstractTransaction;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.messages.Message;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.storage.serialization.StandardSerializers;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import java.nio.ByteBuffer;
import java.util.Map;

import static com.exonum.binding.cryptocurrency.transactions.CryptocurrencyTransactionTemplate.newCryptocurrencyTransactionBuilder;
import static com.exonum.binding.cryptocurrency.transactions.TransactionPreconditions.checkTransaction;
import static com.google.common.base.Preconditions.checkArgument;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A transaction creating a new named wallet.
 */
final public class CreateWalletTx implements Transaction {

    private static final short ID = CryptocurrencyTransaction.CREATE_WALLET.getId();

    private final String name;

    public CreateWalletTx(String name) {
        checkArgument(!name.trim().isEmpty(), "Name must not be blank: '%s'", name);
        this.name = name;
    }

//    public CreateWalletTx(BinaryMessage message) {
//        super(checkTransaction(message, ID));
//        name = from(message.getBody());
//    }

    private static String from(ByteBuffer body) {
        byte[] s = getRemainingBytes(body);

        return StandardSerializers.string()
                .fromBytes(s);
    }

    private static byte[] getRemainingBytes(ByteBuffer body) {
        int numBytes = body.remaining();
        byte[] s = new byte[numBytes];
        body.get(s);
        return s;
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
//            TODO:
//            assert !names.containsKey(walletId) : "walletNames must not contain the getId of " + name;

            Wallet wallet = new Wallet(name, 0L);

            wallets.put(walletId, wallet);
        }
    }

    @Override
    public String info() {
        Map<String, Object> txBody = ImmutableMap.of("name", name);
        // TODO: change
        return new CryptocurrencyTransactionGson().toJson(ID, txBody);
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

    static TransactionMessageConverter<CreateWalletTx> converter() {
        return TransactionConverter.INSTANCE;
    }

    private enum TransactionConverter implements TransactionMessageConverter<CreateWalletTx> {
        INSTANCE;

        @Override
        public CreateWalletTx fromMessage(Message txMessage) {
            checkTransaction(txMessage, ID);
            ByteBuffer body = txMessage.getBody();
            String name = getUtf8String(body);
            return new CreateWalletTx(name);
        }

        private static String getUtf8String(ByteBuffer buffer) {
            byte[] s = getRemainingBytes(buffer);

            return StandardSerializers.string()
                    .fromBytes(s);
        }

        private static byte[] getRemainingBytes(ByteBuffer buffer) {
            int numBytes = buffer.remaining();
            byte[] dst = new byte[numBytes];
            buffer.get(dst);
            return dst;
        }

        @Override
        public BinaryMessage toMessage(CreateWalletTx transaction) {
            return newCryptocurrencyTransactionBuilder(ID)
                    .setBody(serialize(transaction))
                    .buildRaw();
        }

        private static ByteBuffer serialize(CreateWalletTx tx) {
            byte[] nameBytes = StandardSerializers.string().toBytes(tx.name);
            return ByteBuffer.wrap(nameBytes);
        }
    }
}
