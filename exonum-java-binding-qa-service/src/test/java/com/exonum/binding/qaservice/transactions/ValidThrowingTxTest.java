package com.exonum.binding.qaservice.transactions;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.qaservice.QaService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import nl.jqno.equalsverifier.EqualsVerifier;
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
  public void converterFromMessage() {
    long seed = 10L;
    BinaryMessage message = new Message.Builder()
        .mergeFrom(VALID_THROWING_TEMPLATE)
        .setBody(body(seed))
        .buildRaw();

    ValidThrowingTx tx = ValidThrowingTx.converter().fromMessage(message);

    ValidThrowingTx expectedTx = new ValidThrowingTx(seed);
    assertThat(tx, equalTo(expectedTx));
  }

  @Test
  public void converterRoundtrip() {
    long seed = 10L;
    ValidThrowingTx tx = new ValidThrowingTx(seed);

    BinaryMessage message = ValidThrowingTx.converter().toMessage(tx);

    ValidThrowingTx txFromMessage = ValidThrowingTx.converter().fromMessage(message);

    assertThat(txFromMessage, equalTo(tx));
  }

  @Test
  public void isValid() {
    long seed = 10L;
    ValidThrowingTx tx = new ValidThrowingTx(seed);

    assertTrue(tx.isValid());
  }

  @Test
  public void info() {
    long seed = 10L;
    ValidThrowingTx tx = new ValidThrowingTx(seed);
    String info = tx.info();

    Gson gson = QaTransactionGson.instance();
    AnyTransaction<ValidThrowingTx> txParams = gson.fromJson(info,
        new TypeToken<AnyTransaction<ValidThrowingTx>>(){}.getType());

    assertThat(txParams.service_id, equalTo(QaService.ID));
    assertThat(txParams.message_id, equalTo(QaTransaction.VALID_THROWING.id));
    assertThat(txParams.body, equalTo(tx));
  }

  @Test
  public void equals() {
    EqualsVerifier.forClass(ValidThrowingTx.class)
        .verify();
  }

  private static ByteBuffer body(long seed) {
    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putLong(seed);
    buffer.rewind();
    return buffer;
  }
}
