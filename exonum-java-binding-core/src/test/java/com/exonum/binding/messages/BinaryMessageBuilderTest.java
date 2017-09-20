package com.exonum.binding.messages;

import static com.exonum.binding.messages.ByteBufferAllocator.allocateBuffer;
import static com.exonum.binding.messages.Message.SIGNATURE_SIZE;
import static com.exonum.binding.test.Bytes.bytes;
import static com.exonum.binding.test.Bytes.createPrefixed;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.nio.ByteBuffer;
import org.junit.Test;

public class BinaryMessageBuilderTest {

  @Test
  public void buildFromMessage() throws Exception {
    int bodySize = Long.BYTES;
    Message message = createMessage(bodySize);

    BinaryMessage result = new BinaryMessageBuilder(message).build();

    assertThat(result.getNetworkId(), equalTo(message.getNetworkId()));
    assertThat(result.getVersion(), equalTo(message.getVersion()));
    assertThat(result.getServiceId(), equalTo(message.getServiceId()));
    assertThat(result.getMessageType(), equalTo(message.getMessageType()));
    assertThat(result.getBody(), equalTo(message.getBody()));
    assertThat(result.getSignature(), equalTo(message.getSignature()));
    assertThat(result.size(), equalTo(Message.messageSize(bodySize)));
  }

  @Test
  public void buildFromBinaryMessage() throws Exception {
    int bodySize = Long.BYTES;
    Message srcMessage = createMessage(bodySize);

    Message binaryMessage = new BinaryMessageBuilder(srcMessage).build();

    BinaryMessage result = BinaryMessageBuilder.toBinary(binaryMessage);

    assertThat(result, sameInstance(binaryMessage));
  }

  private Message createMessage(int bodySize) {
    ByteBuffer body = allocateBuffer(bodySize);
    for (int i = 0; i < bodySize; i++) {
      body.put((byte) i);
    }
    body.flip();

    ByteBuffer signature = allocateBuffer(SIGNATURE_SIZE)
        .put(createPrefixed(bytes("Signature"), SIGNATURE_SIZE));
    signature.flip();

    return new Message.Builder()
        .setNetworkId((byte) 0xA1)
        .setVersion((byte) 0xB2)
        .setMessageType((short) 0xC003)
        .setServiceId((short) 0xD004)
        .setBody(body)
        .setSignature(signature)
        .buildPartial();
  }
}
