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

import static com.exonum.binding.cryptocurrency.transactions.CreateWalletTransactionUtils.DEFAULT_BALANCE;
import static com.exonum.binding.cryptocurrency.transactions.CreateWalletTransactionUtils.createRawTransaction;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

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
import com.exonum.binding.transaction.InternalTransactionContext;
import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.util.LibraryLoader;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class CreateWalletTxTest {

  static {
    LibraryLoader.load();
  }

  private static final PublicKey OWNER_KEY = PredefinedOwnerKeys.firstOwnerKey;

  @Test
  void fromMessage() {
    long initialBalance = 100L;
    RawTransaction m = createRawTransaction(OWNER_KEY, initialBalance);

    CreateWalletTx tx = CreateWalletTx.fromMessage(m);

    assertThat(tx, equalTo(withMockMessage(OWNER_KEY, initialBalance)));
  }

  @Test
  void constructorRejectsInvalidSizedKey() {
    PublicKey publicKey = PublicKey.fromBytes(new byte[1]);

    Throwable t = assertThrows(IllegalArgumentException.class,
        () -> withMockMessage(publicKey, DEFAULT_BALANCE)
    );
    assertThat(t.getMessage(),
        equalTo("Public key has invalid size (1), must be 32 bytes long."));
  }

  @Test
  void constructorRejectsNegativeBalance() {
    long initialBalance = -1L;

    Throwable t = assertThrows(IllegalArgumentException.class,
        () -> withMockMessage(OWNER_KEY, initialBalance)
    );
    assertThat(t.getMessage(), equalTo("The initial balance (-1) must not be negative."));

  }

  @Test
  @RequiresNativeLibrary
  void executeCreateWalletTx() throws CloseFailuresException {
    CreateWalletTx tx = withMockMessage(OWNER_KEY, DEFAULT_BALANCE);

    try (Database db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);
      InternalTransactionContext context = new InternalTransactionContext(view, null, OWNER_KEY);

      tx.execute(context);

      // Check that entries have been added.
      CryptocurrencySchema schema = new CryptocurrencySchema(view);
      MapIndex<PublicKey, Wallet> wallets = schema.wallets();

      assertTrue(wallets.containsKey(OWNER_KEY));
      assertThat(wallets.get(OWNER_KEY).getBalance(), equalTo(DEFAULT_BALANCE));
    }
  }

  @Test
  @RequiresNativeLibrary
  void executeAlreadyExistingWalletTx() throws CloseFailuresException {
    try (Database db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);
      Long initialBalance = DEFAULT_BALANCE;

      // Create a wallet manually.
      CryptocurrencySchema schema = new CryptocurrencySchema(view);
      {
        MapIndex<PublicKey, Wallet> wallets = schema.wallets();
        wallets.put(OWNER_KEY, new Wallet(initialBalance));
      }

      // Execute the transaction, that has the same owner key.
      // Use twice the initial balance to detect invalid updates.
      long newBalance = 2 * initialBalance;
      CreateWalletTx tx = withMockMessage(OWNER_KEY, newBalance);
      InternalTransactionContext context = new InternalTransactionContext(view, null, OWNER_KEY);

      tx.execute(context);

      // Check it has not changed the entries in the maps.
      {
        MapIndex<PublicKey, Wallet> wallets = schema.wallets();
        assertTrue(wallets.containsKey(OWNER_KEY));
        assertThat(wallets.get(OWNER_KEY).getBalance(), equalTo(initialBalance));
      }
    }
  }

  @Test
  void verifyEquals() {
    EqualsVerifier
        .forClass(CreateWalletTx.class)
        .verify();
  }

  private static CreateWalletTx withMockMessage(PublicKey ownerKey, long initialBalance) {
    // If a normal raw transaction object is ever needed, take the code from the 'fromMessage' test
    // and put it here, replacing `mock(RawTransaction.class)`.
    return new CreateWalletTx(mock(RawTransaction.class), ownerKey, initialBalance);
  }
}
