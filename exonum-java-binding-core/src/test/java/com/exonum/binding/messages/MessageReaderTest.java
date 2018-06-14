package com.exonum.binding.messages;

import static com.exonum.binding.messages.ByteBufferAllocator.allocateBuffer;
import static com.exonum.binding.messages.Message.BODY_OFFSET;
import static com.exonum.binding.messages.Message.MESSAGE_TYPE_OFFSET;
import static com.exonum.binding.messages.Message.NET_ID_OFFSET;
import static com.exonum.binding.messages.Message.PAYLOAD_LENGTH_OFFSET;
import static com.exonum.binding.messages.Message.SERVICE_ID_OFFSET;
import static com.exonum.binding.messages.Message.SIGNATURE_SIZE;
import static com.exonum.binding.messages.Message.VERSION_OFFSET;
import static com.exonum.binding.messages.MessageReader.MIN_MESSAGE_BUFFER_SIZE;
import static com.exonum.binding.test.Bytes.bytes;
import static com.exonum.binding.test.Bytes.createPrefixed;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MessageReaderTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void wrapThrowsIfTooSmall() throws Exception {
    ByteBuffer buf = allocateBuffer(2);

    expectedException.expectMessage("The buffer size (2) is less than the minimal possible "
        + "message size (74)");
    expectedException.expect(IllegalArgumentException.class);
    MessageReader.wrap(buf);
  }

  @Test
  public void wrapThrowsIfTooSmall2() throws Exception {
    ByteBuffer buf = allocateBuffer(MIN_MESSAGE_BUFFER_SIZE - 1);

    expectedException.expectMessage("The buffer size (73) is less than the minimal possible "
        + "message size (74)");
    expectedException.expect(IllegalArgumentException.class);
    MessageReader.wrap(buf);
  }

  @Test
  public void wrapThrowsIfTooSmallWithSetMessageSize() throws Exception {
    ByteBuffer buf = allocateMessageBuffer(MIN_MESSAGE_BUFFER_SIZE - 1);

    expectedException.expectMessage("The buffer size (73) is less than the minimal possible "
        + "message size (74)");
    expectedException.expect(IllegalArgumentException.class);
    MessageReader.wrap(buf);
  }

  @Test
  public void wrapsMinimalMessage() throws Exception {
    ByteBuffer buf = allocateMessageBuffer(MIN_MESSAGE_BUFFER_SIZE);

    MessageReader m = MessageReader.wrap(buf);

    assertThat(m.getBody().remaining(), equalTo(0));
  }

  @Test
  public void wrapsWhenLimitNotEqualCapacity() throws Exception {
    ByteBuffer buf = allocateBuffer(2 * MIN_MESSAGE_BUFFER_SIZE)
        .putInt(PAYLOAD_LENGTH_OFFSET, MIN_MESSAGE_BUFFER_SIZE);
    buf.limit(MIN_MESSAGE_BUFFER_SIZE);

    MessageReader m = MessageReader.wrap(buf);

    assertThat(m.getBody().remaining(), equalTo(0));
  }

  @Test
  public void wrapThrowsIfMessageSizeFieldGreaterThanActual() throws Exception {
    int bufferSize = MIN_MESSAGE_BUFFER_SIZE;
    ByteBuffer buf = allocateBuffer(bufferSize);
    int messageSize = 2048;
    buf.putInt(PAYLOAD_LENGTH_OFFSET, messageSize);

    expectedException.expectMessage("The size of the buffer (" + bufferSize
        + ") does not match the expected size specified in the message header (" + messageSize
        + ")");
    expectedException.expect(IllegalArgumentException.class);
    MessageReader.wrap(buf);
  }

  @Test
  public void wrapThrowsIfMessageSizeFieldLessThanActual() throws Exception {
    int bufferSize = 2 * MIN_MESSAGE_BUFFER_SIZE;
    ByteBuffer buf = allocateBuffer(bufferSize);
    int messageSize = bufferSize - 1;
    buf.putInt(PAYLOAD_LENGTH_OFFSET, messageSize);

    expectedException.expectMessage("The size of the buffer (" + bufferSize
        + ") does not match the expected size specified in the message header (" + messageSize
        + ")");
    expectedException.expect(IllegalArgumentException.class);
    MessageReader.wrap(buf);
  }

  @Test
  public void getNetworkId() throws Exception {
    byte netId = 0x01;
    ByteBuffer buf = allocateMessageBuffer(MIN_MESSAGE_BUFFER_SIZE)
        .put(NET_ID_OFFSET, netId);

    MessageReader m = MessageReader.wrap(buf);

    assertThat(m.getNetworkId(), equalTo(netId));
  }

  @Test
  public void getVersion() throws Exception {
    byte version = 0x02;
    ByteBuffer buf = allocateMessageBuffer(MIN_MESSAGE_BUFFER_SIZE)
        .put(VERSION_OFFSET, version);

    MessageReader m = MessageReader.wrap(buf);

    assertThat(m.getVersion(), equalTo(version));
  }

  @Test
  public void getServiceId() throws Exception {
    short serviceId = 0x0BCD;
    ByteBuffer buf = allocateMessageBuffer(MIN_MESSAGE_BUFFER_SIZE)
        .putShort(SERVICE_ID_OFFSET, serviceId);

    MessageReader m = MessageReader.wrap(buf);

    assertThat(m.getServiceId(), equalTo(serviceId));
  }

  @Test
  public void getMessageType() throws Exception {
    short messageType = 0x0BCD;
    ByteBuffer buf = allocateMessageBuffer(MIN_MESSAGE_BUFFER_SIZE)
        .putShort(MESSAGE_TYPE_OFFSET, messageType);

    MessageReader m = MessageReader.wrap(buf);

    assertThat(m.getMessageType(), equalTo(messageType));
  }

  @Test
  public void getBody_Empty() throws Exception {
    ByteBuffer buf = allocateMessageBuffer(MIN_MESSAGE_BUFFER_SIZE);
    boolean directBuffer = buf.isDirect();

    MessageReader m = MessageReader.wrap(buf);

    ByteBuffer body = m.getBody();
    assertThat(body.isDirect(), equalTo(directBuffer));
    assertThat(body.remaining(), equalTo(0));
  }

  @Test
  public void getBody_4Bytes() throws Exception {
    int bodySize = Integer.BYTES;
    int bodyValue = 0x12345678;
    ByteBuffer buf = allocateMessageBuffer(MIN_MESSAGE_BUFFER_SIZE + bodySize)
        .putInt(BODY_OFFSET, bodyValue);

    MessageReader m = MessageReader.wrap(buf);

    ByteBuffer body = m.getBody()
        .order(ByteOrder.LITTLE_ENDIAN);
    assertThat(body.remaining(), equalTo(bodySize));
    assertThat(body.getInt(), equalTo(bodyValue));
  }

  @Test
  public void getSignature() throws Exception {
    byte[] signature = createPrefixed(bytes("Signature bytes"), SIGNATURE_SIZE);
    ByteBuffer buf = allocateMessageBuffer(MIN_MESSAGE_BUFFER_SIZE);
    buf.position(MIN_MESSAGE_BUFFER_SIZE - SIGNATURE_SIZE);
    buf.put(signature);
    buf.flip();

    MessageReader m = MessageReader.wrap(buf);

    assertThat(m.getSignature(), equalTo(ByteBuffer.wrap(signature)));
  }

  @Test
  public void getMessage() throws Exception {
    ByteBuffer buf = allocateMessageBuffer(MIN_MESSAGE_BUFFER_SIZE)
        .put(NET_ID_OFFSET, (byte) 0x02)
        .put(VERSION_OFFSET, (byte) 0x01)
        .putShort(MESSAGE_TYPE_OFFSET, (short) 0x0ABC);

    MessageReader m = MessageReader.wrap(buf);

    ByteBuffer binaryMessage = m.getMessage();
    assertThat(binaryMessage, equalTo(buf));
  }

  @Test
  public void size() throws Exception {
    int bufferSize = MIN_MESSAGE_BUFFER_SIZE;
    ByteBuffer buf = allocateMessageBuffer(bufferSize);

    MessageReader m = MessageReader.wrap(buf);

    assertThat(m.size(), equalTo(bufferSize));
  }

  /**
   * Allocates a byte buffer of the given size and sets its "payload_length" field.
   */
  private static ByteBuffer allocateMessageBuffer(int bufferSize) {
    return allocateBuffer(bufferSize)
        .putInt(PAYLOAD_LENGTH_OFFSET, bufferSize);
  }
}
