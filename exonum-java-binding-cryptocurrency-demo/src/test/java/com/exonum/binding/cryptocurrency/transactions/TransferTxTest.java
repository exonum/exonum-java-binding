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
import static com.exonum.binding.cryptocurrency.transactions.TransactionError.INSUFFICIENT_FUNDS;
import static com.exonum.binding.cryptocurrency.transactions.TransactionError.UNKNOWN_RECEIVER;
import static com.exonum.binding.cryptocurrency.transactions.TransactionError.UNKNOWN_SENDER;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.containsStringIgnoringCase;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.transaction.InternalTransactionContext;
import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.transaction.TransactionExecutionException;
import com.exonum.binding.util.LibraryLoader;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TransferTxTest {

  static {
    LibraryLoader.load();
  }

  private static final PublicKey FROM_KEY = PredefinedOwnerKeys.firstOwnerKey;
  private static final PublicKey TO_KEY = PredefinedOwnerKeys.secondOwnerKey;

  @Test
  void fromMessage() {
    long seed = 1;
    long amount = 50L;
    RawTransaction m = createRawTransaction(seed, FROM_KEY, TO_KEY, amount);

    TransferTx tx = TransferTx.fromMessage(m);

    assertThat(tx, equalTo(withMockMessage(seed, FROM_KEY, TO_KEY, amount)));
  }

  @Test
  void fromMessageRejectsSameSenderAndReceiver() {
    long seed = 1;
    long amount = 50L;
    RawTransaction m = createRawTransaction(seed, FROM_KEY, FROM_KEY, amount);

    Exception e = assertThrows(IllegalArgumentException.class,
        () -> TransferTx.fromMessage(m));

    assertThat(e.getMessage(), allOf(
        containsStringIgnoringCase("same sender and receiver"),
        containsStringIgnoringCase(FROM_KEY.toString())));
  }

  @ParameterizedTest
  @ValueSource(longs = {
      Long.MIN_VALUE,
      -100,
      -1,
      0
  })
  void fromMessageRejectsNonPositiveBalance(long transferAmount) {
    long seed = 1;
    RawTransaction m = createRawTransaction(seed, FROM_KEY, TO_KEY, transferAmount);

    Exception e = assertThrows(IllegalArgumentException.class,
        () -> TransferTx.fromMessage(m));

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
      InternalTransactionContext context = new InternalTransactionContext(view, null, null);

      // Create source and target wallets with the given initial balances
      long initialBalance = 100L;
      createWallet(view, FROM_KEY, initialBalance);
      createWallet(view, TO_KEY, initialBalance);

      // Create and execute the transaction
      long seed = 1L;
      long transferSum = 40L;
      TransferTx tx = withMockMessage(seed, FROM_KEY, TO_KEY, transferSum);
      tx.execute(context);

      // Check that wallets have correct balances
      CryptocurrencySchema schema = new CryptocurrencySchema(view);
      ProofMapIndexProxy<PublicKey, Wallet> wallets = schema.wallets();
      long expectedFromValue = initialBalance - transferSum;
      assertThat(wallets.get(FROM_KEY).getBalance(), equalTo(expectedFromValue));
      long expectedToValue = initialBalance + transferSum;
      assertThat(wallets.get(TO_KEY).getBalance(), equalTo(expectedToValue));

      // Check history
      HistoryEntity expectedEntity = HistoryEntity.Builder.newBuilder()
          .setSeed(seed)
          .setWalletFrom(FROM_KEY)
          .setWalletTo(TO_KEY)
          .setAmount(transferSum)
          .setTransactionHash(tx.hash())
          .build();
      assertThat(schema.walletHistory(FROM_KEY), hasItem(expectedEntity));
      assertThat(schema.walletHistory(TO_KEY), hasItem(expectedEntity));
    }
  }

  @Test
  @RequiresNativeLibrary
  void executeTransfer_NoSuchFromWallet() throws CloseFailuresException {
    try (Database db = MemoryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);
      InternalTransactionContext context = new InternalTransactionContext(view, null, null);

      // Create a receiver’s wallet with the given initial balance
      long initialBalance = 50L;
      createWallet(view, TO_KEY, initialBalance);

      long seed = 1L;
      long transferValue = 50L;
      TransferTx tx = withMockMessage(seed, FROM_KEY, TO_KEY, transferValue);
      // Execute the transaction that attempts to transfer from an unknown wallet
      TransactionExecutionException e = assertThrows(
          TransactionExecutionException.class, () -> tx.execute(context));
      assertThat(e, hasErrorCode(UNKNOWN_SENDER));
    }
  }

  @Test
  @RequiresNativeLibrary
  void executeTransfer_NoSuchToWallet() throws CloseFailuresException {
    try (Database db = MemoryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);
      InternalTransactionContext context = new InternalTransactionContext(view, null, null);

      // Create a sender’s wallet
      long initialBalance = 100L;
      createWallet(view, FROM_KEY, initialBalance);

      // Create and execute the transaction that attempts to transfer to unknown wallet
      long transferValue = 50L;
      long seed = 1L;

      TransferTx tx = withMockMessage(seed, FROM_KEY, TO_KEY, transferValue);
      TransactionExecutionException e = assertThrows(
          TransactionExecutionException.class, () -> tx.execute(context));
      assertThat(e, hasErrorCode(UNKNOWN_RECEIVER));
    }
  }

  @Test
  @RequiresNativeLibrary
  void executeTransfer_InsufficientFunds() throws CloseFailuresException {
    try (Database db = MemoryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);
      InternalTransactionContext context = new InternalTransactionContext(view, null, null);

      // Create source and target wallets with the given initial balances
      long initialBalance = 100L;
      createWallet(view, FROM_KEY, initialBalance);
      createWallet(view, TO_KEY, initialBalance);

      // Create and execute the transaction that attempts to transfer an amount
      // exceeding the balance
      long seed = 1L;
      long transferValue = initialBalance + 50L;
      TransferTx tx = withMockMessage(seed, FROM_KEY, TO_KEY, transferValue);

      TransactionExecutionException e = assertThrows(
          TransactionExecutionException.class, () -> tx.execute(context));
      assertThat(e, hasErrorCode(INSUFFICIENT_FUNDS));
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

  private static Matcher<TransactionExecutionException> hasErrorCode(TransactionError expected) {
    return new FeatureMatcher<TransactionExecutionException, Byte>(equalTo(expected.errorCode),
        "ExecutionException#errorCode", "errorCode") {

      @Override
      protected Byte featureValueOf(TransactionExecutionException actual) {
        return actual.getErrorCode();
      }
    };
  }
}
