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

import com.exonum.binding.messages.Message;
import com.exonum.binding.qaservice.PromoteToCore;
import com.exonum.binding.qaservice.QaService;

@PromoteToCore("You have to check these preconditions all the time!")
final class TransactionPreconditions {

  private static final short SERVICE_ID = QaService.ID;

  static <MessageT extends Message> MessageT checkTransaction(MessageT message,
                                                              short expectedTxId) {
    short serviceId = message.getServiceId();
    checkArgument(serviceId == SERVICE_ID,
        "This message (%s) does not belong to this service: wrong service id (%s), must be %s",
        message, serviceId, SERVICE_ID);

    short txId = message.getMessageType();
    checkArgument(txId == expectedTxId,
        "This message (%s) has wrong transaction id (%s), must be %s", message, txId, expectedTxId);

    // todo: check empty signature?
    return message;
  }

  static <MessageT extends Message> MessageT checkMessageSize(MessageT message,
                                                              int expectedBodySize) {
    checkArgument(0 <= expectedBodySize, "You cannot expect negative size, can you?");

    int expectedSize = Message.messageSize(expectedBodySize);
    checkArgument(message.size() == expectedSize,
        "This message (%s) has wrong size (%s), expected %s bytes", message, message.size(),
        expectedSize);

    return message;
  }

  private TransactionPreconditions() {
    throw new AssertionError("Non-instantiable");
  }
}
