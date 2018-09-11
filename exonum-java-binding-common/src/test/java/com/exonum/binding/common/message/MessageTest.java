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

import static com.exonum.binding.common.message.ByteBufferAllocator.allocateBuffer;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MessageTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void messageSize_emptyBody() {
    assertThat(Message.messageSize(0),
        equalTo(Message.HEADER_SIZE + Message.SIGNATURE_SIZE));
  }

  @Test
  public void messageSize_nonEmptyBody() {
    int bodySize = 8;
    int expected = bodySize + Message.HEADER_SIZE + Message.SIGNATURE_SIZE;
    assertThat(Message.messageSize(bodySize), equalTo(expected));
  }

  @Test
  @SuppressWarnings({"ResultOfMethodCallIgnored"})
  public void messageSize_negSize() {
    expectedException.expect(IllegalArgumentException.class);
    Message.messageSize(-1);
  }

  @Test
  @SuppressWarnings({"ResultOfMethodCallIgnored"})
  public void messageSize_tooBigSize() {
    expectedException.expect(IllegalArgumentException.class);
    Message.messageSize(Integer.MAX_VALUE);
  }

  @Test
  public void signatureOffset_emptyBody() {
    int bodySize = 0;
    Message m = new Message.Builder()
        .setBody(allocateBuffer(bodySize))
        .buildPartial();

    assertThat(m.signatureOffset(), equalTo(Message.BODY_OFFSET));
  }

  @Test
  public void signatureOffset_nonEmptyBody() {
    int bodySize = 4;
    Message m = new Message.Builder()
        .setBody(allocateBuffer(bodySize))
        .buildPartial();

    assertThat(m.signatureOffset(), equalTo(Message.BODY_OFFSET + bodySize));
  }

  @Test
  public void size_nonEmptyBody() {
    int bodySize = 4;
    Message m = new Message.Builder()
        .setBody(allocateBuffer(bodySize))
        .buildPartial();

    assertThat(m.size(), equalTo(Message.HEADER_SIZE + Message.SIGNATURE_SIZE + bodySize));
  }

  @Test
  public void builder_rejectsSmallSignature() {
    Message.Builder builder = new Message.Builder();

    int signatureSize = 63;
    byte[] signature = new byte[signatureSize];

    expectedException.expect(IllegalArgumentException.class);
    builder.setSignature(signature);
  }

  @Test
  public void builder_rejectsBigSignature() {
    Message.Builder builder = new Message.Builder();

    int signatureSize = 65;
    byte[] signature = new byte[signatureSize];

    expectedException.expect(IllegalArgumentException.class);
    builder.setSignature(signature);
  }

}
