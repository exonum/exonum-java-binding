/*
 * Copyright 2019 The Exonum Team
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

package com.exonum.binding.cryptocurrency;

import static com.exonum.binding.common.blockchain.ExecutionStatuses.serviceError;
import static com.exonum.binding.cryptocurrency.PredefinedServiceParameters.ARTIFACT_FILENAME;
import static com.exonum.binding.cryptocurrency.PredefinedServiceParameters.ARTIFACT_ID;
import static com.exonum.binding.cryptocurrency.PredefinedServiceParameters.SERVICE_ID;
import static com.exonum.binding.cryptocurrency.PredefinedServiceParameters.SERVICE_NAME;
import static com.exonum.binding.cryptocurrency.PredefinedServiceParameters.artifactsDirectory;
import static com.exonum.binding.cryptocurrency.TransactionError.INSUFFICIENT_FUNDS;
import static com.exonum.binding.cryptocurrency.TransactionError.NON_POSITIVE_TRANSFER_AMOUNT;
import static com.exonum.binding.cryptocurrency.TransactionError.SAME_SENDER_AND_RECEIVER;
import static com.exonum.binding.cryptocurrency.TransactionError.UNKNOWN_RECEIVER;
import static com.exonum.binding.cryptocurrency.TransactionError.UNKNOWN_SENDER;
import static com.exonum.binding.cryptocurrency.TransactionUtils.newCreateWalletTransaction;
import static com.exonum.binding.cryptocurrency.TransactionUtils.newTransferTransaction;
import static org.assertj.core.api.Assertions.assertThat;

import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.core.blockchain.Blockchain;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.testkit.TestKit;
import com.exonum.binding.testkit.TestKitExtension;
import com.exonum.core.messages.Runtime.ExecutionStatus;
import java.util.Optional;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TransferTxIntegrationTest {

  @RegisterExtension
  TestKitExtension testKitExtension = new TestKitExtension(
      TestKit.builder()
          .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
          .withService(ARTIFACT_ID, SERVICE_NAME, SERVICE_ID)
          .withArtifactsDirectory(artifactsDirectory));

  private static final KeyPair FROM_KEY_PAIR = PredefinedOwnerKeys.FIRST_OWNER_KEY_PAIR;
  private static final KeyPair TO_KEY_PAIR = PredefinedOwnerKeys.SECOND_OWNER_KEY_PAIR;

  @Test
  @RequiresNativeLibrary
  void executeTransfer(TestKit testKit) {
    // Create source and target wallets with the same initial balance
    long initialBalance = 100L;
    TransactionMessage createFromWalletTx =
        newCreateWalletTransaction(initialBalance, FROM_KEY_PAIR, SERVICE_ID);
    TransactionMessage createToWalletTx =
        newCreateWalletTransaction(initialBalance, TO_KEY_PAIR, SERVICE_ID);
    testKit.createBlockWithTransactions(createFromWalletTx, createToWalletTx);

    // Create and execute the transaction
    long seed = 1L;
    long transferSum = 40L;
    TransactionMessage transferTx = newTransferTransaction(
        seed, FROM_KEY_PAIR, TO_KEY_PAIR.getPublicKey(), transferSum, SERVICE_ID);
    testKit.createBlockWithTransactions(transferTx);

    Snapshot view = testKit.getSnapshot();

    // Check that wallets have correct balances
    CryptocurrencySchema schema = new CryptocurrencySchema(view, SERVICE_NAME);
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
  }

  @Disabled("ECR-4014")
  @ParameterizedTest
  @ValueSource(longs = {Long.MIN_VALUE, -1L, 0L})
  @RequiresNativeLibrary
  void executeTransfer_NonPositiveBalance(long transferSum, TestKit testKit) {
    // Create and execute the transaction
    long seed = 1L;
    TransactionMessage transferTx = newTransferTransaction(
        seed, FROM_KEY_PAIR, TO_KEY_PAIR.getPublicKey(), transferSum, SERVICE_ID);
    testKit.createBlockWithTransactions(transferTx);

    // Check the Exception
    Snapshot view = testKit.getSnapshot();
    Blockchain blockchain = Blockchain.newInstance(view);
    Optional<ExecutionStatus> txResultOpt = blockchain.getTxResult(transferTx.hash());
    String description = "Non-positive transfer amount: " + transferSum;
    ExecutionStatus expectedTransactionResult =
        serviceError(NON_POSITIVE_TRANSFER_AMOUNT.errorCode, description);
    assertThat(txResultOpt).hasValue(expectedTransactionResult);
  }

  @Test
  @Disabled("ECR-4014")
  @RequiresNativeLibrary
  void executeTransfer_NoSuchFromWallet(TestKit testKit) {
    // Create a receiver’s wallet with the given initial balance
    long initialBalance = 50L;
    TransactionMessage createToWalletTx =
        newCreateWalletTransaction(initialBalance, TO_KEY_PAIR, SERVICE_ID);
    testKit.createBlockWithTransactions(createToWalletTx);

    long seed = 1L;
    long transferSum = 50L;
    TransactionMessage transferTx = newTransferTransaction(
        seed, FROM_KEY_PAIR, TO_KEY_PAIR.getPublicKey(), transferSum, SERVICE_ID);
    testKit.createBlockWithTransactions(transferTx);

    Snapshot view = testKit.getSnapshot();
    Blockchain blockchain = Blockchain.newInstance(view);
    Optional<ExecutionStatus> txResult = blockchain.getTxResult(transferTx.hash());
    ExecutionStatus expectedTransactionResult = serviceError(UNKNOWN_SENDER.errorCode);
    assertThat(txResult).hasValue(expectedTransactionResult);
  }

  @Test
  @Disabled("ECR-4014")
  @RequiresNativeLibrary
  void executeTransfer_NoSuchToWallet(TestKit testKit) {
    // Create a receiver’s wallet with the given initial balance
    long initialBalance = 50L;
    TransactionMessage createFromWalletTx =
        newCreateWalletTransaction(initialBalance, FROM_KEY_PAIR, SERVICE_ID);
    testKit.createBlockWithTransactions(createFromWalletTx);

    long seed = 1L;
    long transferSum = 50L;
    TransactionMessage transferTx = newTransferTransaction(
        seed, FROM_KEY_PAIR, TO_KEY_PAIR.getPublicKey(), transferSum, SERVICE_ID);
    testKit.createBlockWithTransactions(transferTx);

    Snapshot view = testKit.getSnapshot();
    Blockchain blockchain = Blockchain.newInstance(view);
    Optional<ExecutionStatus> txResult = blockchain.getTxResult(transferTx.hash());
    ExecutionStatus expectedTransactionResult = serviceError(UNKNOWN_RECEIVER.errorCode);
    assertThat(txResult).hasValue(expectedTransactionResult);
  }

  @Test
  @Disabled("ECR-4014")
  @RequiresNativeLibrary
  void executeTransfer_RejectsSameSenderAndReceiver(TestKit testKit) {
    long seed = 1L;
    long transferSum = 50L;
    TransactionMessage transferTx = newTransferTransaction(
        seed, FROM_KEY_PAIR, FROM_KEY_PAIR.getPublicKey(), transferSum, SERVICE_ID);
    testKit.createBlockWithTransactions(transferTx);

    Snapshot view = testKit.getSnapshot();
    Blockchain blockchain = Blockchain.newInstance(view);
    Optional<ExecutionStatus> txResult = blockchain.getTxResult(transferTx.hash());
    ExecutionStatus expectedTransactionResult = serviceError(SAME_SENDER_AND_RECEIVER.errorCode);
    assertThat(txResult).hasValue(expectedTransactionResult);
  }

  @Test
  @Disabled("ECR-4014")
  @RequiresNativeLibrary
  void executeTransfer_InsufficientFunds(TestKit testKit) {
    // Create source and target wallets with the same initial balance
    long initialBalance = 50L;
    TransactionMessage createFromWalletTx =
        newCreateWalletTransaction(initialBalance, FROM_KEY_PAIR, SERVICE_ID);
    TransactionMessage createToWalletTx =
        newCreateWalletTransaction(initialBalance, TO_KEY_PAIR, SERVICE_ID);
    testKit.createBlockWithTransactions(createFromWalletTx, createToWalletTx);

    // Create and execute the transaction that attempts to transfer an amount exceeding the
    // balance
    long seed = 1L;
    long transferSum = initialBalance + 50L;
    TransactionMessage transferTx = newTransferTransaction(
        seed, FROM_KEY_PAIR, TO_KEY_PAIR.getPublicKey(), transferSum, SERVICE_ID);
    testKit.createBlockWithTransactions(transferTx);

    Snapshot view = testKit.getSnapshot();
    Blockchain blockchain = Blockchain.newInstance(view);
    Optional<ExecutionStatus> txResult = blockchain.getTxResult(transferTx.hash());
    ExecutionStatus expectedTransactionResult = serviceError(INSUFFICIENT_FUNDS.errorCode);
    assertThat(txResult).hasValue(expectedTransactionResult);
  }
}
