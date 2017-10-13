package com.exonum.binding.messages.test;

import static com.google.common.base.Preconditions.checkArgument;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

// Usually such classes would be auto-generated.
public class SendMoneyData {

  static final int SIZE = 3 * Long.BYTES;

  private final ByteBuffer buf;

  public SendMoneyData(ByteBuffer buf) {
    checkArgument(buf.remaining() == SIZE);
    this.buf = buf.duplicate()
        .order(ByteOrder.LITTLE_ENDIAN);
  }

  public long getFrom() {
    return buf.getLong(0);
  }

  public long getTo() {
    return buf.getLong(8);
  }

  public long getAmount() {
    return buf.getLong(16);
  }
}
