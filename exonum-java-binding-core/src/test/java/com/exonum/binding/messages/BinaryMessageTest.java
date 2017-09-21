package com.exonum.binding.messages;

import static com.exonum.binding.messages.ByteBufferAllocator.allocateBuffer;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import com.exonum.binding.hash.Hashes;
import org.junit.Test;

public class BinaryMessageTest {

  @Test
  public void hash() throws Exception {
    BinaryMessage message = new Message.Builder()
        .setNetworkId((byte) 0x01)
        .setVersion((byte) 0x02)
        .setServiceId((short) 0xA103)
        .setMessageType((short) 0xB204)
        .setBody(allocateBuffer(2))
        .setSignature(allocateBuffer(64))
        .buildRaw();

    byte[] hash = message.hash();

    assertThat(hash.length, equalTo(Hashes.HASH_SIZE_BYTES));
  }
}
