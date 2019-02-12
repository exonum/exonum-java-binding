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

import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.qaservice.PromoteToCore;
import com.exonum.binding.qaservice.QaService;
import com.exonum.binding.transaction.RawTransaction;

@PromoteToCore("You have to check these preconditions all the time!")
final class TransactionPreconditions {

  private static final short SERVICE_ID = QaService.ID;

  static void checkTransaction(RawTransaction transaction, short expectedTxId) {
    checkServiceId(transaction);

    short txId = transaction.getTransactionId();
    checkArgument(txId == expectedTxId,
        "This transaction (%s) has wrong transaction id (%s), must be %s",
        transaction, txId, expectedTxId);
  }

  static void checkPayloadSize(RawTransaction transaction, int expectedSize) {
    checkArgument(transaction.getPayload().length == expectedSize,
        "The payload of this transaction (%s) has wrong size (%s), expected %s bytes",
        transaction, transaction.getPayload().length, expectedSize);
  }

  static void checkServiceId(RawTransaction transaction) {
    short serviceId = transaction.getServiceId();
    checkArgument(serviceId == SERVICE_ID,
        "This transaction (%s) does not belong to this service: wrong service id (%s), must be %s",
        transaction, serviceId, SERVICE_ID);
  }

  private TransactionPreconditions() {
    throw new AssertionError("Non-instantiable");
  }
}
