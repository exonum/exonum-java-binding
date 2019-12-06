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

import static com.exonum.binding.cryptocurrency.transactions.PredefinedServiceParameters.ARTIFACT_FILENAME;
import static com.exonum.binding.cryptocurrency.transactions.PredefinedServiceParameters.ARTIFACT_ID;
import static com.exonum.binding.cryptocurrency.transactions.PredefinedServiceParameters.SERVICE_ID;
import static com.exonum.binding.cryptocurrency.transactions.PredefinedServiceParameters.SERVICE_NAME;
import static com.exonum.binding.cryptocurrency.transactions.PredefinedServiceParameters.artifactsDirectory;
import static com.exonum.binding.cryptocurrency.transactions.TransactionUtils.newCreateWalletTransaction;
import static com.exonum.binding.cryptocurrency.transactions.TransactionUtils.newTransferTransaction;
import static org.assertj.core.api.Assertions.assertThat;

import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.cryptocurrency.PredefinedOwnerKeys;
import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.testkit.TestKit;
import com.exonum.binding.testkit.TestKitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@RequiresNativeLibrary
class TransferTxHistoryIntegrationTest {

  @RegisterExtension
  TestKitExtension testKitExtension = new TestKitExtension(
      TestKit.builder()
          .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
          .withService(ARTIFACT_ID, SERVICE_NAME, SERVICE_ID)
          .withArtifactsDirectory(artifactsDirectory));

  private static final KeyPair ACCOUNT_1 = PredefinedOwnerKeys.FIRST_OWNER_KEY_PAIR;
  private static final KeyPair ACCOUNT_2 = PredefinedOwnerKeys.SECOND_OWNER_KEY_PAIR;

  @Test
  @RequiresNativeLibrary
  void transfersHistoryBetweenTwoAccountsTest(TestKit testKit) {
    // Create source and target wallets with the same initial balance
    long initialBalance = 100L;
    TransactionMessage createFromWalletTx1 =
        newCreateWalletTransaction(initialBalance, ACCOUNT_1, SERVICE_ID);
    TransactionMessage createFromWalletTx2 =
        newCreateWalletTransaction(initialBalance, ACCOUNT_2, SERVICE_ID);
    testKit.createBlockWithTransactions(createFromWalletTx1, createFromWalletTx2);

    // Create and execute 1st transaction
    long seed1 = 1L;
    long transferSum1 = 40L;
    TransactionMessage transferTx1 = newTransferTransaction(
        seed1, ACCOUNT_1, ACCOUNT_2.getPublicKey(), transferSum1, SERVICE_ID);
    testKit.createBlockWithTransactions(transferTx1);

    // Create and execute 2nd transaction
    long seed2 = 2L;
    long transferSum2 = 10L;
    TransactionMessage transferTx2 = newTransferTransaction(
        seed2, ACCOUNT_2, ACCOUNT_1.getPublicKey(), transferSum2, SERVICE_ID);
    testKit.createBlockWithTransactions(transferTx2);

    Snapshot view = testKit.getSnapshot();

    // Check that wallets have correct balances
    CryptocurrencySchema schema = new CryptocurrencySchema(view, SERVICE_NAME);
    ProofMapIndexProxy<PublicKey, Wallet> wallets = schema.wallets();
    long expectedBalance1 = initialBalance - transferSum1 + transferSum2;
    assertThat(wallets.get(ACCOUNT_1.getPublicKey()).getBalance())
        .isEqualTo(expectedBalance1);
    long expectedBalance2 = initialBalance + transferSum1 - transferSum2;
    assertThat(wallets.get(ACCOUNT_2.getPublicKey()).getBalance())
        .isEqualTo(expectedBalance2);

    // Check history
    HashCode messageHash1 = transferTx1.hash();
    HashCode messageHash2 = transferTx2.hash();
    assertThat(schema.transactionsHistory(ACCOUNT_1.getPublicKey()))
        .containsExactly(messageHash1, messageHash2);
    assertThat(schema.transactionsHistory(ACCOUNT_2.getPublicKey()))
        .containsExactly(messageHash1, messageHash2);
  }
}
