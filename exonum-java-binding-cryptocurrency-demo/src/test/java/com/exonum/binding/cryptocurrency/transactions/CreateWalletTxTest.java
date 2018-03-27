package com.exonum.binding.cryptocurrency.transactions;

import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.cryptocurrency.CryptocurrencyService;
import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.Hashing;
import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.util.LibraryLoader;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class CreateWalletTxTest {

    static {
        LibraryLoader.load();
    }

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Test
    public void isValidNonEmptyName() {
        String name = "wallet";

        CreateWalletTx tx = new CreateWalletTx(name);

        assertTrue(tx.isValid());
    }

    @Test
    public void isValidEmptyName() {
        String name = "";

        expectedException.expectMessage("Name must not be blank");
        expectedException.expect(IllegalArgumentException.class);
        new CreateWalletTx(name);
    }

    @Test
    public void executeNewWallet() {
        String name = "wallet";

        CreateWalletTx tx = new CreateWalletTx(name);

        try (Database db = new MemoryDb();
             Fork view = db.createFork()) {
            tx.execute(view);

            // Check that entries have been added.
            CryptocurrencySchema schema = new CryptocurrencySchema(view);
            try (MapIndex<HashCode, Wallet> wallets = schema.wallets()) {

                HashCode nameHash = Hashing.defaultHashFunction()
                        .hashString(name, UTF_8);

                assertThat(wallets.get(nameHash).getName(), equalTo(name));
                assertThat(wallets.get(nameHash).getBalance(), equalTo(0L));
            }
        }
    }

    @Test
    public void executeAlreadyExistingWallet() {
        try (Database db = new MemoryDb();
             Fork view = db.createFork()) {
            String name = "wallet";
            Long value = 100L;
            HashCode nameHash = Hashing.defaultHashFunction()
                    .hashString(name, UTF_8);

            // Create a wallet manually.
            CryptocurrencySchema schema = new CryptocurrencySchema(view);
            try (MapIndex<HashCode, Wallet> wallets = schema.wallets()) {
                wallets.put(nameHash, new Wallet(name, value));
            }

            // Execute the transaction, that has the same name.
            CreateWalletTx tx = new CreateWalletTx(name);
            tx.execute(view);

            // Check it has not changed the entries in the maps.
            try (MapIndex<HashCode, Wallet> wallets = schema.wallets()) {
                assertThat(wallets.get(nameHash).getName(), equalTo(name));
                assertThat(wallets.get(nameHash).getBalance(), equalTo(value));
            }
        }
    }

    @Test
    public void info() {
        String name = "wallet";

        CreateWalletTx tx = new CreateWalletTx(name);

        String info = tx.info();

        Gson gson = new Gson();
        AnyTransaction txParams = gson.fromJson(info, AnyTransaction.class);
        assertThat(txParams.getService_id(), equalTo(CryptocurrencyService.ID));
        assertThat(txParams.getMessage_id(), equalTo(CryptocurrencyTransaction.CREATE_WALLET.getId()));
        assertThat(txParams.getBody(), equalTo(ImmutableMap.of("name", name)));
    }
}
