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

import static com.exonum.binding.common.hash.Hashing.defaultHashFunction;
import static com.exonum.binding.common.hash.Hashing.sha256;
import static com.exonum.binding.common.serialization.json.JsonSerializer.json;
import static com.exonum.binding.qaservice.transactions.IncrementCounterTx.converter;
import static com.exonum.binding.qaservice.transactions.QaTransaction.INCREMENT_COUNTER;
import static com.exonum.binding.qaservice.transactions.TransactionError.UNKNOWN_COUNTER;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.blockchain.Blockchain;
import com.exonum.binding.common.blockchain.TransactionResult;
import com.exonum.binding.common.crypto.CryptoFunction;
import com.exonum.binding.common.crypto.CryptoFunctions;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.qaservice.QaSchema;
import com.exonum.binding.qaservice.QaService;
import com.exonum.binding.qaservice.QaServiceImpl;
import com.exonum.binding.qaservice.QaServiceModule;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.test.Bytes;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.testkit.TestKit;
import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.util.LibraryLoader;
import com.google.gson.reflect.TypeToken;
import java.util.Optional;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class IncrementCounterTxIntegrationTest {

  private static final CryptoFunction CRYPTO_FUNCTION = CryptoFunctions.ed25519();

  static {
    LibraryLoader.load();
  }

  @Test
  void converterRejectsWrongServiceId() {
    RawTransaction tx = txTemplate()
        .serviceId((short) -1)
        .build();

    assertThrows(IllegalArgumentException.class,
        () -> converter().fromRawTransaction(tx));
  }

  @Test
  void converterRejectsWrongTxId() {
    RawTransaction tx = txTemplate()
        .transactionId((short) -1)
        .build();

    assertThrows(IllegalArgumentException.class,
        () -> converter().fromRawTransaction(tx));
  }

  @Test
  void converterRoundtrip() {
    long seed = 0;
    HashCode counterId = sha256().hashInt(0);

    IncrementCounterTx tx = new IncrementCounterTx(seed, counterId);
    RawTransaction raw = converter().toRawTransaction(tx);
    IncrementCounterTx txFromRaw = converter().fromRawTransaction(raw);

    assertThat(txFromRaw).isEqualTo(tx);
  }

  @Test
  @RequiresNativeLibrary
  void executeIncrementsCounter() {
    try (TestKit testKit = TestKit.forService(QaServiceModule.class)) {
      QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);

      // Add a new counter with the given name and initial value
      String counterName = "counter";
      service.submitCreateCounter(counterName);
      testKit.createBlock();

      // Submit and execute the transaction
      long seed = 0L;
      HashCode counterId = sha256().hashString(counterName, UTF_8);
      service.submitIncrementCounter(seed, counterId);
      testKit.createBlock();

      testKit.withSnapshot((view) -> {
        // Check the counter has an incremented value
        QaSchema schema = new QaSchema(view);
        MapIndex<HashCode, Long> counters = schema.counters();
        long expectedValue = 1;

        assertThat(counters.get(counterId)).isEqualTo(expectedValue);
        return null;
      });
    }
  }

  @Test
  @RequiresNativeLibrary
  void executeNoSuchCounter() {
    try (TestKit testKit = TestKit.forService(QaServiceModule.class)) {
      String counterName = "unknown-counter";
      HashCode counterId = defaultHashFunction().hashString(counterName, UTF_8);
      TransactionMessage incrementCounterTx = createIncrementCounterTransaction(0L, counterId);
      testKit.createBlockWithTransactions(incrementCounterTx);

      testKit.withSnapshot((view) -> {
        Blockchain blockchain = Blockchain.newInstance(view);
        Optional<TransactionResult> txResult = blockchain.getTxResult(incrementCounterTx.hash());
        TransactionResult expectedTransactionResult =
            TransactionResult.error(UNKNOWN_COUNTER.code, null);
        assertThat(txResult).hasValue(expectedTransactionResult);
        return null;
      });
    }
  }

  @Test
  void info() {
    // Create a transaction with the given parameters.
    long seed = Long.MAX_VALUE - 1;
    String name = "new_counter";
    HashCode nameHash = defaultHashFunction().hashString(name, UTF_8);
    IncrementCounterTx tx = new IncrementCounterTx(seed, nameHash);

    String info = tx.info();

    // Check the transaction parameters in JSON
    AnyTransaction<IncrementCounterTx> txParameters = json().fromJson(info,
        new TypeToken<AnyTransaction<IncrementCounterTx>>(){}.getType());

    assertThat(txParameters.body).isEqualTo(tx);
  }

  @Test
  void equals() {
    EqualsVerifier.forClass(IncrementCounterTx.class)
        .withPrefabValues(HashCode.class, HashCode.fromInt(1), HashCode.fromInt(2))
        .verify();
  }

  private static RawTransaction.Builder txTemplate() {
    return RawTransaction.newBuilder()
        .transactionId(INCREMENT_COUNTER.id())
        .serviceId(QaService.ID)
        .payload(Bytes.bytes());
  }

  private TransactionMessage createIncrementCounterTransaction(long seed, HashCode counterId) {
    IncrementCounterTx incrementCounterTx = new IncrementCounterTx(seed, counterId);
    RawTransaction rawTransaction = incrementCounterTx.toRawTransaction();
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
