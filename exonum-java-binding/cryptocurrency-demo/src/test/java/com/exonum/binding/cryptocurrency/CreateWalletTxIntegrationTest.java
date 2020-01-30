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

import static com.exonum.binding.cryptocurrency.PredefinedServiceParameters.ARTIFACT_FILENAME;
import static com.exonum.binding.cryptocurrency.PredefinedServiceParameters.ARTIFACT_ID;
import static com.exonum.binding.cryptocurrency.PredefinedServiceParameters.SERVICE_ID;
import static com.exonum.binding.cryptocurrency.PredefinedServiceParameters.SERVICE_NAME;
import static com.exonum.binding.cryptocurrency.PredefinedServiceParameters.artifactsDirectory;
import static com.exonum.binding.cryptocurrency.TransactionError.WALLET_ALREADY_EXISTS;
import static com.exonum.binding.cryptocurrency.TransactionUtils.DEFAULT_INITIAL_BALANCE;
import static com.exonum.binding.cryptocurrency.TransactionUtils.newCreateWalletTransaction;
import static com.exonum.binding.cryptocurrency.TransferTxIntegrationTest.checkServiceError;
import static org.assertj.core.api.Assertions.assertThat;

import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.core.blockchain.Blockchain;
import com.exonum.binding.core.blockchain.BlockchainData;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.storage.indices.MapIndex;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.testkit.TestKit;
import com.exonum.binding.testkit.TestKitExtension;
import com.exonum.core.messages.Runtime.ExecutionStatus;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class CreateWalletTxIntegrationTest {

  @RegisterExtension
  TestKitExtension testKitExtension = new TestKitExtension(
      TestKit.builder()
          .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
          .withService(ARTIFACT_ID, SERVICE_NAME, SERVICE_ID)
          .withArtifactsDirectory(artifactsDirectory));

  private static final KeyPair OWNER_KEY_PAIR = PredefinedOwnerKeys.FIRST_OWNER_KEY_PAIR;

  @Test
  @RequiresNativeLibrary
  void executeCreateWalletTx(TestKit testKit) {
    TransactionMessage transactionMessage =
        newCreateWalletTransaction(DEFAULT_INITIAL_BALANCE, OWNER_KEY_PAIR, SERVICE_ID);
    testKit.createBlockWithTransactions(transactionMessage);

    // Check that entries have been added
    BlockchainData blockchainData = testKit.getBlockchainData(SERVICE_NAME);
    CryptocurrencySchema schema =
        new CryptocurrencySchema(blockchainData.getExecutingServiceData());
    MapIndex<PublicKey, Wallet> wallets = schema.wallets();

    PublicKey emulatedNodePublicKey = OWNER_KEY_PAIR.getPublicKey();
    assertThat(wallets.containsKey(emulatedNodePublicKey)).isTrue();
    assertThat(wallets.get(emulatedNodePublicKey).getBalance())
        .isEqualTo(DEFAULT_INITIAL_BALANCE);
  }

  @Test
  @RequiresNativeLibrary
  void executeAlreadyExistingWalletTx(TestKit testKit) {
    // Create a new wallet
    TransactionMessage transactionMessage =
        newCreateWalletTransaction(DEFAULT_INITIAL_BALANCE, OWNER_KEY_PAIR, SERVICE_ID);
    testKit.createBlockWithTransactions(transactionMessage);

    // Attempt to execute a transaction with the same owner public key.
    // Use different balance so that it is not rejected as a duplicate
    TransactionMessage transactionMessage2 =
        newCreateWalletTransaction(DEFAULT_INITIAL_BALANCE * 2, OWNER_KEY_PAIR, SERVICE_ID);
    testKit.createBlockWithTransactions(transactionMessage2);

    // Check that the second tx has failed
    Snapshot view = testKit.getSnapshot();
    Blockchain blockchain = Blockchain.newInstance(view);
    Optional<ExecutionStatus> txResult = blockchain.getTxResult(transactionMessage2.hash());
    checkServiceError(txResult, WALLET_ALREADY_EXISTS);
  }
}
