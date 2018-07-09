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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.text.MatchesPattern.matchesPattern;

import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.qaservice.QaService;
import com.google.common.collect.ImmutableMap;
import java.nio.ByteBuffer;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class QaTransactionConverterTest {

  @Rule public ExpectedException expectedException = ExpectedException.none();

  private QaTransactionConverter converter;

  @Before
  public void setUp() {
    converter = new QaTransactionConverter();
  }

  @Test
  public void hasFactoriesForEachTransaction() {
    // Check that the QaTransaction enum is kept in sync with the map of transaction factories,
    // i.e., each transaction type is mapped to the corresponding factory.
    for (QaTransaction tx : QaTransaction.values()) {
      short id = tx.id();

      assertThat(QaTransactionConverter.TRANSACTION_FACTORIES)
          .as("No entry for transaction %s with id=%d", tx, id)
          .containsKey(id);
    }
  }

  @Test
  public void toTransactionTransactionOfAnotherService() {
    BinaryMessage message = new Message.Builder()
        .setServiceId((short) (QaService.ID + 1))
        .setMessageType(QaTransaction.INCREMENT_COUNTER.id())
        .setBody(ByteBuffer.allocate(0))
        .setSignature(new byte[Message.SIGNATURE_SIZE])
        .buildRaw();

    expectedException.expectMessage(matchesPattern(
        "Wrong service id \\(\\d+\\), must be " + QaService.ID));
    expectedException.expect(IllegalArgumentException.class);
    converter.toTransaction(message);
  }

  @Test
  public void toTransactionUnknownTransaction() {
    UnknownTx unknownTx = new UnknownTx();
    BinaryMessage message = unknownTx.getMessage();

    expectedException.expectMessage("Unknown transaction");
    expectedException.expect(IllegalArgumentException.class);
    converter.toTransaction(message);
  }

  @Test
  public void toTransaction() {
    Map<Class<? extends Transaction>, Message> transactionTemplates = ImmutableMap.of(
        CreateCounterTx.class, CreateCounterTxIntegrationTest.CREATE_COUNTER_MESSAGE_TEMPLATE,
        IncrementCounterTx.class, IncrementCounterTxIntegrationTest.INC_COUNTER_TX_MESSAGE_TEMPLATE,
        InvalidThrowingTx.class, new Message.Builder()
            .mergeFrom(Transactions.QA_TX_MESSAGE_TEMPLATE)
            .setMessageType(QaTransaction.INVALID_THROWING.id())
            .buildRaw(),
        InvalidTx.class, new Message.Builder()
            .mergeFrom(Transactions.QA_TX_MESSAGE_TEMPLATE)
            .setMessageType(QaTransaction.INVALID.id())
            .buildRaw(),
        ValidThrowingTx.class, ValidThrowingTxTest.VALID_THROWING_TEMPLATE
    );

    // Check that the test data includes all known transactions.
    assertThat(transactionTemplates).hasSameSizeAs(QaTransaction.values());

    // Check it converts a valid binary message of each known type
    // to the expected executable transaction type.
    for (Map.Entry<Class<? extends Transaction>, Message> classMessageEntry :
        transactionTemplates.entrySet()) {

      BinaryMessage message = new Message.Builder()
          .mergeFrom(classMessageEntry.getValue())
          .buildRaw();

      Transaction transaction = converter.toTransaction(message);

      Class<? extends Transaction> expectedType = classMessageEntry.getKey();
      assertThat(transaction)
          .isInstanceOf(expectedType);
    }
  }
}
