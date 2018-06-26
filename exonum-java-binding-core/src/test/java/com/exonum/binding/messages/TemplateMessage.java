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

package com.exonum.binding.messages;

import static com.exonum.binding.messages.ByteBufferAllocator.allocateBuffer;
import static com.exonum.binding.messages.Message.SIGNATURE_SIZE;

public class TemplateMessage {

  /**
   * An immutable template message, that has all fields of a message set.
   * Use it in tests when you need a binary message with no or a few fields set
   * to particular values.
   */
  public static final Message TEMPLATE_MESSAGE = new Message.Builder()
      .setNetworkId((byte) 1)
      .setVersion((byte) 0)
      .setServiceId((short) 0)
      .setMessageType((short) 1)
      .setBody(allocateBuffer(2))
      .setSignature(allocateBuffer(SIGNATURE_SIZE))
      .buildPartial();

  private TemplateMessage() {}
}
