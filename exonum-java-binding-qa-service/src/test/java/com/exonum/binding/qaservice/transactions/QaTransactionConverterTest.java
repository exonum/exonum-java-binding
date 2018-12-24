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

import static com.exonum.binding.test.Bytes.bytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.qaservice.QaService;
import com.exonum.binding.transaction.RawTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

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

}
