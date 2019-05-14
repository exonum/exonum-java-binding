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

import static com.exonum.binding.common.hash.Hashing.sha256;
import static com.exonum.binding.common.serialization.json.JsonSerializer.json;
import static com.exonum.binding.qaservice.transactions.CreateCounterTx.converter;
import static com.exonum.binding.qaservice.transactions.QaTransaction.CREATE_COUNTER;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import nl.jqno.equalsverifier.EqualsVerifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class CreateCounterTxIntegrationTest {

  private static final CryptoFunction CRYPTO_FUNCTION = CryptoFunctions.ed25519();

  static {
    LibraryLoader.load();
  }

  private ListAppender logAppender;

  @BeforeEach
  void setUp() {
    logAppender = getCapturingLogAppender();
  }

  private static ListAppender getCapturingLogAppender() {
    LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
    Configuration config = ctx.getConfiguration();
    ListAppender appender = (ListAppender) config.getAppenders().get("ListAppender");
    // Clear the appender so that it doesn't contain entries from the previous tests.
    appender.clear();
    return appender;
  }

  @AfterEach
  void tearDown() {
    logAppender.clear();
  }

  @Test
  void converterRejectsWrongServiceId() {
    RawTransaction tx = txTemplate()
        .serviceId((short) -1)
        .build();

    assertThrows(IllegalArgumentException.class, () -> converter().fromRawTransaction(tx));
  }

  @Test
  void converterRejectsWrongTxId() {
    RawTransaction tx = txTemplate()
        .transactionId((short) -1)
        .build();

    assertThrows(IllegalArgumentException.class, () -> converter().fromRawTransaction(tx));
  }

  @Test
  void converterRoundtrip() {
    String name = "counter";
    CreateCounterTx tx = new CreateCounterTx(name);

    RawTransaction raw = converter().toRawTransaction(tx);
    CreateCounterTx txFromRaw = converter().fromRawTransaction(raw);

    assertThat(txFromRaw).isEqualTo(tx);
  }

  @Test
  void rejectsEmptyName() {
    String name = "";

    Exception e = assertThrows(IllegalArgumentException.class,
        () -> new CreateCounterTx(name));
    assertThat(e.getMessage()).contains("Name must not be blank");
  }

  @Test
  @RequiresNativeLibrary
  void executeNewCounter() {
    try (TestKit testKit = TestKit.forService(QaServiceModule.class)) {
      QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
      String counterName = "counter";
      service.submitCreateCounter(counterName);
      testKit.createBlock();

      testKit.withSnapshot((view) -> {
        QaSchema schema = new QaSchema(view);
        MapIndex<HashCode, Long> counters = schema.counters();
        MapIndex<HashCode, String> counterNames = schema.counterNames();
        HashCode counterId = sha256().hashString(counterName, UTF_8);

        assertThat(counters.get(counterId)).isEqualTo(0L);
        assertThat(counterNames.get(counterId)).isEqualTo(counterName);
        return null;
      });
    }
  }

  @Test
  @RequiresNativeLibrary
  void executeAlreadyExistingCounter() {
    try (TestKit testKit = TestKit.forService(QaServiceModule.class)) {
      String counterName = "counter";
      TransactionMessage transactionMessage = createCreateCounterTransaction(counterName);
      TransactionMessage transactionMessage2 = createCreateCounterTransaction(counterName);
      testKit.createBlockWithTransactions(transactionMessage);
      testKit.createBlockWithTransactions(transactionMessage2);

      List<String> logMessages = logAppender.getMessages();
      // Logger contains two #getStateHashes messages and an exception message
      int expectedNumMessages = 3;
      assertThat(logMessages).hasSize(expectedNumMessages);

      String exceptionMessage = "com.exonum.binding.transaction.TransactionExecutionException";
      assertThat(logMessages.get(1))
          .contains("INFO")
          .contains(exceptionMessage);
    }
  }

  @Test
  void info() {
    String name = "counter";
    CreateCounterTx tx = new CreateCounterTx(name);

    String info = tx.info();

    AnyTransaction<CreateCounterTx> txParams = json().fromJson(info,
        new TypeToken<AnyTransaction<CreateCounterTx>>(){}.getType()
    );
    assertThat(txParams.service_id).isEqualTo(QaService.ID);
    assertThat(txParams.message_id).isEqualTo(CREATE_COUNTER.id());
    assertThat(txParams.body).isEqualTo(tx);
  }

  @Test
  void equals() {
    EqualsVerifier.forClass(CreateCounterTx.class)
        .verify();
  }

  private TransactionMessage createCreateCounterTransaction(String counterName) {
    CreateCounterTx createCounterTx = new CreateCounterTx(counterName);
    RawTransaction rawTransaction = createCounterTx.toRawTransaction();
    return toTransactionMessage(rawTransaction);
  }

  private TransactionMessage toTransactionMessage(RawTransaction rawTransaction) {
    return TransactionMessage.builder()
        .serviceId(rawTransaction.getServiceId())
        .transactionId(rawTransaction.getTransactionId())
        .payload(rawTransaction.getPayload())
        .sign(CRYPTO_FUNCTION.generateKeyPair(), CRYPTO_FUNCTION);
  }

  private static RawTransaction.Builder txTemplate() {
    return RawTransaction.newBuilder()
        .transactionId(CREATE_COUNTER.id())
        .serviceId(QaService.ID)
        .payload(Bytes.bytes());
  }
}
