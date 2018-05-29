package com.exonum.binding.cryptocurrency.transactions;

import static com.exonum.binding.cryptocurrency.HashUtils.hashUtf8String;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.hash.HashCode;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.util.LibraryLoader;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class TransferTxTest {

  static {
    LibraryLoader.load();
  }

  @Test
  public void isValid() {
    long seed = 1L;
    HashCode fromWallet = hashUtf8String("from");
    HashCode toWallet = hashUtf8String("to");
    long sum = 50L;

    TransferTx tx = new TransferTx(seed, fromWallet, toWallet, sum);

    assertTrue(tx.isValid());
  }

  @Test
  public void executeTransfer() throws CloseFailuresException {
    try (Database db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);
      // Create source and target wallets with the given initial values
      String from = "wallet-1";
      String to = "wallet-2";
      long initialValue = 100;
      createWallet(view, from, initialValue);
      createWallet(view, to, initialValue);

      // Create and execute the transaction
      long seed = 1;
      HashCode fromWallet = hashUtf8String(from);
      HashCode toWallet = hashUtf8String(to);
      long transferSum = 40;
      TransferTx tx = new TransferTx(seed, fromWallet, toWallet, transferSum);
      tx.execute(view);

      // Check that wallets have correct values
      CryptocurrencySchema schema = new CryptocurrencySchema(view);
      ProofMapIndexProxy<HashCode, Wallet> wallets = schema.wallets();
      long expectedFromValue = initialValue - transferSum;
      assertThat(wallets.get(fromWallet).getBalance(), equalTo(expectedFromValue));
      long expectedToValue = initialValue + transferSum;
      assertThat(wallets.get(toWallet).getBalance(), equalTo(expectedToValue));
    }
  }

  @Test
  public void executeNoSuchFromWallet() throws CloseFailuresException {
    try (Database db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);
      // Create source wallet with the given initial value
      String from = "from-wallet";
      String to = "unknown-wallet";
      long initialValue = 50L;
      createWallet(view, from, initialValue);

      long seed = 1L;
      HashCode fromWallet = hashUtf8String(from);
      HashCode toWallet = hashUtf8String(to);
      long transferValue = 50L;
      TransferTx tx = new TransferTx(seed, fromWallet, toWallet, transferValue);
      // Execute the transaction that attempts to transfer to an unknown wallet
      tx.execute(view);

      // Check that balance of fromWallet is unchanged
      CryptocurrencySchema schema = new CryptocurrencySchema(view);
      MapIndex<HashCode, Wallet> wallets = schema.wallets();
      assertThat(wallets.get(fromWallet).getBalance(), equalTo(initialValue));
    }
  }

  @Test
  public void executeNoSuchToWallet() throws CloseFailuresException {
    try (Database db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);
      // Create and execute the transaction that attempts to transfer from unknown wallet
      String from = "unknown-wallet";
      String to = "to-wallet";
      HashCode fromWallet = hashUtf8String(from);
      HashCode toWallet = hashUtf8String(to);
      long initialValue = 100L;
      createWallet(view, to, initialValue);
      long transferValue = 50L;
      long seed = 1L;
      TransferTx tx = new TransferTx(seed, fromWallet, toWallet, transferValue);
      tx.execute(view);

      // Check that balance of toWallet is unchanged
      CryptocurrencySchema schema = new CryptocurrencySchema(view);
      MapIndex<HashCode, Wallet> wallets = schema.wallets();
      assertThat(wallets.get(toWallet).getBalance(), equalTo(initialValue));
    }
  }

  @Test
  public void converterRoundtrip() {
    long seed = 0;
    String from = "from-wallet";
    String to = "to-wallet";
    HashCode fromWallet = hashUtf8String(from);
    HashCode toWallet = hashUtf8String(to);
    long sum = 50L;

    TransferTx tx = new TransferTx(seed, fromWallet, toWallet, sum);
    BinaryMessage message = tx.getMessage();
    TransferTx txFromMessage = TransferTx.converter().fromMessage(message);

    assertThat(txFromMessage, equalTo(tx));
  }

  @Test
  public void info() {
    long seed = Long.MAX_VALUE - 1;
    String name = "new_wallet";
    HashCode nameHash = hashUtf8String(name);
    TransferTx tx = new TransferTx(seed, nameHash, nameHash, 50L);

    String info = tx.info();

    // Check the transaction parameters in JSON
    Gson gson = CryptocurrencyTransactionGson.instance();

    Transaction txParameters = gson.fromJson(info, new TypeToken<TransferTx>() {}.getType());

    assertThat(txParameters, equalTo(tx));
  }

  @Test
  public void verifyEquals() {
    EqualsVerifier
        .forClass(TransferTx.class)
        .withPrefabValues(HashCode.class, HashCode.fromInt(1), HashCode.fromInt(2))
        .verify();
  }

  private void createWallet(Fork view, String name, Long initialValue) {
    HashCode nameHash = hashUtf8String(name);
    CryptocurrencySchema schema = new CryptocurrencySchema(view);
    MapIndex<HashCode, Wallet> wallets = schema.wallets();
    wallets.put(nameHash, new Wallet(name, initialValue));
  }
}
