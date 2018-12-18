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

package com.exonum.binding.common.message;

import com.exonum.binding.transaction.RawTransaction;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TemplateRawTransaction {

  /**
   * Creates default raw transaction.
   */
  public static RawTransaction createRawTransaction(short transactionId) {
    return RawTransaction.newBuilder()
        .serviceId((short) 0)
        .transactionId(transactionId)
        .payload(
            ByteBuffer.allocate(2)
                .order(ByteOrder.LITTLE_ENDIAN)
                .array())
        .build();
  }

  private TemplateRawTransaction() {
    throw new AssertionError("Non-instantiable");
  }
}
