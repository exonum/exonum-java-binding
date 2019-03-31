package com.exonum.binding.cryptocurrency.transactions;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.cryptocurrency.PredefinedOwnerKeys;
import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.transaction.Transaction;
import com.exonum.binding.transaction.TransactionContext;
import com.exonum.binding.transaction.TransactionExecutionException;
import com.exonum.binding.util.LibraryLoader;
import com.google.common.reflect.TypeToken;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.exonum.binding.common.serialization.json.JsonSerializer.json;
import static com.exonum.binding.cryptocurrency.transactions.ContextUtils.newContextBuilder;
import static com.exonum.binding.cryptocurrency.transactions.CreateMultisignTransferTransactionUtils.createRawTransaction;
import static com.exonum.binding.cryptocurrency.transactions.CreateMultisignTransferTransactionUtils.createWallet;
import static com.exonum.binding.cryptocurrency.transactions.TransactionError.INSUFFICIENT_FUNDS;
import static com.exonum.binding.cryptocurrency.transactions.TransactionError.SAME_SIGNER_AND_TX_SENDER;
import static com.exonum.binding.cryptocurrency.transactions.TransactionError.SIGNER_NOT_EQUALS_TO_SENDER;
import static com.exonum.binding.cryptocurrency.transactions.TransferTxTest.hasErrorCode;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.containsStringIgnoringCase;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MultisignTransferTxTest {

  static {
    LibraryLoader.load();
  }

  private static final PublicKey FROM_KEY = PredefinedOwnerKeys.FIRST_OWNER_KEY;
  private static final PublicKey TO_KEY = PredefinedOwnerKeys.SECOND_OWNER_KEY;
  private static final PublicKey SIGNER_KEY = PredefinedOwnerKeys.SIGNER_OWNER_KEY;

  @Test
  void fromRawTransaction() {
    long seed = 1;
    long amount = 50L;
    RawTransaction raw = createRawTransaction(seed, TO_KEY, amount);

    MultisignTransferTx tx = MultisignTransferTx.fromRawTransaction(raw);

    assertThat(tx, equalTo(new MultisignTransferTx(seed, TO_KEY, amount)));
  }

  @ParameterizedTest
  @ValueSource(longs = {
      Long.MIN_VALUE,
      -100,
      -1,
      0
  })
  void fromRawTransactionRejectsNonPositiveBalance(long transferAmount) {
    long seed = 1;
    RawTransaction tx =
      CreateMultisignTransferTransactionUtils.createRawTransaction(seed, TO_KEY, transferAmount);

    Exception e = assertThrows(IllegalArgumentException.class,
        () -> MultisignTransferTx.fromRawTransaction(tx));

    assertThat(e.getMessage(), allOf(
        containsStringIgnoringCase("transfer amount"),
        containsString(Long.toString(transferAmount))));
  }

  @Test
  @RequiresNativeLibrary
  void executeTransfer() throws Exception {
    try (Database db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);

      long initialBalance = 100L;
      long pendingInitialBalance = 0L;

      createWallet(view, FROM_KEY, initialBalance, pendingInitialBalance, SIGNER_KEY);
      createWallet(view, TO_KEY, initialBalance, pendingInitialBalance, SIGNER_KEY);
      createWallet(view, SIGNER_KEY, initialBalance, pendingInitialBalance, FROM_KEY);

      long seed = 1L;
      long transferSum = 40L;
      HashCode hash = HashCode
        .fromString("a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0");
      MultisignTransferTx tx = new MultisignTransferTx(seed, TO_KEY, transferSum);
      TransactionContext context = newContextBuilder(view)
          .txMessageHash(hash)
          .authorPk(FROM_KEY)
          .build();
      tx.execute(context);

      CryptocurrencySchema schema = new CryptocurrencySchema(view);
      ProofMapIndexProxy<PublicKey, Wallet> wallets = schema.wallets();
      long exceptedFromValue = initialBalance;
      long exceptedPendingFromValue = pendingInitialBalance - transferSum;
      assertThat(wallets.get(FROM_KEY).getBalance(), equalTo(exceptedFromValue));
      assertThat(wallets.get(FROM_KEY).getPendingBalance(), equalTo(exceptedPendingFromValue));
      long expectedToValue = initialBalance;
      long exceptedPendingToValue = pendingInitialBalance;
      assertThat(wallets.get(TO_KEY).getBalance(), equalTo(expectedToValue));
      assertThat(wallets.get(TO_KEY).getPendingBalance(), equalTo(exceptedPendingToValue));

      // Check history
      assertThat(schema.transactionsHistory(FROM_KEY), hasItem(hash));
      assertThat(schema.transactionsHistory(TO_KEY), hasItem(hash));

      long seed1 = 2L;
      HashCode hash1 = HashCode
        .fromString("0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a");
      SignMultisignTransferTx tx1 = new SignMultisignTransferTx(seed1, hash);
      TransactionContext context1 = newContextBuilder(view)
          .txMessageHash(hash1)
          .authorPk(SIGNER_KEY)
          .build();
      tx1.execute(context1);

      long exceptedFromValue1 = initialBalance - transferSum;
      long exceptedPendingFromValue1 = pendingInitialBalance;
      assertThat(wallets.get(FROM_KEY).getBalance(), equalTo(exceptedFromValue1));
      assertThat(wallets.get(FROM_KEY).getPendingBalance(), equalTo(exceptedPendingFromValue1));
      long expectedToValue1 = initialBalance + transferSum;
      long exceptedPendingToValue1 = pendingInitialBalance;
      assertThat(wallets.get(TO_KEY).getBalance(), equalTo(expectedToValue1));
      assertThat(wallets.get(TO_KEY).getPendingBalance(), equalTo(exceptedPendingToValue1));

      // Check history
      assertThat(schema.transactionsHistory(FROM_KEY), hasItem(hash1));
      assertThat(schema.transactionsHistory(TO_KEY), hasItem(hash1));
      assertThat(schema.transactionsHistory(SIGNER_KEY), hasItem(hash1));

    }
  }

  @Test
  @RequiresNativeLibrary
  void executeTransfer_sameSignerAndSender() throws Exception {
    try (Database db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);

      long initialBalance = 100L;
      long pendingInitialBalance = 0L;

      createWallet(view, FROM_KEY, initialBalance, pendingInitialBalance, SIGNER_KEY);
      createWallet(view, TO_KEY, initialBalance, pendingInitialBalance, SIGNER_KEY);
      createWallet(view, SIGNER_KEY, initialBalance, pendingInitialBalance, FROM_KEY);

      long seed = 1L;
      long transferSum = 40L;
      HashCode hash =
        HashCode.fromString("a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0");
      MultisignTransferTx tx = new MultisignTransferTx(seed, TO_KEY, transferSum);
      TransactionContext context = newContextBuilder(view)
          .txMessageHash(hash)
          .authorPk(FROM_KEY)
          .build();
      tx.execute(context);

      CryptocurrencySchema schema = new CryptocurrencySchema(view);
      ProofMapIndexProxy<PublicKey, Wallet> wallets = schema.wallets();
      long exceptedFromValue = initialBalance;
      long exceptedPendingFromValue = pendingInitialBalance - transferSum;
      assertThat(wallets.get(FROM_KEY).getBalance(), equalTo(exceptedFromValue));
      assertThat(wallets.get(FROM_KEY).getPendingBalance(), equalTo(exceptedPendingFromValue));
      long expectedToValue = initialBalance;
      long exceptedPendingToValue = pendingInitialBalance;
      assertThat(wallets.get(TO_KEY).getBalance(), equalTo(expectedToValue));
      assertThat(wallets.get(TO_KEY).getPendingBalance(), equalTo(exceptedPendingToValue));

      // Check history
      assertThat(schema.transactionsHistory(FROM_KEY), hasItem(hash));
      assertThat(schema.transactionsHistory(TO_KEY), hasItem(hash));

      long seed1 = 2L;
      HashCode hash1 =
        HashCode.fromString("0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a");
      SignMultisignTransferTx tx1 = new SignMultisignTransferTx(seed1, hash);
      TransactionContext context1 = newContextBuilder(view)
          .txMessageHash(hash1)
          .authorPk(FROM_KEY)
          .build();

      TransactionExecutionException e = assertThrows(
          TransactionExecutionException.class, () -> tx1.execute(context1));
      assertThat(e, hasErrorCode(SAME_SIGNER_AND_TX_SENDER));
    }
  }

  @Test
  @RequiresNativeLibrary
  void executeTransfer_wrongSenderForSigning() throws Exception {
    try (Database db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);

      long initialBalance = 100L;
      long pendingInitialBalance = 0L;

      createWallet(view, FROM_KEY, initialBalance, pendingInitialBalance, SIGNER_KEY);
      createWallet(view, TO_KEY, initialBalance, pendingInitialBalance, SIGNER_KEY);
      createWallet(view, SIGNER_KEY, initialBalance, pendingInitialBalance, FROM_KEY);

      long seed = 1L;
      long transferSum = 40L;
      HashCode hash =
        HashCode.fromString("a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0");
      MultisignTransferTx tx = new MultisignTransferTx(seed, TO_KEY, transferSum);
      TransactionContext context = newContextBuilder(view)
          .txMessageHash(hash)
          .authorPk(FROM_KEY)
          .build();
      tx.execute(context);

      CryptocurrencySchema schema = new CryptocurrencySchema(view);
      ProofMapIndexProxy<PublicKey, Wallet> wallets = schema.wallets();
      long exceptedFromValue = initialBalance;
      long exceptedPendingFromValue = pendingInitialBalance - transferSum;
      assertThat(wallets.get(FROM_KEY).getBalance(), equalTo(exceptedFromValue));
      assertThat(wallets.get(FROM_KEY).getPendingBalance(), equalTo(exceptedPendingFromValue));
      long expectedToValue = initialBalance;
      long exceptedPendingToValue = pendingInitialBalance;
      assertThat(wallets.get(TO_KEY).getBalance(), equalTo(expectedToValue));
      assertThat(wallets.get(TO_KEY).getPendingBalance(), equalTo(exceptedPendingToValue));

      // Check history
      assertThat(schema.transactionsHistory(FROM_KEY), hasItem(hash));
      assertThat(schema.transactionsHistory(TO_KEY), hasItem(hash));

      long seed1 = 2L;
      HashCode hash1 =
        HashCode.fromString("0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a");
      SignMultisignTransferTx tx1 = new SignMultisignTransferTx(seed1, hash);
      TransactionContext context1 = newContextBuilder(view)
          .txMessageHash(hash1)
          .authorPk(PredefinedOwnerKeys.THIRD_OWNER_KEY)
          .build();

      TransactionExecutionException e = assertThrows(
          TransactionExecutionException.class, () -> tx1.execute(context1));
      assertThat(e, hasErrorCode(SIGNER_NOT_EQUALS_TO_SENDER));
    }
  }

  @Test
  @RequiresNativeLibrary
  void executeTransfer_InsufficientFundsPending() throws CloseFailuresException {
    try (Database db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);

      // Create source and target wallets with the given initial balances
      long initialBalance = 100L;
      long pendingInitialBalance = -150L;
      PublicKey signer = PublicKey.fromHexString("abcd");

      CreateTransferTransactionUtils
        .createWallet(view, FROM_KEY, initialBalance, pendingInitialBalance, signer);
      CreateTransferTransactionUtils
        .createWallet(view, TO_KEY, initialBalance, pendingInitialBalance, signer);

      // Create and execute the transaction that attempts to transfer an amount
      // exceeding the balance
      long seed = 1L;
      long transferValue = initialBalance;
      MultisignTransferTx tx = new MultisignTransferTx(seed, TO_KEY, transferValue);
      TransactionContext context = newContextBuilder(view)
          .authorPk(FROM_KEY)
          .build();

      TransactionExecutionException e = assertThrows(
          TransactionExecutionException.class, () -> tx.execute(context));
      assertThat(e, hasErrorCode(INSUFFICIENT_FUNDS));
    }
  }

  @Test
  void infoMultisignTransferTx() {
    long seed = Long.MAX_VALUE - 1L;
    MultisignTransferTx tx =  new MultisignTransferTx(seed, TO_KEY, 50L);

    String info = tx.info();

    // Check the transaction parameters in JSON
    Transaction txParameters = json().fromJson(info, new TypeToken<MultisignTransferTx>() {
    }.getType());

    assertThat(txParameters, CoreMatchers.equalTo(tx));
  }

  @Test
  void infoSignMultisignTransferTx() {
    long seed = Long.MAX_VALUE - 1L;
    HashCode hash = HashCode.fromString("a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0");
    SignMultisignTransferTx tx =  new SignMultisignTransferTx(seed, hash);

    String info = tx.info();

    // Check the transaction parameters in JSON
    Transaction txParameters = json().fromJson(info, new TypeToken<SignMultisignTransferTx>() {
    }.getType());

    assertThat(txParameters, CoreMatchers.equalTo(tx));
  }

  @Test
  void verifyEqualsMultisignTransferTx() {
    EqualsVerifier
        .forClass(MultisignTransferTx.class)
        .withPrefabValues(HashCode.class, HashCode.fromInt(1), HashCode.fromInt(2))
        .verify();
  }

  @Test
  void verifyEqualsSignMultisignTransferTx() {
    EqualsVerifier
        .forClass(SignMultisignTransferTx.class)
        .withPrefabValues(HashCode.class, HashCode.fromInt(1), HashCode.fromInt(2))
        .verify();
  }
}
