/*
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.cryptocurrency.transactions;

import static com.exonum.binding.common.serialization.json.JsonSerializer.json;
import static com.exonum.binding.cryptocurrency.transactions.ContextUtils.newContextBuilder;
import static com.exonum.binding.cryptocurrency.transactions.CreateWalletTransactionUtils.DEFAULT_INITIAL_BALANCE;
import static com.exonum.binding.cryptocurrency.transactions.CreateWalletTransactionUtils.DEFAULT_INITIAL_PENDING_BALANCE;
import static com.exonum.binding.cryptocurrency.transactions.CreateWalletTransactionUtils.DEFAULT_INITIAL_SIGNER;
import static com.exonum.binding.cryptocurrency.transactions.CreateWalletTransactionUtils.createRawTransaction;
import static com.exonum.binding.cryptocurrency.transactions.TransactionError.WALLET_ALREADY_EXISTS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.cryptocurrency.PredefinedOwnerKeys;
import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.transaction.TransactionContext;
import com.exonum.binding.transaction.TransactionExecutionException;
import com.exonum.binding.util.LibraryLoader;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class CreateWalletTxTest {

  static {
    LibraryLoader.load();
  }

  private static final PublicKey OWNER_KEY = PredefinedOwnerKeys.FIRST_OWNER_KEY;

  @Test
  void fromRawTransaction() {
    long initialBalance = 100L;
    RawTransaction raw = createRawTransaction(initialBalance, DEFAULT_INITIAL_SIGNER);

    CreateWalletTx tx = CreateWalletTx.fromRawTransaction(raw);

    assertThat(tx, equalTo(new CreateWalletTx(initialBalance, DEFAULT_INITIAL_SIGNER)));
  }

  @Test
  void constructorRejectsNegativeBalance() {
    long initialBalance = -1L;

    Throwable t = assertThrows(IllegalArgumentException.class,
        () -> new CreateWalletTx(initialBalance, DEFAULT_INITIAL_SIGNER));

    assertThat(t.getMessage(), equalTo("The initial balance (-1) must not be negative."));
  }

  @Test
  @RequiresNativeLibrary
  void executeCreateWalletTx() throws Exception {
    CreateWalletTx tx = new CreateWalletTx(DEFAULT_INITIAL_BALANCE, DEFAULT_INITIAL_SIGNER);

    try (Database db = MemoryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);

      // Execute the transaction
      TransactionContext context = newContextBuilder(view)
          .authorPk(OWNER_KEY)
          .build();
      tx.execute(context);

      // Check that entries have been added.
      CryptocurrencySchema schema = new CryptocurrencySchema(view);
      MapIndex<PublicKey, Wallet> wallets = schema.wallets();

      assertTrue(wallets.containsKey(OWNER_KEY));
      assertThat(wallets.get(OWNER_KEY).getBalance(), equalTo(DEFAULT_INITIAL_BALANCE));
    }
  }

  @Test
  @RequiresNativeLibrary
  void executeAlreadyExistingWalletTx() throws CloseFailuresException {
    try (Database db = MemoryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);

      Long initialBalance = DEFAULT_INITIAL_BALANCE;
      Long pendingInitialBalance = DEFAULT_INITIAL_PENDING_BALANCE;
      PublicKey signer = DEFAULT_INITIAL_SIGNER;

      // Create a wallet manually.
      CryptocurrencySchema schema = new CryptocurrencySchema(view);
      {
        MapIndex<PublicKey, Wallet> wallets = schema.wallets();
        wallets.put(OWNER_KEY, new Wallet(initialBalance, pendingInitialBalance, signer));
      }

      // Execute the transaction, that has the same owner key.
      // Use twice the initial balance to detect invalid updates.
      long newBalance = 2 * initialBalance;
      CreateWalletTx tx = new CreateWalletTx(newBalance, signer);
      TransactionContext context = newContextBuilder(view)
          .authorPk(OWNER_KEY)
          .build();
      TransactionExecutionException e = assertThrows(
          TransactionExecutionException.class, () -> tx.execute(context));

      assertThat(e.getErrorCode(), equalTo(WALLET_ALREADY_EXISTS.errorCode));
    }
  }

  @Test
  void info() {
    CreateWalletTx tx = new CreateWalletTx(DEFAULT_INITIAL_BALANCE, DEFAULT_INITIAL_SIGNER);

    String info = tx.info();

    CreateWalletTx txParams = json()
        .fromJson(info, CreateWalletTx.class);

    assertThat(txParams, equalTo(tx));
  }

  @Test
  void verifyEquals() {
    EqualsVerifier
        .forClass(CreateWalletTx.class)
        .verify();
  }

}
