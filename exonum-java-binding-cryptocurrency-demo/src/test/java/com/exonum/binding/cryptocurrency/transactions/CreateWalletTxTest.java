package com.exonum.binding.cryptocurrency.transactions;

import static com.exonum.binding.cryptocurrency.HashUtils.hashUtf8String;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.cryptocurrency.CryptocurrencyService;
import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.hash.HashCode;
import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.util.LibraryLoader;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class CreateWalletTxTest {

  static {
    LibraryLoader.load();
  }

  @Rule public final ExpectedException expectedException = ExpectedException.none();

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
  public void executeCreateWalletTx() {
    String name = "wallet";

    CreateWalletTx tx = new CreateWalletTx(name);

    try (Database db = new MemoryDb();
        Fork view = db.createFork()) {
      tx.execute(view);

      // Check that entries have been added.
      CryptocurrencySchema schema = new CryptocurrencySchema(view);
      try (MapIndex<HashCode, Wallet> wallets = schema.wallets()) {

        HashCode nameHash = hashUtf8String(name);

        assertThat(wallets.get(nameHash).getName(), equalTo(name));
        assertThat(wallets.get(nameHash).getBalance(), equalTo(0L));
      }
    }
  }

  @Test
  public void executeAlreadyExistingWalletTx() {
    try (Database db = new MemoryDb();
        Fork view = db.createFork()) {
      String name = "wallet";
      Long value = 100L;
      HashCode nameHash = hashUtf8String(name);

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

    BaseTx txParams = CryptocurrencyTransactionGson.instance().fromJson(info, BaseTx.class);
    assertThat(txParams.getServiceId(), equalTo(CryptocurrencyService.ID));
    assertThat(txParams.getMessageId(), equalTo(CryptocurrencyTransaction.CREATE_WALLET.getId()));
  }

  @Test
  public void verifyEquals() {
    EqualsVerifier
        .forClass(CreateWalletTx.class)
        .verify();
  }
}
