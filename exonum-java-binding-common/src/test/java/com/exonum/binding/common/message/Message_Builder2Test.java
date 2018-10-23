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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import com.exonum.binding.common.message.Message.Builder;
import com.exonum.binding.test.Bytes;
import java.nio.ByteBuffer;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

/**
 * A test of our patches to the auto-generated message builder.
 */
class Message_Builder2Test {

  @Test
  void setBodyBytes() {
    byte[] source = Bytes.fromHex("12ab");

    Message message = new Builder()
        .setBody(source)
        .buildPartial();

    ByteBuffer expectedBody = ByteBuffer.wrap(source);
    assertThat(message.getBody(), equalTo(expectedBody));
  }

  @Test
  void valueEquals() {
    EqualsVerifier.forClass(Message_Builder2.Value.class)
        .verify();
  }

  @Test
  void partialEquals() {
    EqualsVerifier.forClass(Message_Builder2.Partial.class)
        .verify();
  }
}
