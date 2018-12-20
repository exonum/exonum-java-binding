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

import static com.exonum.binding.qaservice.transactions.QaTransaction.CREATE_COUNTER;
import static com.exonum.binding.qaservice.transactions.QaTransaction.INCREMENT_COUNTER;
import static com.exonum.binding.qaservice.transactions.QaTransaction.VALID_ERROR;
import static com.exonum.binding.qaservice.transactions.QaTransaction.VALID_THROWING;
import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.qaservice.QaService;
import com.exonum.binding.service.TransactionConverter;
import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.transaction.Transaction;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import java.util.function.Function;

/** A converter of QA service transaction messages. */
public final class QaTransactionConverter implements TransactionConverter {

  @VisibleForTesting
  static final ImmutableMap<Short, Function<RawTransaction, Transaction>> TRANSACTION_FACTORIES =
      ImmutableMap.<Short, Function<RawTransaction, Transaction>>builder()
          .put(INCREMENT_COUNTER.id(), IncrementCounterTx.converter()::fromRawTransaction)
          .put(CREATE_COUNTER.id(), CreateCounterTx.converter()::fromRawTransaction)
          .put(VALID_THROWING.id(), ValidThrowingTx.converter()::fromRawTransaction)
          .put(VALID_ERROR.id(), ValidErrorTx.converter()::fromRawTransaction)
          .build();

  @Override
  public Transaction toTransaction(RawTransaction rawTransaction) {
    checkServiceId(rawTransaction);

    short txId = rawTransaction.getTransactionId();
    return TRANSACTION_FACTORIES.getOrDefault(txId, (m) -> {
      throw new IllegalArgumentException("Unknown transaction id: " + txId);
    })
        .apply(rawTransaction);
  }

  private static void checkServiceId(RawTransaction rawTransaction) {
    short serviceId = rawTransaction.getServiceId();
    checkArgument(serviceId == QaService.ID,
        "Wrong service id (%s), must be %s", serviceId, QaService.ID);
  }
}
