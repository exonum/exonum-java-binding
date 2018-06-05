package com.exonum.binding.cryptocurrency.transactions;

import static org.abstractj.kalium.NaCl.Sodium.CRYPTO_SIGN_ED25519_PUBLICKEYBYTES;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.crypto.PublicKey;
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
import com.exonum.binding.test.Bytes;
import com.exonum.binding.util.LibraryLoader;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class TransferTxTest {

  static {
    LibraryLoader.load();
  }

  private static final PublicKey fromOwnerKey =
      PublicKey.fromBytes(Bytes.createPrefixed(Bytes.bytes(0), CRYPTO_SIGN_ED25519_PUBLICKEYBYTES));

  private static final PublicKey toOwnerKey =
      PublicKey.fromBytes(Bytes.createPrefixed(Bytes.bytes(1), CRYPTO_SIGN_ED25519_PUBLICKEYBYTES));

  @Test
  public void isValid() {
    long seed = 1L;
    long sum = 50L;

    TransferTx tx = new TransferTx(seed, fromOwnerKey, toOwnerKey, sum);

    assertTrue(tx.isValid());
  }

  @Test
  public void executeTransfer() throws CloseFailuresException {
    try (Database db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);
      // Create source and target wallets with the given initial values
      long initialValue = 100L;
      createWallet(view, fromOwnerKey, initialValue);
      createWallet(view, toOwnerKey, initialValue);

      // Create and execute the transaction
      long seed = 1L;
      long transferSum = 40L;
      TransferTx tx = new TransferTx(seed, fromOwnerKey, toOwnerKey, transferSum);
      tx.execute(view);

      // Check that wallets have correct values
      CryptocurrencySchema schema = new CryptocurrencySchema(view);
      ProofMapIndexProxy<PublicKey, Wallet> wallets = schema.wallets();
      long expectedFromValue = initialValue - transferSum;
      assertThat(wallets.get(fromOwnerKey).getBalance(), equalTo(expectedFromValue));
      long expectedToValue = initialValue + transferSum;
      assertThat(wallets.get(toOwnerKey).getBalance(), equalTo(expectedToValue));
    }
  }

  @Test
  public void executeNoSuchFromWallet() throws CloseFailuresException {
    try (Database db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);
      // Create source wallet with the given initial value
      long initialValue = 50L;
      createWallet(view, fromOwnerKey, initialValue);

      long seed = 1L;
      long transferValue = 50L;
      TransferTx tx = new TransferTx(seed, fromOwnerKey, toOwnerKey, transferValue);
      // Execute the transaction that attempts to transfer to an unknown wallet
      tx.execute(view);

      // Check that balance of fromOwnerKey is unchanged
      CryptocurrencySchema schema = new CryptocurrencySchema(view);
      MapIndex<PublicKey, Wallet> wallets = schema.wallets();
      assertThat(wallets.get(fromOwnerKey).getBalance(), equalTo(initialValue));
    }
  }

  @Test
  public void executeNoSuchToWallet() throws CloseFailuresException {
    try (Database db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);
      // Create and execute the transaction that attempts to transfer from unknown wallet
      long initialValue = 100L;
      createWallet(view, toOwnerKey, initialValue);
      long transferValue = 50L;
      long seed = 1L;
      TransferTx tx = new TransferTx(seed, fromOwnerKey, toOwnerKey, transferValue);
      tx.execute(view);

      // Check that balance of toOwnerKey is unchanged
      CryptocurrencySchema schema = new CryptocurrencySchema(view);
      MapIndex<PublicKey, Wallet> wallets = schema.wallets();
      assertThat(wallets.get(toOwnerKey).getBalance(), equalTo(initialValue));
    }
  }

  @Test
  public void converterRoundtrip() {
    long seed = 0L;
    long sum = 50L;

    TransferTx tx = new TransferTx(seed, fromOwnerKey, toOwnerKey, sum);
    BinaryMessage message = tx.getMessage();
    TransferTx txFromMessage = TransferTx.converter().fromMessage(message);

    assertThat(txFromMessage, equalTo(tx));
  }

  @Test
  public void info() {
    long seed = Long.MAX_VALUE - 1L;
    TransferTx tx = new TransferTx(seed, fromOwnerKey, toOwnerKey, 50L);

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

  private void createWallet(Fork view, PublicKey publicKey, Long initialValue) {
    CryptocurrencySchema schema = new CryptocurrencySchema(view);
    MapIndex<PublicKey, Wallet> wallets = schema.wallets();
    wallets.put(publicKey, new Wallet(initialValue));
  }
}
