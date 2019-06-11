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
import static com.exonum.binding.cryptocurrency.transactions.TransactionError.WALLET_ALREADY_EXISTS;
import static com.exonum.binding.cryptocurrency.transactions.TransactionUtils.DEFAULT_INITIAL_BALANCE;
import static com.exonum.binding.cryptocurrency.transactions.TransactionUtils.newCreateWalletRawTransaction;
import static com.exonum.binding.cryptocurrency.transactions.TransactionUtils.newCreateWalletTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.blockchain.Blockchain;
import com.exonum.binding.common.blockchain.TransactionResult;
import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.cryptocurrency.CryptocurrencyServiceModule;
import com.exonum.binding.cryptocurrency.PredefinedOwnerKeys;
import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.testkit.TestKit;
import com.exonum.binding.testkit.TestKitExtension;
import com.exonum.binding.transaction.RawTransaction;
import java.util.Optional;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD) // Testkit extension is also broken in parallel mode
class CreateWalletTxTest {

  @RegisterExtension
  TestKitExtension testKitExtension = new TestKitExtension(
      TestKit.builder()
          .withService(CryptocurrencyServiceModule.class));

  private static final KeyPair OWNER_KEY_PAIR = PredefinedOwnerKeys.FIRST_OWNER_KEY_PAIR;

  @Test
  void fromRawTransaction() {
    long initialBalance = 100L;
    RawTransaction raw = newCreateWalletRawTransaction(initialBalance);

    CreateWalletTx tx = CreateWalletTx.fromRawTransaction(raw);

    assertThat(tx).isEqualTo(new CreateWalletTx(initialBalance));
  }

  @Test
  void constructorRejectsNegativeBalance() {
    long initialBalance = -1L;

    Throwable t = assertThrows(IllegalArgumentException.class,
        () -> new CreateWalletTx(initialBalance));

    assertThat(t.getMessage()).isEqualTo("The initial balance (-1) must not be negative.");
  }

  @Test
  @RequiresNativeLibrary
  void executeCreateWalletTx(TestKit testKit) {
    TransactionMessage transactionMessage =
        newCreateWalletTransaction(DEFAULT_INITIAL_BALANCE, OWNER_KEY_PAIR);
    testKit.createBlockWithTransactions(transactionMessage);

    testKit.withSnapshot((view) -> {
      // Check that entries have been added
      CryptocurrencySchema schema = new CryptocurrencySchema(view);
      MapIndex<PublicKey, Wallet> wallets = schema.wallets();

      PublicKey emulatedNodePublicKey = OWNER_KEY_PAIR.getPublicKey();
      assertThat(wallets.containsKey(emulatedNodePublicKey)).isTrue();
      assertThat(wallets.get(emulatedNodePublicKey).getBalance())
          .isEqualTo(DEFAULT_INITIAL_BALANCE);
      return null;
    });
  }

  @Test
  @RequiresNativeLibrary
  void executeAlreadyExistingWalletTx(TestKit testKit) {
    // Create a new wallet
    TransactionMessage transactionMessage =
        newCreateWalletTransaction(DEFAULT_INITIAL_BALANCE, OWNER_KEY_PAIR);
    testKit.createBlockWithTransactions(transactionMessage);

    // Attempt to execute a transaction with the same owner public key.
    // Use different balance so that it is not rejected as a duplicate
    TransactionMessage transactionMessage2 =
        newCreateWalletTransaction(DEFAULT_INITIAL_BALANCE * 2, OWNER_KEY_PAIR);
    testKit.createBlockWithTransactions(transactionMessage2);

    // Check that the second tx has failed
    testKit.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      Optional<TransactionResult> txResult = blockchain.getTxResult(transactionMessage2.hash());
      TransactionResult expectedTransactionResult =
          TransactionResult.error(WALLET_ALREADY_EXISTS.errorCode, null);
      assertThat(txResult).hasValue(expectedTransactionResult);
      return null;
    });
  }

  @Test
  void info() {
    CreateWalletTx tx = new CreateWalletTx(DEFAULT_INITIAL_BALANCE);

    String info = tx.info();

    CreateWalletTx txParams = json()
        .fromJson(info, CreateWalletTx.class);

    assertThat(txParams).isEqualTo(tx);
  }

  @Test
  void verifyEquals() {
    EqualsVerifier
        .forClass(CreateWalletTx.class)
        .verify();
  }

}
