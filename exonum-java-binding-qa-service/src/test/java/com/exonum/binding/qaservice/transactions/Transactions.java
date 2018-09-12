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

import com.exonum.binding.common.message.Message;
import com.exonum.binding.qaservice.QaService;
import java.nio.ByteBuffer;

final class Transactions {

  /** A template of QA service transaction. */
  static final Message QA_TX_MESSAGE_TEMPLATE = new Message.Builder()
      .setNetworkId((byte) 0)
      .setVersion((byte) 0)
      .setServiceId(QaService.ID)
      .setMessageType(Short.MAX_VALUE)
      .setBody(ByteBuffer.allocate(0))
      .setSignature(new byte[Message.SIGNATURE_SIZE])
      .buildPartial();

  private Transactions() {
    throw new AssertionError("Non-instantiable");
  }
}
