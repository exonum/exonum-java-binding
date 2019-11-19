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
import static com.exonum.binding.qaservice.transactions.QaTransaction.CREATE_COUNTER;
import static com.exonum.binding.qaservice.transactions.QaTransaction.INCREMENT_COUNTER;
import static com.exonum.binding.qaservice.transactions.QaTransaction.VALID_ERROR;
import static com.exonum.binding.qaservice.transactions.QaTransaction.VALID_THROWING;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.core.transaction.RawTransaction;
import com.exonum.binding.core.transaction.Transaction;
import com.exonum.binding.qaservice.transactions.TxMessageProtos.CreateCounterTxBody;
import com.exonum.binding.qaservice.transactions.TxMessageProtos.ErrorTxBody;
import com.exonum.binding.qaservice.transactions.TxMessageProtos.IncrementCounterTxBody;
import com.exonum.binding.qaservice.transactions.TxMessageProtos.ThrowingTxBody;
import com.google.protobuf.ByteString;
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
    int id = tx.id();

    assertThat(QaTransactionConverter.TRANSACTION_FACTORIES)
        .as("No entry for transaction %s with id=%d", tx, id)
        .containsKey(id);
  }

  @Test
  void toTransactionUnknownTransaction() {
    RawTransaction raw = UnknownTx.newRawTransaction(1);

    Exception e = assertThrows(IllegalArgumentException.class,
        () -> converter.toTransaction(UnknownTx.ID, raw.getPayload()));
    assertThat(e.getMessage()).startsWith("Unknown transaction")
        .contains(Integer.toString(UnknownTx.ID));
  }

  @ParameterizedTest
  @MethodSource("transactions")
  void toTransaction(int txId, byte[] arguments, Transaction expectedTx) {
    Transaction transaction = converter.toTransaction(txId, arguments);

    assertThat(transaction).isEqualTo(expectedTx);
  }

  private static Collection<Arguments> transactions() {
    List<Arguments> transactionTemplates = asList(
        createCounterArgs(),
        incrementCounterArgs(),
        throwingTxArgs(),
        errorTxArgs()
    );

    // Check that the test data includes all known transactions.
    assertThat(transactionTemplates).hasSameSizeAs(QaTransaction.values());

    return transactionTemplates;
  }

  private static Arguments createCounterArgs() {
    String name = "name";
    return arguments(CREATE_COUNTER.id(),
        CreateCounterTxBody.newBuilder()
            .setName(name)
            .build()
            .toByteArray(),
        new CreateCounterTx(name));
  }

  private static Arguments incrementCounterArgs() {
    long seed = 10L;
    HashCode id = defaultHashFunction().hashString("name", UTF_8);
    return arguments(INCREMENT_COUNTER.id(),
        IncrementCounterTxBody.newBuilder()
            .setSeed(10L)
            .setCounterId(ByteString.copyFrom(id.asBytes()))
            .build()
            .toByteArray(),
        new IncrementCounterTx(seed, id));
  }

  private static Arguments throwingTxArgs() {
    long seed = 1L;
    return arguments(VALID_THROWING.id(),
        ThrowingTxBody.newBuilder()
            .setSeed(seed)
            .build()
            .toByteArray(),
        new ThrowingTx(seed));
  }

  private static Arguments errorTxArgs() {
    long seed = 10L;
    byte errorCode = (byte) 1;
    String description = "some error";
    return arguments(VALID_ERROR.id(),
        ErrorTxBody.newBuilder()
            .setSeed(seed)
            .setErrorCode(errorCode)
            .setErrorDescription(description)
            .build()
            .toByteArray(),
        new ErrorTx(seed, errorCode, description));
  }
}
