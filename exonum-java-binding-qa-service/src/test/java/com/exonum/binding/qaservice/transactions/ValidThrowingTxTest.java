package com.exonum.binding.qaservice.transactions;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.qaservice.QaService;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ValidThrowingTxTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  static final Message VALID_THROWING_TEMPLATE = new Message.Builder()
      .mergeFrom(Transactions.QA_TX_MESSAGE_TEMPLATE)
      .setMessageType(QaTransaction.VALID_THROWING.id)
      .setBody(body(0))
      .buildPartial();

  @Test
  public void isValid() {
    BinaryMessage message = new Message.Builder()
        .mergeFrom(VALID_THROWING_TEMPLATE)
        .buildRaw();

    ValidThrowingTx tx = new ValidThrowingTx(message);

    assertTrue(tx.isValid());
  }

  @Test
  public void info() {
    long seed = 10L;
    BinaryMessage message = new Message.Builder()
        .mergeFrom(VALID_THROWING_TEMPLATE)
        .setBody(body(seed))
        .buildRaw();

    ValidThrowingTx tx = new ValidThrowingTx(message);
    String info = tx.info();

    Gson gson = new Gson();
    AnyTransaction txParams = gson.fromJson(info, AnyTransaction.class);

    assertThat(txParams.service_id, equalTo(QaService.ID));
    assertThat(txParams.message_id, equalTo(QaTransaction.VALID_THROWING.id));
    assertThat(txParams.body, equalTo(ImmutableMap.of("seed", Long.toHexString(seed))));
  }

  private static ByteBuffer body(long seed) {
    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putLong(seed);
    buffer.rewind();
    return buffer;
  }
}
