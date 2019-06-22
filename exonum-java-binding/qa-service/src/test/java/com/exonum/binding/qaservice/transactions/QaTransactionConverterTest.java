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
import static com.exonum.binding.test.Bytes.bytes;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.core.transaction.RawTransaction;
import com.exonum.binding.core.transaction.Transaction;
import com.exonum.binding.qaservice.QaService;
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
    RawTransaction tx = RawTransaction.newBuilder()
        .serviceId((short) (QaService.ID + 1))
        .transactionId(QaTransaction.INCREMENT_COUNTER.id())
        .payload(bytes())
        .build();

    Exception e = assertThrows(IllegalArgumentException.class,
        () -> converter.toTransaction(tx));
    assertThat(e).hasMessageMatching("This transaction \\(.+\\) does not belong "
        + "to this service: wrong service id \\(\\d+\\), must be " + QaService.ID);
  }

  @Test
  void toTransactionUnknownTransaction() {
    RawTransaction raw = UnknownTx.createRawTransaction();

    Exception e = assertThrows(IllegalArgumentException.class,
        () -> converter.toTransaction(raw));
    assertThat(e).hasMessageStartingWith("Unknown transaction");
  }

  @ParameterizedTest
  @MethodSource("transactions")
  void toTransaction(Class<? extends Transaction> expectedType, RawTransaction tx) {
    Transaction transaction = converter.toTransaction(tx);

    assertThat(transaction).isInstanceOf(expectedType);
  }

  private static Collection<Arguments> transactions() {
    List<Arguments> transactionTemplates = asList(
        Arguments.of(CreateCounterTx.class,
            new CreateCounterTx("name").toRawTransaction()),

        Arguments.of(IncrementCounterTx.class,
            new IncrementCounterTx(10L, defaultHashFunction().hashString("name", UTF_8))
                .toRawTransaction()),

        Arguments.of(ThrowingTx.class,
            new ThrowingTx(10L).toRawTransaction()),

        Arguments.of(ErrorTx.class,
            new ErrorTx(10L, (byte) 1, "some error").toRawTransaction())
    );

    // Check that the test data includes all known transactions.
    assertThat(transactionTemplates).hasSameSizeAs(QaTransaction.values());

    return transactionTemplates;
  }

}
