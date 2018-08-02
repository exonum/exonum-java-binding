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

import static com.exonum.binding.qaservice.transactions.QaTransaction.INCREMENT_COUNTER;
import static com.exonum.binding.qaservice.transactions.ValidErrorTx.serializeBody;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.messages.TransactionExecutionException;
import com.exonum.binding.qaservice.QaService;
import com.exonum.binding.storage.database.Fork;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ValidErrorTxTest {

  static Message MESSAGE_TEMPLATE = new Message.Builder()
      .mergeFrom(Transactions.QA_TX_MESSAGE_TEMPLATE)
      .setMessageType(QaTransaction.VALID_ERROR.id())
      .setBody(serializeBody(new ValidErrorTx(0L, (byte) 1, "Boom")))
      .buildPartial();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void converterFromMessageRejectsWrongServiceId() {
    BinaryMessage message = new Message.Builder()
        .mergeFrom(MESSAGE_TEMPLATE)
        .setServiceId((short) (QaService.ID + 1))
        .buildRaw();

    expectedException.expect(IllegalArgumentException.class);
    IncrementCounterTx.converter().fromMessage(message);
  }

  @Test
  public void converterFromMessageRejectsWrongTxId() {
    BinaryMessage message = new Message.Builder()
        .mergeFrom(MESSAGE_TEMPLATE)
        .setMessageType((short) (INCREMENT_COUNTER.id() + 1))
        .buildRaw();

    expectedException.expect(IllegalArgumentException.class);
    IncrementCounterTx.converter().fromMessage(message);
  }

  @Test
  public void converterRoundtrip() {
    ValidErrorTx tx = new ValidErrorTx(1L, (byte) 2, "Foo");

    BinaryMessage txMessage = ValidErrorTx.converter().toMessage(tx);
    ValidErrorTx txFromMessage = ValidErrorTx.converter().fromMessage(txMessage);

    assertThat(txFromMessage, equalTo(tx));
  }

  @Test
  public void constructorRejectsInvalidErrorCode() {
    byte invalidErrorCode = -1;
    expectedException.expect(IllegalArgumentException.class);
    new ValidErrorTx(1L, invalidErrorCode, "Boom");
  }

  @Test
  public void constructorRejectsInvalidDescription() {
    String invalidDescription = "";
    expectedException.expect(IllegalArgumentException.class);
    new ValidErrorTx(1L, (byte) 1, invalidDescription);
  }

  @Test
  public void isValid() {
    ValidErrorTx tx = new ValidErrorTx(1L, (byte) 2, "Boom");
    assertTrue(tx.isValid());
  }

  @Test
  public void executeNoDescription() {
    byte errorCode = 2;
    Transaction tx = new ValidErrorTx(1L, errorCode, null);

    try {
      tx.execute(mock(Fork.class));
      fail("Must throw");
    } catch (TransactionExecutionException expected) {
      assertThat(expected.getErrorCode(), equalTo(errorCode));
      assertNull(expected.getMessage());
    }
  }

  @Test
  public void executeWithDescription() {
    byte errorCode = 2;
    String description = "Boom";
    Transaction tx = new ValidErrorTx(1L, errorCode, description);

    try {
      tx.execute(mock(Fork.class));
      fail("Must throw");
    } catch (TransactionExecutionException expected) {
      assertThat(expected.getErrorCode(), equalTo(errorCode));
      assertThat(expected.getMessage(), equalTo(description));
    }
  }

  @Test
  public void equals() {
    EqualsVerifier.forClass(ValidErrorTx.class)
        .verify();
  }
}
