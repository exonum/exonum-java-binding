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

import static com.exonum.binding.cryptocurrency.CryptocurrencyServiceImpl.CRYPTO_FUNCTION;
import static com.exonum.binding.cryptocurrency.transactions.CreateTransferTransactionUtils.createSignedMessage;
import static com.exonum.binding.cryptocurrency.transactions.CreateTransferTransactionUtils.createUnsignedMessage;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.BinaryMessage;
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
import com.exonum.binding.transaction.Transaction;
import com.exonum.binding.transaction.TransactionExecutionException;
import com.exonum.binding.util.LibraryLoader;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
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

  private static final PublicKey fromKey = PredefinedOwnerKeys.firstOwnerKey;

  private static final PublicKey toKey = PredefinedOwnerKeys.secondOwnerKey;

  @Test
  void fromMessage() {
    long seed = 1;
    long amount = 50L;
    BinaryMessage m = createUnsignedMessage(seed, fromKey, toKey, amount);

    TransferTx tx = TransferTx.fromMessage(m);

    assertThat(tx, equalTo(withMockMessage(seed, fromKey, toKey, amount)));
  }

  @Test
  void fromMessageRejectsSameSenderAndReceiver() {
    long seed = 1;
    long amount = 50L;
    BinaryMessage m = createUnsignedMessage(seed, fromKey, fromKey, amount);

    Exception e = assertThrows(IllegalArgumentException.class,
        () -> TransferTx.fromMessage(m));

    assertThat(e.getMessage(), allOf(
        containsStringIgnoringCase("same sender and receiver"),
        containsStringIgnoringCase(fromKey.toString())));
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
    BinaryMessage m = createUnsignedMessage(seed, fromKey, toKey, transferAmount);

    Exception e = assertThrows(IllegalArgumentException.class,
        () -> TransferTx.fromMessage(m));

    assertThat(e.getMessage(), allOf(
        containsStringIgnoringCase("transfer amount"),
        containsString(Long.toString(transferAmount))));
  }

  @Test
  void isValidSigned() {
    long seed = 1;
    long amount = 50L;
    KeyPair senderKeyPair = CRYPTO_FUNCTION.generateKeyPair();

    BinaryMessage m = createSignedMessage(seed, senderKeyPair, toKey, amount);

    TransferTx tx = TransferTx.fromMessage(m);

    assertTrue(tx.isValid());
  }

  @Test
  void isValidWrongSignature() {
    long seed = 1;
    long amount = 50L;

    // A message that is not signed does not have a proper cryptographic signature.
    BinaryMessage m = createUnsignedMessage(seed, fromKey, toKey, amount);

    TransferTx tx = TransferTx.fromMessage(m);

    assertFalse(tx.isValid());
  }

  @Test
  @RequiresNativeLibrary
  void executeTransfer() throws Exception {
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
      tx.execute(view);

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
  void executeTransfer_NoSuchFromWallet() throws CloseFailuresException {
    try (Database db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);
      // Create a receiver’s wallet with the given initial balance
      long initialBalance = 50L;
      createWallet(view, toKey, initialBalance);

      long seed = 1L;

      long transferValue = 50L;
      TransferTx tx = withMockMessage(seed, fromKey, toKey, transferValue);
      // Execute the transaction that attempts to transfer from an unknown wallet
      TransactionExecutionException e = assertThrows(
          TransactionExecutionException.class, () -> tx.execute(view));
      assertThat(e, hasErrorCode(UNKNOWN_SENDER));
    }
  }

  @Test
  @RequiresNativeLibrary
  void executeTransfer_NoSuchToWallet() throws CloseFailuresException {
    try (Database db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);
      // Create a sender’s wallet
      long initialBalance = 100L;
      createWallet(view, fromKey, initialBalance);

      // Create and execute the transaction that attempts to transfer to unknown wallet
      long transferValue = 50L;
      long seed = 1L;

      TransferTx tx = withMockMessage(seed, fromKey, toKey, transferValue);
      TransactionExecutionException e = assertThrows(
          TransactionExecutionException.class, () -> tx.execute(view));
      assertThat(e, hasErrorCode(UNKNOWN_RECEIVER));
    }
  }

  @Test
  @RequiresNativeLibrary
  void executeTransfer_InsufficientFunds() throws CloseFailuresException {
    try (Database db = MemoryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);
      // Create source and target wallets with the given initial balances
      long initialBalance = 100L;
      createWallet(view, fromKey, initialBalance);
      createWallet(view, toKey, initialBalance);

      // Create and execute the transaction that attempts to transfer an amount
      // exceeding the balance
      long seed = 1L;
      long transferValue = initialBalance + 50L;
      TransferTx tx = withMockMessage(seed, fromKey, toKey, transferValue);

      TransactionExecutionException e = assertThrows(
          TransactionExecutionException.class, () -> tx.execute(view));
      assertThat(e, hasErrorCode(INSUFFICIENT_FUNDS));
    }
  }

  @Test
  void info() {
    long seed = Long.MAX_VALUE - 1L;
    TransferTx tx = withMockMessage(seed, fromKey, toKey, 50L);

    String info = tx.info();

    // Check the transaction parameters in JSON
    Gson gson = CryptocurrencyTransactionGson.instance();

    Transaction txParameters = gson.fromJson(info, new TypeToken<TransferTx>() {
    }.getType());

    assertThat(txParameters, equalTo(tx));
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
    // If a normal binary message object is ever needed, take the code from the 'fromMessage' test
    // and put it here, replacing `mock(BinaryMessage.class)`.
    BinaryMessage message = mock(BinaryMessage.class);
    lenient().when(message.hash()).thenReturn(HashCode.fromString("a0a0a0a0"));
    return new TransferTx(message, seed, senderId, recipientId, amount);
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
