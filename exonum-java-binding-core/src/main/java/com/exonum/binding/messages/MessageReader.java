package com.exonum.binding.messages;

import static com.google.common.base.Preconditions.checkArgument;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A reader of binary Exonum messages.
 *
 * <p>See <a href=https://exonum.com/doc/architecture/serialization/#message-serialization>the definition of the message format</a>.
 */
public final class MessageReader implements BinaryMessage {

  /** Assuming zero-length body. */
  static final int MIN_MESSAGE_BUFFER_SIZE = HEADER_SIZE + SIGNATURE_SIZE;

  private final ByteBuffer message;

  /**
   * Creates a MessageReader from the given byte buffer.
   *
   * @param buffer a byte buffer to read. Its position must be zero,
   *               and the limit must be set to the size of the message
   * @return a message reader of this byte buffer
   * @throws IllegalArgumentException if the message has invalid size
   */
  public static MessageReader wrap(ByteBuffer buffer) {
    MessageReader reader = new MessageReader(buffer);

    int bufferSize = buffer.limit();
    checkArgument(MIN_MESSAGE_BUFFER_SIZE <= bufferSize, 
        "The buffer size (%s) is less than the minimal possible (%s)",
        bufferSize, MIN_MESSAGE_BUFFER_SIZE);
    int expectedSize = Message.messageSize(reader.bodySize());
    checkArgument(bufferSize == expectedSize,
        "The size of the buffer (%s) does not match expected (%s)", bufferSize, expectedSize);
    return reader;
  }

  private MessageReader(ByteBuffer buf) {
    this.message = buf.duplicate()
        .order(ByteOrder.LITTLE_ENDIAN);
  }

  @Override
  public byte getNetworkId() {
    return message.get(NET_ID_OFFSET);
  }

  @Override
  public byte getVersion() {
    return message.get(VERSION_OFFSET);
  }

  @Override
  public short getServiceId() {
    return message.getShort(SERVICE_ID_OFFSET);
  }

  @Override
  public short getMessageType() {
    return message.getShort(MESSAGE_TYPE_OFFSET);
  }

  /**
   * Returns the body of the message as a view in the underlying byte buffer.
   *
   * <p>The returned byte buffer shares the content of the underlying byte buffer of this message,
   * and is read-only. Its position is zero, limit is set to the position after-the-last
   * element of the body.
   */
  // !!! todo: shall it be read-only? Perhaps yes, for you don't want anyone to modify
  // the underlying buffer. But that would make the underlying array inaccessible
  // if you want to pass it through JNI!
  // On top of that, we intentionally do not copy the original byte buffer,
  // so a malicious code may keep a reference to that byte buffer and modify it.
  @Override
  public ByteBuffer getBody() {
    message.position(BODY_OFFSET);
    ByteBuffer body = message.slice();
    body.limit(bodySize());
    return body;
  }

  private int bodySize() {
    return message.getInt(BODY_LENGTH_OFFSET);
  }

  /**
   * {@inheritDoc}
   *
   * <p>The returned byte buffer shares the content of the underlying byte buffer of this message,
   * and is read-only.
   */
  @Override
  public ByteBuffer getSignature() {
    message.position(signatureOffset());
    return message.slice();
  }

  @Override
  public int size() {
    return message.limit();
  }

  /**
   * {@inheritDoc}
   *
   * <p>The returned byte buffer shares the content of the underlying byte buffer of this message,
   * and is read-only.
   */
  @Override
  public ByteBuffer getMessage() {
    message.position(0);
    return message.duplicate();
  }
}
