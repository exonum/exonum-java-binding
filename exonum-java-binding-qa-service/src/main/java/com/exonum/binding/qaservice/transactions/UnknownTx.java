package com.exonum.binding.qaservice.transactions;

import com.exonum.binding.messages.AbstractTransaction;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.qaservice.QaService;
import com.exonum.binding.storage.database.ForkProxy;
import java.nio.ByteBuffer;

/**
 * A transaction that has QA service identifier, but an unknown transaction id.
 * Such transaction must be rejected when received by other nodes.
 */
public final class UnknownTx extends AbstractTransaction {

  static final short ID = 9999;

  // todo: do we need seed here? Won't we pollute the local tx pool if allow the seed?
  public UnknownTx() {
    super(createMessage(0L));
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public void execute(ForkProxy view) {
    throw new AssertionError("Must never be executed by the framework: " + this);
  }

  private static BinaryMessage createMessage(long seed) {
    return new Message.Builder()
        .setServiceId(QaService.ID)
        .setMessageType(ID)
        .setNetworkId((byte) 0)
        .setVersion((byte) 0)
        .setBody(ByteBuffer.allocate(Long.BYTES))
        .setSignature(ByteBuffer.allocate(Message.SIGNATURE_SIZE))
        .buildRaw();
  }
}
