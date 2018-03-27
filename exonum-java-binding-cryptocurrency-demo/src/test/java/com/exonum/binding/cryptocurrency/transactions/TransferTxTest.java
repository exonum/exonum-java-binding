package com.exonum.binding.cryptocurrency.transactions;

import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.Hashing;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.util.LibraryLoader;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.exonum.binding.cryptocurrency.transactions.CryptocurrencyTransaction.TRANSFER;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TransferTxTest {
    static {
        LibraryLoader.load();
    }

    static Message TX_MESSAGE_TEMPLATE = new Message.Builder()
            .mergeFrom(Transactions.TX_MESSAGE_TEMPLATE)
            .setMessageType(TRANSFER.getId())
            .setBody(ByteBuffer.allocate(TransferTx.BODY_SIZE))
            .buildPartial();

    @Test
    public void isValid() {
//        BinaryMessage message = messageBuilder()
//                .buildRaw();
//        TransferTx tx = new TransferTx(message);
//
//        assertTrue(tx.isValid());
    }

    @Test
    public void executeTransfer() {
        try (Database db = new MemoryDb();
             Fork view = db.createFork()) {
            // Add a new wallet with the given name and initial value
            String from = "wallet-1";
            String to = "wallet-2";
            long seed = 1;
            long initialValue = 100;
            long transferSum = 40;
            createWallet(view, from, initialValue);
            createWallet(view, to, initialValue);

            // Create and execute the transaction
            HashCode fromWallet = Hashing.defaultHashFunction().hashString(from, UTF_8);
            HashCode toWallet = Hashing.defaultHashFunction().hashString(to, UTF_8);
            TransferTx tx = new TransferTx(seed, fromWallet, toWallet, transferSum);
            tx.execute(view);

            // Check that wallets have correct values
            CryptocurrencySchema schema = new CryptocurrencySchema(view);
            try (ProofMapIndexProxy<HashCode, Wallet> wallets = schema.wallets()) {
                long expectedFromValue = initialValue - transferSum;
                assertThat(wallets.get(fromWallet).getBalance(), equalTo(expectedFromValue));
                long expectedToValue = initialValue + transferSum;
                assertThat(wallets.get(toWallet).getBalance(), equalTo(expectedToValue));
            }
        }
    }

    @Test
    public void executeNoSuchFromWallet() {
        try (Database db = new MemoryDb();
             Fork view = db.createFork()) {
            // Create and execute the transaction that attempts to transfer to unknown wallet
            String from = "from-wallet";
            String to = "unknown-wallet";
            HashCode fromWallet = Hashing.defaultHashFunction().hashString(from, UTF_8);
            HashCode toWallet = Hashing.defaultHashFunction().hashString(to, UTF_8);
            long initialValue = 50L;
            createWallet(view, from, initialValue);
            long transferValue = 50L;
            long seed = 1L;
            TransferTx tx = new TransferTx(seed, fromWallet, toWallet, transferValue);
            tx.execute(view);

            // Check that balance of fromWallet is unchanged
            CryptocurrencySchema schema = new CryptocurrencySchema(view);
            try (MapIndex<HashCode, Wallet> wallets = schema.wallets()) {
                assertThat(wallets.get(fromWallet).getBalance(), equalTo(initialValue));
            }
        }
    }

    @Test
    public void executeNoSuchToWallet() {
        try (Database db = new MemoryDb();
             Fork view = db.createFork()) {
            // Create and execute the transaction that attempts to transfer from unknown wallet
            String from = "unknown-wallet";
            String to = "to-wallet";
            HashCode fromWallet = Hashing.defaultHashFunction().hashString(from, UTF_8);
            HashCode toWallet = Hashing.defaultHashFunction().hashString(to, UTF_8);
            long initialValue = 50L;
            createWallet(view, to, initialValue);
            long transferValue = 50L;
            long seed = 1L;
            TransferTx tx = new TransferTx(seed, fromWallet, toWallet, transferValue);
            tx.execute(view);

            // Check that balance of toWallet is unchanged
            CryptocurrencySchema schema = new CryptocurrencySchema(view);
            try (MapIndex<HashCode, Wallet> wallets = schema.wallets()) {
                assertThat(wallets.get(toWallet).getBalance(), equalTo(initialValue));
            }
        }
    }

    @Test
    public void converterRoundtrip() {
        long seed = 0;
        String from = "from-wallet";
        String to = "to-wallet";
        HashCode fromWallet = Hashing.defaultHashFunction().hashString(from, UTF_8);
        HashCode toWallet = Hashing.defaultHashFunction().hashString(to, UTF_8);
        long sum = 50L;

        TransferTx tx = new TransferTx(seed, fromWallet, toWallet, sum);
        BinaryMessage message = tx.getMessage();
        TransferTx txFromMessage = TransferTx.converter().fromMessage(message);

        assertThat(txFromMessage, equalTo(tx));
    }

    @Test
    public void info() {
        // Create a transaction with the given parameters.
//        long seed = Long.MAX_VALUE - 1;
        long seed = 1L;
        String name = "new_wallet";
        HashCode nameHash = Hashing.defaultHashFunction()
                .hashString(name, UTF_8);
        TransferTx tx = new TransferTx(seed, nameHash, nameHash, 50L);

        String info = tx.info();

        // Check the transaction parameters in JSON
        Gson gson = CryptocurrencyTransactionGson.instance();

        AnyTransaction<TransferTx> txParameters = gson.fromJson(info,
                new TypeToken<AnyTransaction<TransferTx>>(){}.getType());

        assertThat(txParameters.getBody(), equalTo(tx));
    }

    private static Message.Builder messageBuilder() {
        return new Message.Builder()
                .mergeFrom(TX_MESSAGE_TEMPLATE);
    }

    private void createWallet(Fork view, String name, Long initialValue) {
        HashCode nameHash = Hashing.defaultHashFunction().hashString(name, UTF_8);
        CryptocurrencySchema schema = new CryptocurrencySchema(view);
        try (MapIndex<HashCode, Wallet> wallets = schema.wallets()) {
            wallets.put(nameHash, new Wallet(name, initialValue));
        }
    }
}
