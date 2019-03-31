package com.exonum.binding.cryptocurrency.transactions;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.cryptocurrency.PredefinedOwnerKeys;
import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.transaction.TransactionContext;
import com.exonum.binding.util.LibraryLoader;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import static com.exonum.binding.cryptocurrency.transactions.ContextUtils.newContextBuilder;
import static com.exonum.binding.cryptocurrency.transactions.CreateTransferTransactionUtils.createWallet;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

@RequiresNativeLibrary
public class TransferMultisigHistoryTest {
  static {
    LibraryLoader.load();
  }

  private static final PublicKey ACCOUNT_1 = PredefinedOwnerKeys.FIRST_OWNER_KEY;
  private static final PublicKey ACCOUNT_2 = PredefinedOwnerKeys.SECOND_OWNER_KEY;
  private static final PublicKey SIGNER = PredefinedOwnerKeys.SIGNER_OWNER_KEY;

  @Test
  @RequiresNativeLibrary
  void transfersHistoryBetweenTwoAccountsTest() throws Exception {
    try (Database db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);

      // Create wallets with the given initial balances
      long initialBalance = 100L;
      long pendingInitialBalance = 0L;
      createWallet(view, ACCOUNT_1, initialBalance, pendingInitialBalance, SIGNER);
      createWallet(view, ACCOUNT_2, initialBalance, pendingInitialBalance, SIGNER);

      // Create and execute 1st transaction
      long seed1 = 1L;
      long transferSum1 = 40L;
      HashCode txMessageHash1 = HashCode.fromString("a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0");
      MultisignTransferTx tx1 = new MultisignTransferTx(seed1, ACCOUNT_2, transferSum1);
      TransactionContext context1 = newContextBuilder(view)
          .txMessageHash(txMessageHash1)
          .authorPk(ACCOUNT_1)
          .build();
      tx1.execute(context1);

      // Create and execute 2nd transaction
      long seed2 = 2L;
      long transferSum2 = 10L;
      HashCode txMessageHash2 = HashCode.fromString("b1b1b1b1b1b1b1b1b1b1b1b1b1b1b1b1b1b1b1b1b1b1b1b1b1b1b1b1b1b1b1b1");
      MultisignTransferTx tx2 = new MultisignTransferTx(seed2, ACCOUNT_1, transferSum2);
      TransactionContext context2 = newContextBuilder(view)
          .txMessageHash(txMessageHash2)
          .authorPk(ACCOUNT_2)
          .build();
      tx2.execute(context2);

      // Check that wallets have correct balances
      CryptocurrencySchema schema = new CryptocurrencySchema(view);
      ProofMapIndexProxy<PublicKey, Wallet> wallets = schema.wallets();
      long expectedBalance1 = initialBalance;
      assertThat(wallets.get(ACCOUNT_1).getBalance(), equalTo(expectedBalance1));
      long expectedBalance2 = initialBalance;
      assertThat(wallets.get(ACCOUNT_2).getBalance(), equalTo(expectedBalance2));

      // Check history
      Matcher<Iterable<?>> containsExpectedEntries = contains(txMessageHash1, txMessageHash2);
      assertThat(schema.transactionsHistory(ACCOUNT_1), containsExpectedEntries);
      assertThat(schema.transactionsHistory(ACCOUNT_2), containsExpectedEntries);

      // Create and execute 3st transaction
      long seed3 = 3L;
      HashCode txMessageHash3 = HashCode.fromString("c1c1c1c1c1c1c1c1c1c1c1c1c1c1c1c1c1c1c1c1c1c1c1c1c1c1c1c1c1c1c1c1");
      SignMultisignTransferTx tx3 = new SignMultisignTransferTx(seed3, txMessageHash1);
      TransactionContext context3 = newContextBuilder(view)
          .txMessageHash(txMessageHash3)
          .authorPk(SIGNER)
          .build();
      tx3.execute(context3);

      // Create and execute 2nd transaction
      long seed4 = 4L;
      HashCode txMessageHash4 = HashCode.fromString("d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1");
      SignMultisignTransferTx tx4 = new SignMultisignTransferTx(seed4, txMessageHash2);
      TransactionContext context4 = newContextBuilder(view)
          .txMessageHash(txMessageHash4)
          .authorPk(SIGNER)
          .build();
      tx4.execute(context4);

      // Check that wallets have correct balances
      wallets = schema.wallets();
      long expectedBalance3 = initialBalance - transferSum1 + transferSum2;
      assertThat(wallets.get(ACCOUNT_1).getBalance(), equalTo(expectedBalance3));
      long expectedBalance4 = initialBalance + transferSum1 - transferSum2;
      assertThat(wallets.get(ACCOUNT_2).getBalance(), equalTo(expectedBalance4));

      // Check history
      Matcher<Iterable<?>> containsExpectedEntries1 =
          contains(txMessageHash1, txMessageHash2, txMessageHash3, txMessageHash4);
      assertThat(schema.transactionsHistory(ACCOUNT_1), containsExpectedEntries1);
      assertThat(schema.transactionsHistory(ACCOUNT_2), containsExpectedEntries1);

    }
  }
}
