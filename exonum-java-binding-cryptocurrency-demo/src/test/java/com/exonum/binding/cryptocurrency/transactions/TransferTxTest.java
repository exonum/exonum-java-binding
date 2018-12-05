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

import static com.exonum.binding.cryptocurrency.transactions.CreateTransferTransactionUtils.createRawTransaction;
import static com.exonum.binding.cryptocurrency.transactions.CreateTransferTransactionUtils.createWallet;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.cryptocurrency.HistoryEntity;
import com.exonum.binding.cryptocurrency.PredefinedOwnerKeys;
import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.transaction.InternalTransactionContext;
import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.util.LibraryLoader;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class TransferTxTest {

  static {
    LibraryLoader.load();
  }

  private static final PublicKey fromKey = PredefinedOwnerKeys.firstOwnerKey;

  private static final PublicKey toKey = PredefinedOwnerKeys.secondOwnerKey;

  @Test
  void fromMessage() {
    long seed = 1;
    long amount = 50L;
    RawTransaction m = createRawTransaction(seed, fromKey, toKey, amount);

    TransferTx tx = TransferTx.fromMessage(m);

    assertThat(tx, equalTo(withMockMessage(seed, fromKey, toKey, amount)));
  }

  @Test
  @RequiresNativeLibrary
  void executeTransfer() throws CloseFailuresException {
    try (Database db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);
      // Create source and target wallets with the given initial balances
      long initialBalance = 100L;
      createWallet(view, fromKey, initialBalance);
      createWallet(view, toKey, initialBalance);

      // Create and execute the transaction
      long seed = 1L;
      long transferSum = 40L;
      TransferTx tx = withMockMessage(seed, fromKey, toKey, transferSum);
      InternalTransactionContext context = new InternalTransactionContext(view,null, null);

      tx.execute(context);

      // Check that wallets have correct balances
      CryptocurrencySchema schema = new CryptocurrencySchema(view);
      ProofMapIndexProxy<PublicKey, Wallet> wallets = schema.wallets();
      long expectedFromValue = initialBalance - transferSum;
      assertThat(wallets.get(fromKey).getBalance(), equalTo(expectedFromValue));
      long expectedToValue = initialBalance + transferSum;
      assertThat(wallets.get(toKey).getBalance(), equalTo(expectedToValue));
      // Check history
      HistoryEntity expectedEntity = HistoryEntity.Builder.newBuilder()
          .setSeed(seed)
          .setWalletFrom(fromKey)
          .setWalletTo(toKey)
          .setAmount(transferSum)
          .setTransactionHash(tx.hash())
          .build();
      assertThat(schema.walletHistory(fromKey), hasItem(expectedEntity));
      assertThat(schema.walletHistory(toKey), hasItem(expectedEntity));
    }
  }

  @Test
  @RequiresNativeLibrary
  void executeTransferToTheSameWallet() throws CloseFailuresException {
    try (Database db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);

      long initialBalance = 100L;
      createWallet(view, fromKey, initialBalance);

      // Create and execute the transaction
      long seed = 1L;
      long transferSum = 40L;
      TransferTx tx = withMockMessage(seed, fromKey, fromKey, transferSum);
      InternalTransactionContext context = new InternalTransactionContext(view,null, null);

      tx.execute(context);

      // Check that the balance of the wallet remains the same
      CryptocurrencySchema schema = new CryptocurrencySchema(view);
      ProofMapIndexProxy<PublicKey, Wallet> wallets = schema.wallets();
      assertThat(wallets.get(fromKey).getBalance(), equalTo(initialBalance));
    }
  }

  @Test
  @RequiresNativeLibrary
  void executeNoSuchFromWallet() throws CloseFailuresException {
    try (Database db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);
      // Create source wallet with the given initial balance
      long initialBalance = 50L;
      createWallet(view, fromKey, initialBalance);

      long seed = 1L;

      long transferValue = 50L;
      TransferTx tx = withMockMessage(seed, fromKey, toKey, transferValue);
      InternalTransactionContext context = new InternalTransactionContext(view,null, null);

      // Execute the transaction that attempts to transfer to an unknown wallet
      tx.execute(context);

      // Check that balance of fromKey is unchanged
      CryptocurrencySchema schema = new CryptocurrencySchema(view);
      MapIndex<PublicKey, Wallet> wallets = schema.wallets();
      assertThat(wallets.get(fromKey).getBalance(), equalTo(initialBalance));
    }
  }

  @Test
  @RequiresNativeLibrary
  void executeNoSuchToWallet() throws CloseFailuresException {
    try (Database db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);
      // Create and execute the transaction that attempts to transfer from unknown wallet
      long initialBalance = 100L;
      createWallet(view, toKey, initialBalance);
      long transferValue = 50L;
      long seed = 1L;
      TransferTx tx = withMockMessage(seed, fromKey, toKey, transferValue);
      InternalTransactionContext context = new InternalTransactionContext(view,null, null);

      tx.execute(context);

      // Check that balance of toKey is unchanged
      CryptocurrencySchema schema = new CryptocurrencySchema(view);
      MapIndex<PublicKey, Wallet> wallets = schema.wallets();
      assertThat(wallets.get(toKey).getBalance(), equalTo(initialBalance));
    }
  }

  @Test
  void verifyEquals() {
    EqualsVerifier
        .forClass(TransferTx.class)
        .withPrefabValues(HashCode.class, HashCode.fromInt(1), HashCode.fromInt(2))
        .verify();
  }

  private TransferTx withMockMessage(long seed, PublicKey senderId, PublicKey recipientId,
      long amount) {
    // If a normal raw transaction object is ever needed, take the code from the 'fromMessage' test
    // and put it here, replacing `mock(RawTransaction.class)`.
    RawTransaction rawTransaction = mock(RawTransaction.class);
    lenient().when(rawTransaction.hash()).thenReturn(HashCode.fromString("a0a0a0a0"));
    return new TransferTx(rawTransaction, seed, senderId, recipientId, amount);
  }
}
