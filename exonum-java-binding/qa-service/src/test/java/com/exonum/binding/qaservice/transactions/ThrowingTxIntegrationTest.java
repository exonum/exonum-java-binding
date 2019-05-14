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

import com.exonum.binding.qaservice.QaService;
import com.exonum.binding.qaservice.QaServiceImpl;
import com.exonum.binding.qaservice.QaServiceModule;
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

class ThrowingTxIntegrationTest {

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
      QaServiceImpl service = testKit.getService(QaService.ID, QaServiceImpl.class);
      service.submitValidThrowingTx(1L);
      testKit.createBlock();
      List<String> logMessages = logAppender.getMessages();
      // Logger contains two #getStateHashes messages and an exception message
      int expectedNumMessages = 3;
      assertThat(logMessages).hasSize(expectedNumMessages);

      String exceptionMessage = "#execute of this transaction always throws";
      assertThat(logMessages.get(1))
          .contains("ERROR")
          .contains(exceptionMessage);
    }
  }

  @Test
  void equals() {
    EqualsVerifier.forClass(ThrowingTx.class)
        .verify();
  }
}
