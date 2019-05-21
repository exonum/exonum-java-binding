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
 *
 */

package com.exonum.binding.cryptocurrency.transactions;

import static com.exonum.binding.cryptocurrency.transactions.TransactionUtils.createCreateWalletTransaction;
import static com.exonum.binding.cryptocurrency.transactions.TransactionUtils.createTransferTransaction;
import static org.assertj.core.api.Assertions.assertThat;

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
import com.exonum.binding.util.LibraryLoader;
import org.junit.jupiter.api.Test;

@RequiresNativeLibrary
class TransferTxHistoryTest {

  private static final KeyPair TO_KEY_PAIR = PredefinedOwnerKeys.FIRST_OWNER_KEY_PAIR;
  private static final KeyPair FROM_KEY_PAIR = PredefinedOwnerKeys.SECOND_OWNER_KEY_PAIR;

  static {
    LibraryLoader.load();
  }

  @Test
  @RequiresNativeLibrary
  void transfersHistoryBetweenTwoAccountsTest() {
    try (TestKit testKit = TestKit.forService(CryptocurrencyServiceModule.class)) {
      // Create source and target wallets with the given initial balances
      long initialBalance = 100L;
      TransactionMessage createFromWalletTx1 =
          createCreateWalletTransaction(initialBalance, TO_KEY_PAIR);
      TransactionMessage createFromWalletTx2 =
          createCreateWalletTransaction(initialBalance, FROM_KEY_PAIR);
      testKit.createBlockWithTransactions(createFromWalletTx1, createFromWalletTx2);

      // Create and execute 1st transaction
      long seed1 = 1L;
      long transferSum1 = 40L;
      TransactionMessage transferTx1 = createTransferTransaction(
          seed1, FROM_KEY_PAIR.getPublicKey(), transferSum1, TO_KEY_PAIR);
      testKit.createBlockWithTransactions(transferTx1);

      // Create and execute 2nd transaction
      long seed2 = 2L;
      long transferSum2 = 10L;
      TransactionMessage transferTx2 = createTransferTransaction(
          seed2, TO_KEY_PAIR.getPublicKey(), transferSum2, FROM_KEY_PAIR);
      testKit.createBlockWithTransactions(transferTx2);

      testKit.withSnapshot((view) -> {
        // Check that wallets have correct balances
        CryptocurrencySchema schema = new CryptocurrencySchema(view);
        ProofMapIndexProxy<PublicKey, Wallet> wallets = schema.wallets();
        long expectedBalance1 = initialBalance - transferSum1 + transferSum2;
        assertThat(wallets.get(TO_KEY_PAIR.getPublicKey()).getBalance())
            .isEqualTo(expectedBalance1);
        long expectedBalance2 = initialBalance + transferSum1 - transferSum2;
        assertThat(wallets.get(FROM_KEY_PAIR.getPublicKey()).getBalance())
            .isEqualTo(expectedBalance2);

        // Check history
        HashCode messageHash1 = transferTx1.hash();
        HashCode messageHash2 = transferTx2.hash();
        assertThat(schema.transactionsHistory(TO_KEY_PAIR.getPublicKey()))
            .contains(messageHash1, messageHash2);
        assertThat(schema.transactionsHistory(FROM_KEY_PAIR.getPublicKey()))
            .contains(messageHash1, messageHash2);
        return null;
      });
    }
  }
}
