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

import static com.exonum.binding.cryptocurrency.transactions.CreateTransferTransactionUtils.createWallet;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.BinaryMessage;
import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.cryptocurrency.HistoryEntity;
import com.exonum.binding.cryptocurrency.PredefinedOwnerKeys;
import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.util.LibraryLoader;
import org.junit.jupiter.api.Test;

@RequiresNativeLibrary
class TransferTxHistoryTest {

  static {
    LibraryLoader.load();
  }

  private static final PublicKey ACCOUNT_1 = PredefinedOwnerKeys.firstOwnerKey;
  private static final PublicKey ACCOUNT_2 = PredefinedOwnerKeys.secondOwnerKey;

  @Test
  void transfersHistoryBetweenTwoAccountsTest() throws Exception {
    try (Database db = MemoryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      Fork view = db.createFork(cleaner);

      // Create wallets with the given initial balances
      long initialBalance = 100L;
      createWallet(view, ACCOUNT_1, initialBalance);
      createWallet(view, ACCOUNT_2, initialBalance);

      // Create and execute 1st transaction
      long seed1 = 1L;
      long transferSum1 = 40L;
      TransferTx tx1 = withMockMessage(seed1, ACCOUNT_1, ACCOUNT_2, transferSum1);
      tx1.execute(view);
      // Create and execute 2nd transaction
      long seed2 = 2L;
      long transferSum2 = 10L;
      TransferTx tx2 = withMockMessage(seed2, ACCOUNT_2, ACCOUNT_1, transferSum2);
      tx2.execute(view);

      // Check that wallets have correct balances
      CryptocurrencySchema schema = new CryptocurrencySchema(view);
      ProofMapIndexProxy<PublicKey, Wallet> wallets = schema.wallets();
      long expectedBalance1 = initialBalance - transferSum1 + transferSum2;
      assertThat(wallets.get(ACCOUNT_1).getBalance(), equalTo(expectedBalance1));
      long expectedBalance2 = initialBalance + transferSum1 - transferSum2;
      assertThat(wallets.get(ACCOUNT_2).getBalance(), equalTo(expectedBalance2));

      // Check history
      HistoryEntity expectedEntity = HistoryEntity.Builder.newBuilder()
          .setSeed(seed1)
          .setWalletFrom(ACCOUNT_1)
          .setWalletTo(ACCOUNT_2)
          .setAmount(transferSum1)
          .setTransactionHash(tx1.hash())
          .build();
      HistoryEntity expectedEntity2 = HistoryEntity.Builder.newBuilder()
          .setSeed(seed2)
          .setWalletFrom(ACCOUNT_2)
          .setWalletTo(ACCOUNT_1)
          .setAmount(transferSum2)
          .setTransactionHash(tx2.hash())
          .build();
      assertThat(schema.walletHistory(ACCOUNT_1),
          allOf(iterableWithSize(2), hasItem(expectedEntity), hasItem(expectedEntity2)));
      assertThat(schema.walletHistory(ACCOUNT_2),
          allOf(iterableWithSize(2), hasItem(expectedEntity), hasItem(expectedEntity2)));
    }

  }

  private TransferTx withMockMessage(long seed, PublicKey senderId, PublicKey recipientId,
      long amount) {
    // If a normal binary message object is ever needed, take the code from the 'fromMessage' test
    // and put it here, replacing `mock(BinaryMessage.class)`.
    BinaryMessage message = mock(BinaryMessage.class);
    lenient().when(message.hash()).thenReturn(HashCode.fromString("a0a0a0a0"));
    return new TransferTx(message, seed, senderId, recipientId, amount);
  }

}
