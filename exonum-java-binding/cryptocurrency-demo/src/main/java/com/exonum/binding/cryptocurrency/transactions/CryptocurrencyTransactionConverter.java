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

package com.exonum.binding.cryptocurrency.transactions;

import com.exonum.binding.core.service.TransactionConverter;
import com.exonum.binding.core.transaction.Transaction;
import com.google.common.collect.ImmutableMap;
import java.util.function.Function;

/** A converter of cryptocurrency service transaction messages. */
public final class CryptocurrencyTransactionConverter implements TransactionConverter {

  private static final ImmutableMap<Integer, Function<byte[], Transaction>>
      TRANSACTION_FACTORIES =
      ImmutableMap.of(
          CreateWalletTx.ID, CreateWalletTx::from,
          TransferTx.ID, TransferTx::from);

  @Override
  public Transaction toTransaction(int txId, byte[] arguments) {
    return TRANSACTION_FACTORIES
        .getOrDefault(
            txId,
            (a) -> {
              throw new IllegalArgumentException("Unknown transaction id (" + txId + "), "
                  + "must be one of " + TRANSACTION_FACTORIES.keySet());
            })
        .apply(arguments);
  }
}
