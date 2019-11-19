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

import com.exonum.binding.core.service.TransactionConverter;
import com.exonum.binding.core.transaction.Transaction;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import java.util.function.Function;

/** A converter of QA service transaction messages. */
public final class QaTransactionConverter implements TransactionConverter {

  @VisibleForTesting
  static final ImmutableMap<Integer, Function<byte[], Transaction>> TRANSACTION_FACTORIES =
      ImmutableMap.<Integer, Function<byte[], Transaction>>builder()
          .put(INCREMENT_COUNTER.id(), IncrementCounterTx::fromBytes)
          .put(CREATE_COUNTER.id(), CreateCounterTx::fromBytes)
          .put(VALID_THROWING.id(), ThrowingTx::fromBytes)
          .put(VALID_ERROR.id(), ErrorTx::fromBytes)
          .build();

  @Override
  public Transaction toTransaction(int txId, byte[] arguments) {
    return TRANSACTION_FACTORIES.getOrDefault(txId, (args) -> {
      throw new IllegalArgumentException("Unknown transaction id: " + txId);
    })
        .apply(arguments);
  }

}
