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

import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.cryptocurrency.CryptocurrencyService;
import com.exonum.binding.transaction.RawTransaction;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

final class TransactionPreconditions {

  private static final short SERVICE_ID = CryptocurrencyService.ID;

  private TransactionPreconditions() {
    throw new AssertionError("Non-instantiable");
  }

  @CanIgnoreReturnValue
  static <T extends RawTransaction> T checkTransaction(T transaction, short expectedTxId) {
    checkServiceId(transaction);
    checkTransactionId(transaction, expectedTxId);
    return transaction;
  }

  private static <T extends RawTransaction> void checkServiceId(T transaction) {
    short serviceId = transaction.getServiceId();
    checkArgument(
        serviceId == SERVICE_ID,
        "Transaction (%s) does not belong to this service: wrong service ID (%s), must be %s",
        transaction,
        serviceId,
        SERVICE_ID);
  }

  private static <T extends RawTransaction> void checkTransactionId(
      T transaction, short expectedTxId) {
    short txId = transaction.getTransactionId();
    checkArgument(
        txId == expectedTxId,
        "Transaction (%s) has wrong transaction ID (%s), must be %s",
        transaction,
        txId,
        expectedTxId);
  }
  
}
