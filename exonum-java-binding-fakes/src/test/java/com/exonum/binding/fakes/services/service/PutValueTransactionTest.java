package com.exonum.binding.fakes.services.service;

import static com.exonum.binding.fakes.services.service.PutValueTransaction.BODY_CHARSET;
import static com.exonum.binding.fakes.services.service.TestSchemaFactories.createTestSchemaFactory;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.Hashing;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.storage.database.ForkProxy;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import java.nio.ByteBuffer;
import org.junit.Test;

public class PutValueTransactionTest {

  private static final Message TX_MESSAGE_TEMPLATE = new Message.Builder()
      .setNetworkId((byte) 0)
      .setServiceId(TestService.ID)
      .setMessageType(PutValueTransaction.ID)
      .setVersion((byte) 1)
      .setBody(encode("any value"))
      .setSignature(ByteBuffer.allocate(Message.SIGNATURE_SIZE))
      .buildPartial();

  @Test(expected = IllegalArgumentException.class)
  public void from_WrongMessageType() {
    BinaryMessage txMessage = new Message.Builder()
        .mergeFrom(TX_MESSAGE_TEMPLATE)
        .setMessageType((short) (PutValueTransaction.ID - 1))
        .buildRaw();

    PutValueTransaction.from(txMessage, TestSchema::new);
  }

  @Test(expected = IllegalArgumentException.class)
  public void from_WrongService() {
    BinaryMessage txMessage = new Message.Builder()
        .mergeFrom(TX_MESSAGE_TEMPLATE)
        .setServiceId((short) (TestService.ID + 1))
        .buildRaw();

    PutValueTransaction.from(txMessage, TestSchema::new);
  }

  @Test
  public void isValid() {
    BinaryMessage txMessage = new Message.Builder()
        .mergeFrom(TX_MESSAGE_TEMPLATE)
        .buildRaw();

    PutValueTransaction tx = PutValueTransaction.from(txMessage, TestSchema::new);
    assertTrue(tx.isValid());
  }

  @Test
  @SuppressWarnings("unchecked")  // No type parameters for clarity
  public void execute() {
    String value = "A value to put";
    HashCode hash = Hashing.defaultHashFunction()
        .hashString(value, BODY_CHARSET);
    BinaryMessage txMessage = new Message.Builder()
        .mergeFrom(TX_MESSAGE_TEMPLATE)
        .setBody(encode(value))
        .buildRaw();

    ForkProxy fork = mock(ForkProxy.class);
    TestSchema schema = mock(TestSchema.class);
    ProofMapIndexProxy testMap = mock(ProofMapIndexProxy.class);
    when(schema.testMap()).thenReturn(testMap);

    PutValueTransaction tx = PutValueTransaction.from(txMessage,
        createTestSchemaFactory(fork, schema));

    tx.execute(fork);

    verify(testMap).put(eq(hash), eq(value));
  }

  private static ByteBuffer encode(String value) {
    return ByteBuffer.wrap(value.getBytes(BODY_CHARSET));
  }
}
