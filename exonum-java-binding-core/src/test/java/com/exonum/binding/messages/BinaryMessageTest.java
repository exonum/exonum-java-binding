package com.exonum.binding.messages;

import static com.exonum.binding.messages.ByteBufferAllocator.allocateBuffer;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.Hashing;
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

    HashCode hash = message.hash();

    assertThat(hash.bits(), equalTo(Hashing.DEFAULT_HASH_SIZE_BITS));
  }
}
