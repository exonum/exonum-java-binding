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
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.crypto.CryptoFunction;
import com.exonum.binding.common.crypto.CryptoFunctions;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.qaservice.QaService;
import com.exonum.binding.qaservice.QaServiceModule;
import com.exonum.binding.test.Bytes;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.testkit.TestKit;
import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.transaction.Transaction;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import javax.annotation.Nullable;
import java.util.List;

class ErrorTxIntegrationTest {

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

    assertThrows(IllegalArgumentException.class,
        () -> IncrementCounterTx.converter().fromRawTransaction(tx));
  }

  @Test
  void converterRejectsWrongTxId() {
    RawTransaction tx = txTemplate()
        .transactionId((short) -1)
        .build();

    assertThrows(IllegalArgumentException.class,
        () -> IncrementCounterTx.converter().fromRawTransaction(tx));
  }

  @Test
  void converterRoundtrip() {
    ErrorTx tx = new ErrorTx(1L, (byte) 2, "Foo");

    RawTransaction raw = ErrorTx.converter().toRawTransaction(tx);
    ErrorTx txFromRaw = ErrorTx.converter().fromRawTransaction(raw);

    assertThat(txFromRaw).isEqualTo(equalTo(tx));
  }

  @Test
  void constructorRejectsInvalidErrorCode() {
    byte invalidErrorCode = -1;
    assertThrows(IllegalArgumentException.class,
        () -> new ErrorTx(1L, invalidErrorCode, "Boom"));
  }

  @Test
  void constructorRejectsInvalidDescription() {
    String invalidDescription = "";
    assertThrows(IllegalArgumentException.class,
        () -> new ErrorTx(1L, (byte) 1, invalidDescription));
  }

  @Test
  @RequiresNativeLibrary
  void executeNoDescription() {
    try (TestKit testKit = TestKit.forService(QaServiceModule.class)) {
      byte errorCode = 1;
      TransactionMessage errorTx = createErrorTransaction(0L, errorCode, null);
      testKit.createBlockWithTransactions(errorTx);
      List<String> logMessages = logAppender.getMessages();
      // Logger contains two #getStateHashes messages and an exception message
      int expectedNumMessages = 3;
      assertThat(logMessages).hasSize(expectedNumMessages);

      String exceptionMessage = "com.exonum.binding.transaction.TransactionExecutionException: null";
      assertThat(logMessages.get(1))
          .contains("INFO")
          .contains(exceptionMessage);
    }
  }

  @Test
  @RequiresNativeLibrary
  void executeWithDescription() {
    try (TestKit testKit = TestKit.forService(QaServiceModule.class)) {
      byte errorCode = 1;
      String errorDescription = "Test";
      TransactionMessage errorTx = createErrorTransaction(0L, errorCode, errorDescription);
      testKit.createBlockWithTransactions(errorTx);
      List<String> logMessages = logAppender.getMessages();
      // Logger contains two #getStateHashes messages and an exception message
      int expectedNumMessages = 3;
      assertThat(logMessages).hasSize(expectedNumMessages);

      String exceptionMessage =
          "com.exonum.binding.transaction.TransactionExecutionException: " + errorDescription;
      assertThat(logMessages.get(1))
          .contains("INFO")
          .contains(exceptionMessage);
    }
  }

  @CsvSource({
      "1, 0, Boom", // min error code value
      "-1, 1, 'Longer error message'",
      "9223372036854775807, 127,", // Max seed value, max error code value, null message
  })
  @ParameterizedTest
  void info(long seed, byte errorCode, String errorMessage) {
    Transaction tx = new ErrorTx(seed, errorCode, errorMessage);

    String txInJson = tx.info();

    AnyTransaction<ErrorTx> txFromJson = json().fromJson(txInJson,
        new TypeToken<AnyTransaction<ErrorTx>>(){}.getType());

    assertThat(txFromJson.message_id).isEqualTo(QaTransaction.VALID_ERROR.id());
    assertThat(txFromJson.body).isEqualTo(tx);
  }

  @Test
  void equals() {
    EqualsVerifier.forClass(ErrorTx.class)
        .verify();
  }

  private TransactionMessage createErrorTransaction(
      long seed, byte errorCode, @Nullable String errorDescription) {
    ErrorTx errorTx = new ErrorTx(seed, errorCode, errorDescription);
    RawTransaction rawTransaction = errorTx.toRawTransaction();
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
        .transactionId(QaTransaction.VALID_ERROR.id())
        .serviceId(QaService.ID)
        .payload(Bytes.bytes());
  }

}
