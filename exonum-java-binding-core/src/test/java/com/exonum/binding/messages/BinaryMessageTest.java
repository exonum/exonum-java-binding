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

package com.exonum.binding.messages;

import static com.exonum.binding.messages.ByteBufferAllocator.allocateBuffer;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.Hashing;
import org.junit.Test;

public class BinaryMessageTest {

  @Test
  public void hash() {
    BinaryMessage message = new Message.Builder()
        .setNetworkId((byte) 0x01)
        .setVersion((byte) 0x02)
        .setServiceId((short) 0xA103)
        .setMessageType((short) 0xB204)
        .setBody(allocateBuffer(2))
        .setSignature(allocateBuffer(64))
        .buildRaw();

    HashCode hash = message.hash();

    assertThat(hash.bits(), equalTo(Hashing.DEFAULT_HASH_SIZE_BITS));
  }
}
