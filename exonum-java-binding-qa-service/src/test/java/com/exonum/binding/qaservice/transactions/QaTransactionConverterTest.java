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
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.message.BinaryMessage;
import com.exonum.binding.common.message.Message;
import com.exonum.binding.qaservice.QaService;
import com.exonum.binding.transaction.Transaction;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class QaTransactionConverterTest {

  private QaTransactionConverter converter;

  @BeforeEach
  void setUp() {
    converter = new QaTransactionConverter();
  }

  @ParameterizedTest
  @EnumSource(QaTransaction.class)
  void hasFactoriesForEachTransaction(QaTransaction tx) {
    // Check that the QaTransaction enum is kept in sync with the map of transaction factories,
    // i.e., each transaction type is mapped to the corresponding factory.
    short id = tx.id();

    assertThat(QaTransactionConverter.TRANSACTION_FACTORIES)
            .as("No entry for transaction %s with id=%d", tx, id)
            .containsKey(id);
  }

  @Test
  void toTransactionTransactionOfAnotherService() {
    BinaryMessage message = new Message.Builder()
        .setServiceId((short) (QaService.ID + 1))
        .setMessageType(QaTransaction.INCREMENT_COUNTER.id())
        .setBody(ByteBuffer.allocate(0))
        .setSignature(new byte[Message.SIGNATURE_SIZE])
        .buildRaw();

    Exception e = assertThrows(IllegalArgumentException.class,
        () -> converter.toTransaction(message));
    assertThat(e).hasMessageMatching("Wrong service id \\(\\d+\\), must be "
        + QaService.ID);
  }

  @Test
  void toTransactionUnknownTransaction() {
    UnknownTx unknownTx = new UnknownTx();
    BinaryMessage message = unknownTx.getMessage();

    Exception e = assertThrows(IllegalArgumentException.class,
        () -> converter.toTransaction(message));
    assertThat(e).hasMessageStartingWith("Unknown transaction");
  }

  @ParameterizedTest
  @MethodSource("transactionMessages")
  void toTransaction(Class<? extends Transaction> expectedType, Message messageTemplate) {
    // Check the converter converts a valid binary message of each known type
    // to the expected executable transaction type.
    BinaryMessage message = new Message.Builder()
        .mergeFrom(messageTemplate)
        .buildRaw();

    Transaction transaction = converter.toTransaction(message);

    assertThat(transaction)
        .isInstanceOf(expectedType);
  }

  private static Collection<Arguments> transactionMessages() {
    List<Arguments> transactionTemplates = Arrays.asList(
        Arguments.of(CreateCounterTx.class,
            CreateCounterTxIntegrationTest.MESSAGE_TEMPLATE),

        Arguments.of(IncrementCounterTx.class,
            IncrementCounterTxIntegrationTest.MESSAGE_TEMPLATE),

        Arguments.of(InvalidThrowingTx.class, new Message.Builder()
            .mergeFrom(Transactions.QA_TX_MESSAGE_TEMPLATE)
            .setMessageType(QaTransaction.INVALID_THROWING.id())
            .buildRaw()),

        Arguments.of(InvalidTx.class, new Message.Builder()
            .mergeFrom(Transactions.QA_TX_MESSAGE_TEMPLATE)
            .setMessageType(QaTransaction.INVALID.id())
            .buildRaw()),

        Arguments.of(ValidThrowingTx.class, ValidThrowingTxIntegrationTest.MESSAGE_TEMPLATE),

        Arguments.of(ValidErrorTx.class, ValidErrorTxIntegrationTest.MESSAGE_TEMPLATE)
    );

    // Check that the test data includes all known transactions.
    assertThat(transactionTemplates).hasSameSizeAs(QaTransaction.values());

    return transactionTemplates;
  }
}
