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

  static <T extends RawTransaction> T checkTransaction(T transaction, short expectedTxId) {
    short serviceId = transaction.getServiceId();
    checkArgument(serviceId == SERVICE_ID,
        "This message (%s) does not belong to this service: wrong service id (%s), must be %s",
        transaction, serviceId, SERVICE_ID);

    short txId = transaction.getTransactionId();
    checkArgument(txId == expectedTxId,
        "This message (%s) has wrong transaction id (%s), must be %s",
        transaction, txId, expectedTxId);

    return transaction;
  }

  static <T extends RawTransaction> T checkPayloadSize(T transaction, int expectedSize) {
    checkArgument(transaction.getPayload().length == expectedSize,
        "This transaction (%s) has wrong size (%s), expected %s bytes",
        transaction, transaction.getPayload().length, expectedSize);

    return transaction;
  }


  private TransactionPreconditions() {
    throw new AssertionError("Non-instantiable");
  }
}
