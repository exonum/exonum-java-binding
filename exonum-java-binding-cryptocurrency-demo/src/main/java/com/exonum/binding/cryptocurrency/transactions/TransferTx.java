package com.exonum.binding.cryptocurrency.transactions;

import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.cryptocurrency.transactions.converters.TransactionMessageConverter;
import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.Hashing;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.storage.serialization.StandardSerializers;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

import static com.exonum.binding.cryptocurrency.transactions.CryptocurrencyTransactionTemplate.newCryptocurrencyTransactionBuilder;
import static com.exonum.binding.cryptocurrency.transactions.TransactionPreconditions.checkMessageSize;
import static com.exonum.binding.cryptocurrency.transactions.TransactionPreconditions.checkTransaction;

public final class TransferTx implements Transaction {

    private static final short ID = CryptocurrencyTransaction.TRANSFER.getId();

    @VisibleForTesting
    static final int BODY_SIZE = Long.BYTES * 2 + Hashing.DEFAULT_HASH_SIZE_BYTES * 2;

    private final long seed;
    private final HashCode fromWallet;
    private final HashCode toWallet;
    private final long sum;

    public TransferTx(long seed, HashCode fromWallet, HashCode toWallet, long sum) {
        this.seed = seed;
        this.fromWallet = fromWallet;
        this.toWallet = toWallet;
        this.sum = sum;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void execute(Fork view) {
        System.out.println("FROM" + fromWallet);
        System.out.println("TO" + toWallet);
        System.out.println("SUM" + sum);
        CryptocurrencySchema schema = new CryptocurrencySchema(view);
        try (ProofMapIndexProxy<HashCode, Wallet> wallets = schema.wallets()) {
            if (wallets.containsKey(fromWallet) && wallets.containsKey(toWallet)) {
                Wallet from = wallets.get(fromWallet);
                Wallet to = wallets.get(toWallet);
                assert from.getBalance() >= sum;
                wallets.put(fromWallet, new Wallet(from.getName(), from.getBalance() - sum));
                wallets.put(toWallet, new Wallet(to.getName(), to.getBalance() + sum));
            }
        }
    }

    @Override
    public String info() {
        Map<String, Object> txBody = ImmutableMap.of(
                "seed", Long.toHexString(seed),
                "from_wallet", fromWallet,
                "to_wallet", toWallet,
                "sum", Long.toHexString(sum)
        );
        return new CryptocurrencyTransactionGson().toJson(ID, txBody);
    }

    @Override
    public BinaryMessage getMessage() {
        return converter().toMessage(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransferTx that = (TransferTx) o;
        return seed == that.seed &&
                sum == that.sum &&
                Objects.equal(fromWallet, that.fromWallet) &&
                Objects.equal(toWallet, that.toWallet);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(seed, fromWallet, toWallet, sum);
    }

    static TransactionMessageConverter<TransferTx> converter() {
        return TransferTx.TransactionConverter.INSTANCE;
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
        public BinaryMessage toMessage(TransferTx transaction) {
            return newCryptocurrencyTransactionBuilder(ID)
                    .setBody(serialize(transaction))
                    .buildRaw();
        }

        private static ByteBuffer serialize(TransferTx transaction) {
            ByteBuffer body = ByteBuffer.allocate(BODY_SIZE)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .putLong(transaction.seed)
                    .put(transaction.fromWallet.asBytes())
                    .put(transaction.toWallet.asBytes())
                    .putLong(transaction.sum);
            body.rewind();
            return body;
        }
    }
}
