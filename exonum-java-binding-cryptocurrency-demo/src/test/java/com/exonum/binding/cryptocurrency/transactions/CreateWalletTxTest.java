package com.exonum.binding.cryptocurrency.transactions;

import static com.exonum.binding.cryptocurrency.transactions.CreateWalletTx.DEFAULT_BALANCE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.crypto.CryptoFunction;
import com.exonum.binding.crypto.CryptoFunctions;
import com.exonum.binding.crypto.PublicKey;
import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.cryptocurrency.CryptocurrencyService;
import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
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

  private CryptoFunction cryptoFunction = CryptoFunctions.ed25519();

  @Rule public final ExpectedException expectedException = ExpectedException.none();

  @Test
  public void isValidNonEmptyName() {
    PublicKey publicKey = cryptoFunction.generateKeyPair().getPublicKey();

    CreateWalletTx tx = new CreateWalletTx(publicKey);

    assertTrue(tx.isValid());
  }

  @Test
  public void isValidEmptyName() {
    PublicKey publicKey = PublicKey.fromBytes(new byte[0]);

    expectedException.expectMessage("Public key must not empty");
    expectedException.expect(IllegalArgumentException.class);
    new CreateWalletTx(publicKey);
  }

  @Test
  public void executeCreateWalletTx() throws CloseFailuresException {
    PublicKey publicKey = cryptoFunction.generateKeyPair().getPublicKey();

    CreateWalletTx tx = new CreateWalletTx(publicKey);

    try (Database db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);
      tx.execute(view);

      // Check that entries have been added.
      CryptocurrencySchema schema = new CryptocurrencySchema(view);
      MapIndex<PublicKey, Wallet> wallets = schema.wallets();
      PublicKey walletId = PublicKey.fromBytes(publicKey.toBytes());
      
      assertThat(wallets.get(walletId).getPublicKey(), equalTo(publicKey));
      assertThat(wallets.get(walletId).getBalance(), equalTo(DEFAULT_BALANCE));
    }
  }

  @Test
  public void executeAlreadyExistingWalletTx() throws CloseFailuresException {
    try (Database db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);
      PublicKey publicKey = cryptoFunction.generateKeyPair().getPublicKey();
      Long value = DEFAULT_BALANCE;

      // Create a wallet manually.
      CryptocurrencySchema schema = new CryptocurrencySchema(view);
      {
        MapIndex<PublicKey, Wallet> wallets = schema.wallets();
        wallets.put(publicKey, new Wallet(publicKey, value));
      }

      // Execute the transaction, that has the same name.
      CreateWalletTx tx = new CreateWalletTx(publicKey);
      tx.execute(view);

      // Check it has not changed the entries in the maps.
      {
        MapIndex<PublicKey, Wallet> wallets = schema.wallets();
        assertThat(wallets.get(publicKey).getPublicKey(), equalTo(publicKey));
        assertThat(wallets.get(publicKey).getBalance(), equalTo(value));
      }
    }
  }

  @Test
  public void info() {
    PublicKey publicKey = cryptoFunction.generateKeyPair().getPublicKey();

    CreateWalletTx tx = new CreateWalletTx(publicKey);

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
