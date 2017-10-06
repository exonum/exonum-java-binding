package com.exonum.binding.messages;

import static com.exonum.binding.messages.ByteBufferAllocator.allocateBuffer;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.nio.ByteBuffer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MessageTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void messageSize_emptyBody() throws Exception {
    assertThat(Message.messageSize(0),
        equalTo(Message.HEADER_SIZE + Message.SIGNATURE_SIZE));
  }

  @Test
  public void messageSize_nonEmptyBody() throws Exception {
    int bodySize = 8;
    int expected = bodySize + Message.HEADER_SIZE + Message.SIGNATURE_SIZE;
    assertThat(Message.messageSize(bodySize), equalTo(expected));
  }

  @Test
  @SuppressWarnings({"ResultOfMethodCallIgnored"})
  public void messageSize_negSize() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    Message.messageSize(-1);
  }

  @Test
  @SuppressWarnings({"ResultOfMethodCallIgnored"})
  public void messageSize_tooBigSize() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    Message.messageSize(Integer.MAX_VALUE);
  }

  @Test
  public void signatureOffset_emptyBody() throws Exception {
    int bodySize = 0;
    Message m = new Message.Builder()
        .setBody(allocateBuffer(bodySize))
        .buildPartial();

    assertThat(m.signatureOffset(), equalTo(Message.BODY_OFFSET));
  }

  @Test
  public void signatureOffset_nonEmptyBody() throws Exception {
    int bodySize = 4;
    Message m = new Message.Builder()
        .setBody(allocateBuffer(bodySize))
        .buildPartial();

    assertThat(m.signatureOffset(), equalTo(Message.BODY_OFFSET + bodySize));
  }

  @Test
  public void size_nonEmptyBody() throws Exception {
    int bodySize = 4;
    Message m = new Message.Builder()
        .setBody(allocateBuffer(bodySize))
        .buildPartial();

    assertThat(m.size(), equalTo(Message.HEADER_SIZE + Message.SIGNATURE_SIZE + bodySize));
  }

  @Test
  public void builder_rejectsSmallSignature() throws Exception {
    Message.Builder builder = new Message.Builder();

    int signatureSize = 63;
    ByteBuffer signature = allocateBuffer(signatureSize);

    expectedException.expect(IllegalArgumentException.class);
    builder.setSignature(signature);
  }

  @Test
  public void builder_rejectsBigSignature() throws Exception {
    Message.Builder builder = new Message.Builder();

    int signatureSize = 65;
    ByteBuffer signature = allocateBuffer(signatureSize);

    expectedException.expect(IllegalArgumentException.class);
    builder.setSignature(signature);
  }

}
