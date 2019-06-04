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
import static com.exonum.binding.cryptocurrency.transactions.TransactionError.INSUFFICIENT_FUNDS;
import static com.exonum.binding.cryptocurrency.transactions.TransactionError.SAME_SENDER_AND_RECEIVER;
import static com.exonum.binding.cryptocurrency.transactions.TransactionError.UNKNOWN_RECEIVER;
import static com.exonum.binding.cryptocurrency.transactions.TransactionError.UNKNOWN_SENDER;
import static com.exonum.binding.cryptocurrency.transactions.TransactionUtils.newCreateWalletTransaction;
import static com.exonum.binding.cryptocurrency.transactions.TransactionUtils.newTransferRawTransaction;
import static com.exonum.binding.cryptocurrency.transactions.TransactionUtils.newTransferTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.blockchain.Blockchain;
import com.exonum.binding.common.blockchain.TransactionResult;
import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.cryptocurrency.CryptocurrencyServiceModule;
import com.exonum.binding.cryptocurrency.PredefinedOwnerKeys;
import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.testkit.TestKit;
import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.transaction.Transaction;
import com.google.common.reflect.TypeToken;
import java.util.Optional;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TransferTxTest {

  private static final KeyPair FROM_KEY_PAIR = PredefinedOwnerKeys.FIRST_OWNER_KEY_PAIR;
  private static final KeyPair TO_KEY_PAIR = PredefinedOwnerKeys.SECOND_OWNER_KEY_PAIR;

  @Test
  void fromRawTransaction() {
    long seed = 1;
    long amount = 50L;
    PublicKey recipientKey = TO_KEY_PAIR.getPublicKey();
    RawTransaction raw = newTransferRawTransaction(seed, amount, recipientKey);

    TransferTx tx = TransferTx.fromRawTransaction(raw);

    assertThat(tx).isEqualTo(new TransferTx(seed, recipientKey, amount));
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
        newTransferRawTransaction(seed, transferAmount, TO_KEY_PAIR.getPublicKey());

    Exception e = assertThrows(IllegalArgumentException.class,
        () -> TransferTx.fromRawTransaction(tx));

    assertThat(e.getMessage()).contains("transfer amount")
        .contains(Long.toString(transferAmount));
  }

  @Test
  @RequiresNativeLibrary
  void executeTransfer() {
    try (TestKit testKit = TestKit.forService(CryptocurrencyServiceModule.class)) {
      // Create source and target wallets with the same initial balance
      long initialBalance = 100L;
      TransactionMessage createFromWalletTx =
          newCreateWalletTransaction(initialBalance, FROM_KEY_PAIR);
      TransactionMessage createToWalletTx =
          newCreateWalletTransaction(initialBalance, TO_KEY_PAIR);
      testKit.createBlockWithTransactions(createFromWalletTx, createToWalletTx);

      // Create and execute the transaction
      long seed = 1L;
      long transferSum = 40L;
      TransactionMessage transferTx = newTransferTransaction(
          seed, FROM_KEY_PAIR, TO_KEY_PAIR.getPublicKey(), transferSum);
      testKit.createBlockWithTransactions(transferTx);

      testKit.withSnapshot((view) -> {
        // Check that wallets have correct balances
        CryptocurrencySchema schema = new CryptocurrencySchema(view);
        ProofMapIndexProxy<PublicKey, Wallet> wallets = schema.wallets();
        long expectedFromValue = initialBalance - transferSum;
        assertThat(wallets.get(FROM_KEY_PAIR.getPublicKey()).getBalance())
            .isEqualTo(expectedFromValue);
        long expectedToValue = initialBalance + transferSum;
        assertThat(wallets.get(TO_KEY_PAIR.getPublicKey()).getBalance())
            .isEqualTo(expectedToValue);

        // Check history
        HashCode messageHash = transferTx.hash();
        assertThat(schema.transactionsHistory(FROM_KEY_PAIR.getPublicKey()))
            .containsExactly(messageHash);
        assertThat(schema.transactionsHistory(TO_KEY_PAIR.getPublicKey()))
            .containsExactly(messageHash);
        return null;
      });
    }
  }

  @Test
  @RequiresNativeLibrary
  void executeTransfer_NoSuchFromWallet() {
    try (TestKit testKit = TestKit.forService(CryptocurrencyServiceModule.class)) {
      // Create a receiver’s wallet with the given initial balance
      long initialBalance = 50L;
      TransactionMessage createToWalletTx =
          newCreateWalletTransaction(initialBalance, TO_KEY_PAIR);
      testKit.createBlockWithTransactions(createToWalletTx);

      long seed = 1L;
      long transferSum = 50L;
      TransactionMessage transferTx = newTransferTransaction(
          seed, FROM_KEY_PAIR, TO_KEY_PAIR.getPublicKey(), transferSum);
      testKit.createBlockWithTransactions(transferTx);

      testKit.withSnapshot((view) -> {
        Blockchain blockchain = Blockchain.newInstance(view);
        Optional<TransactionResult> txResult = blockchain.getTxResult(transferTx.hash());
        TransactionResult expectedTransactionResult =
            TransactionResult.error(UNKNOWN_SENDER.errorCode, null);
        assertThat(txResult).hasValue(expectedTransactionResult);
        return null;
      });
    }
  }

  @Test
  @RequiresNativeLibrary
  void executeTransfer_NoSuchToWallet() {
    try (TestKit testKit = TestKit.forService(CryptocurrencyServiceModule.class)) {
      // Create a receiver’s wallet with the given initial balance
      long initialBalance = 50L;
      TransactionMessage createFromWalletTx =
          newCreateWalletTransaction(initialBalance, FROM_KEY_PAIR);
      testKit.createBlockWithTransactions(createFromWalletTx);

      long seed = 1L;
      long transferSum = 50L;
      TransactionMessage transferTx = newTransferTransaction(
          seed, FROM_KEY_PAIR, TO_KEY_PAIR.getPublicKey(), transferSum);
      testKit.createBlockWithTransactions(transferTx);

      testKit.withSnapshot((view) -> {
        Blockchain blockchain = Blockchain.newInstance(view);
        Optional<TransactionResult> txResult = blockchain.getTxResult(transferTx.hash());
        TransactionResult expectedTransactionResult =
            TransactionResult.error(UNKNOWN_RECEIVER.errorCode, null);
        assertThat(txResult).hasValue(expectedTransactionResult);
        return null;
      });
    }
  }

  @Test
  @RequiresNativeLibrary
  void executeTransfer_RejectsSameSenderAndReceiver() {
    try (TestKit testKit = TestKit.forService(CryptocurrencyServiceModule.class)) {
      long seed = 1L;
      long transferSum = 50L;
      TransactionMessage transferTx = newTransferTransaction(
          seed, FROM_KEY_PAIR, FROM_KEY_PAIR.getPublicKey(), transferSum);
      testKit.createBlockWithTransactions(transferTx);

      testKit.withSnapshot((view) -> {
        Blockchain blockchain = Blockchain.newInstance(view);
        Optional<TransactionResult> txResult = blockchain.getTxResult(transferTx.hash());
        TransactionResult expectedTransactionResult =
            TransactionResult.error(SAME_SENDER_AND_RECEIVER.errorCode, null);
        assertThat(txResult).hasValue(expectedTransactionResult);
        return null;
      });
    }
  }

  @Test
  @RequiresNativeLibrary
  void executeTransfer_InsufficientFunds() {
    try (TestKit testKit = TestKit.forService(CryptocurrencyServiceModule.class)) {
      // Create source and target wallets with the same initial balance
      long initialBalance = 50L;
      TransactionMessage createFromWalletTx =
          newCreateWalletTransaction(initialBalance, FROM_KEY_PAIR);
      TransactionMessage createToWalletTx =
          newCreateWalletTransaction(initialBalance, TO_KEY_PAIR);
      testKit.createBlockWithTransactions(createFromWalletTx, createToWalletTx);

      // Create and execute the transaction that attempts to transfer an amount exceeding the
      // balance
      long seed = 1L;
      long transferSum = initialBalance + 50L;
      TransactionMessage transferTx = newTransferTransaction(
          seed, FROM_KEY_PAIR, TO_KEY_PAIR.getPublicKey(), transferSum);
      testKit.createBlockWithTransactions(transferTx);

      testKit.withSnapshot((view) -> {
        Blockchain blockchain = Blockchain.newInstance(view);
        Optional<TransactionResult> txResult = blockchain.getTxResult(transferTx.hash());
        TransactionResult expectedTransactionResult =
            TransactionResult.error(INSUFFICIENT_FUNDS.errorCode, null);
        assertThat(txResult).hasValue(expectedTransactionResult);
        return null;
      });
    }
  }

  @Test
  void info() {
    long seed = Long.MAX_VALUE - 1L;
    TransferTx tx =  new TransferTx(seed, TO_KEY_PAIR.getPublicKey(), 50L);

    String info = tx.info();

    // Check the transaction parameters in JSON
    Transaction txParameters = json().fromJson(info, new TypeToken<TransferTx>() {
    }.getType());

    assertThat(txParameters).isEqualTo(tx);
  }

  @Test
  void verifyEquals() {
    EqualsVerifier
        .forClass(TransferTx.class)
        .withPrefabValues(HashCode.class, HashCode.fromInt(1), HashCode.fromInt(2))
        .verify();
  }
}
