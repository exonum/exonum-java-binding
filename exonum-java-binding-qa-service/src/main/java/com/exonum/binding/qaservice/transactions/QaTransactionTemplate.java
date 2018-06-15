/* 
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.qaservice.transactions;

import com.exonum.binding.messages.Message;
import com.exonum.binding.qaservice.QaService;
import java.nio.ByteBuffer;

class QaTransactionTemplate {
  /**
   * A template of any transaction of QA service. It has an id of QA service, empty body
   * and all-zero signature.
   */
  private static final Message QA_TRANSACTION_TEMPLATE = new Message.Builder()
      .setServiceId(QaService.ID)
      .setMessageType(UnknownTx.ID)
      .setBody(allocateReadOnly(0))
      .setSignature(allocateReadOnly(Message.SIGNATURE_SIZE))
      .build();

  /**
   * Creates a new builder of QA service transaction.
   *
   * @param transactionId a message type of QA service transaction
   * @return a message builder that has an id of QA service, the given transaction id, empty body
   *     and all-zero signature
   */
  static Message.Builder newQaTransactionBuilder(short transactionId) {
    return new Message.Builder()
        .mergeFrom(QA_TRANSACTION_TEMPLATE)
        .setMessageType(transactionId);
  }

  private static ByteBuffer allocateReadOnly(int size) {
    return ByteBuffer.allocate(size).asReadOnlyBuffer();
  }

  private QaTransactionTemplate() {}
}
