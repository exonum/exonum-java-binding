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

import static com.exonum.binding.qaservice.transactions.ValidThrowingTx.serializeBody;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.qaservice.QaService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class ValidThrowingTxTest {

  static final Message MESSAGE_TEMPLATE = new Message.Builder()
      .mergeFrom(Transactions.QA_TX_MESSAGE_TEMPLATE)
      .setMessageType(QaTransaction.VALID_THROWING.id())
      .setBody(serializeBody(new ValidThrowingTx(1L)))
      .buildPartial();

  @Test
  void converterRoundtrip() {
    long seed = 10L;
    ValidThrowingTx tx = new ValidThrowingTx(seed);

    BinaryMessage message = ValidThrowingTx.converter().toMessage(tx);

    ValidThrowingTx txFromMessage = ValidThrowingTx.converter().fromMessage(message);

    assertThat(txFromMessage, equalTo(tx));
  }

  @Test
  void isValid() {
    long seed = 10L;
    ValidThrowingTx tx = new ValidThrowingTx(seed);

    assertTrue(tx.isValid());
  }

  @Test
  void info() {
    long seed = 10L;
    ValidThrowingTx tx = new ValidThrowingTx(seed);
    String info = tx.info();

    Gson gson = QaTransactionGson.instance();
    AnyTransaction<ValidThrowingTx> txParams = gson.fromJson(info,
        new TypeToken<AnyTransaction<ValidThrowingTx>>(){}.getType());

    assertThat(txParams.service_id, equalTo(QaService.ID));
    assertThat(txParams.message_id, equalTo(QaTransaction.VALID_THROWING.id()));
    assertThat(txParams.body, equalTo(tx));
  }

  @Test
  void equals() {
    EqualsVerifier.forClass(ValidThrowingTx.class)
        .verify();
  }
}
