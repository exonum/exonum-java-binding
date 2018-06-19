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

import com.exonum.binding.messages.AbstractTransaction;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.qaservice.QaService;
import com.exonum.binding.storage.database.Fork;
import java.nio.ByteBuffer;

/**
 * A transaction that has QA service identifier, but an unknown transaction id.
 * Such transaction must be rejected when received by other nodes.
 */
public final class UnknownTx extends AbstractTransaction {

  static final short ID = 9999;

  // todo: do we need seed here? Won't we pollute the local tx pool if allow the seed?
  public UnknownTx() {
    super(createMessage(0L));
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public void execute(Fork view) {
    throw new AssertionError("Must never be executed by the framework: " + this);
  }

  private static BinaryMessage createMessage(long seed) {
    return new Message.Builder()
        .setServiceId(QaService.ID)
        .setMessageType(ID)
        .setNetworkId((byte) 0)
        .setVersion((byte) 0)
        .setBody(ByteBuffer.allocate(Long.BYTES))
        .setSignature(ByteBuffer.allocate(Message.SIGNATURE_SIZE))
        .buildRaw();
  }
}
