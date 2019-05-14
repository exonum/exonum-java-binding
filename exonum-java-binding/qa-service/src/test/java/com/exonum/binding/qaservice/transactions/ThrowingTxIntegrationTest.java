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

package com.exonum.binding.qaservice.transactions;

import static com.exonum.binding.common.serialization.json.JsonSerializer.json;
import static org.assertj.core.api.Assertions.assertThat;

import com.exonum.binding.blockchain.Blockchain;
import com.exonum.binding.common.blockchain.TransactionResult;
import com.exonum.binding.common.crypto.CryptoFunction;
import com.exonum.binding.common.crypto.CryptoFunctions;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.qaservice.QaService;
import com.exonum.binding.qaservice.QaServiceModule;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.testkit.TestKit;
import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.util.LibraryLoader;
import com.google.gson.reflect.TypeToken;
import java.util.Optional;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class ThrowingTxIntegrationTest {

  private static final CryptoFunction CRYPTO_FUNCTION = CryptoFunctions.ed25519();

  static {
    LibraryLoader.load();
  }

  @Test
  void converterRoundtrip() {
    long seed = 10L;
    ThrowingTx tx = new ThrowingTx(seed);

    RawTransaction message = ThrowingTx.converter().toRawTransaction(tx);

    ThrowingTx txFromRaw = ThrowingTx.converter().fromRawTransaction(message);

    assertThat(txFromRaw).isEqualTo(tx);
  }

  @Test
  void info() {
    long seed = 10L;
    ThrowingTx tx = new ThrowingTx(seed);
    String info = tx.info();

    AnyTransaction<ThrowingTx> txParams = json().fromJson(info,
        new TypeToken<AnyTransaction<ThrowingTx>>(){}.getType());

    assertThat(txParams.service_id).isEqualTo(QaService.ID);
    assertThat(txParams.message_id).isEqualTo(QaTransaction.VALID_THROWING.id());
    assertThat(txParams.body).isEqualTo(tx);
  }

  @Test
  @RequiresNativeLibrary
  void executeThrows() {
    try (TestKit testKit = TestKit.forService(QaServiceModule.class)) {
      TransactionMessage throwingTx = createThrowingTransaction(0L);
      testKit.createBlockWithTransactions(throwingTx);

      testKit.withSnapshot((view) -> {
        Blockchain blockchain = Blockchain.newInstance(view);
        Optional<TransactionResult> txResult = blockchain.getTxResult(throwingTx.hash());
        assertThat(txResult).isNotEmpty();
        TransactionResult transactionResult = txResult.get();
        assertThat(transactionResult.getType()).isEqualTo(TransactionResult.Type.UNEXPECTED_ERROR);
        assertThat(transactionResult.getErrorCode()).isEmpty();
        assertThat(transactionResult.getErrorDescription())
            .contains("#execute of this transaction always throws");
        return null;
      });
    }
  }

  @Test
  void equals() {
    EqualsVerifier.forClass(ThrowingTx.class)
        .verify();
  }

  private TransactionMessage createThrowingTransaction(long seed) {
    ThrowingTx throwingTx = new ThrowingTx(seed);
    RawTransaction rawTransaction = throwingTx.toRawTransaction();
    return toTransactionMessage(rawTransaction);
  }

  private TransactionMessage toTransactionMessage(RawTransaction rawTransaction) {
    return TransactionMessage.builder()
        .serviceId(rawTransaction.getServiceId())
        .transactionId(rawTransaction.getTransactionId())
        .payload(rawTransaction.getPayload())
        .sign(CRYPTO_FUNCTION.generateKeyPair(), CRYPTO_FUNCTION);
  }
}
