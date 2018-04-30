package com.exonum.binding.qaservice.transactions;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.MatchesPattern.matchesPattern;

import com.exonum.binding.messages.Message;
import com.exonum.binding.qaservice.QaService;
import java.nio.ByteBuffer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TransactionPreconditionsTest {

  @Rule
  public final ExpectedException expectedException = ExpectedException.none();

  @Test
  public void checkTransactionValid() {
    short messageType = 0x01;
    Message message = new Message.Builder()
        .setServiceId(QaService.ID)
        .setMessageType(messageType)
        .buildPartial();

    Message actual = TransactionPreconditions.checkTransaction(message, messageType);

    assertThat(actual, sameInstance(message));
  }

  @Test
  public void checkTransactionOfAnotherService() {
    short messageType = 0x01;
    short serviceId = 10;
    Message message = new Message.Builder()
        .setServiceId(serviceId)
        .setMessageType(messageType)
        .buildPartial();

    expectedException.expectMessage(
        matchesPattern("This message \\(.+\\) does not belong to this service: "
            + "wrong service id \\(10\\), must be " + QaService.ID));
    expectedException.expect(IllegalArgumentException.class);
    TransactionPreconditions.checkTransaction(message, messageType);
  }

  @Test
  public void checkTransactionOfAnotherType() {
    short expectedMessageType = 20;
    short messageType = 1;
    short serviceId = QaService.ID;
    Message message = new Message.Builder()
        .setServiceId(serviceId)
        .setMessageType(messageType)
        .buildPartial();

    expectedException.expectMessage(
        matchesPattern("This message \\(.+\\) has wrong transaction id \\(1\\), must be "
            + expectedMessageType));
    expectedException.expect(IllegalArgumentException.class);
    TransactionPreconditions.checkTransaction(message, expectedMessageType);
  }

  @Test
  public void checkMessageCorrectSize() {
    int body = 10;
    Message message = new Message.Builder()
        .setBody(ByteBuffer.allocate(body))
        .buildPartial();

    Message actual = TransactionPreconditions.checkMessageSize(message, body);

    assertThat(actual, sameInstance(message));
  }

  @Test
  public void checkMessageWrongSize() {
    int expectedBody = 11;
    int body = 10;
    Message message = new Message.Builder()
        .setBody(ByteBuffer.allocate(body))
        .buildPartial();

    expectedException.expectMessage(
        matchesPattern("This message \\(.+\\) has wrong size \\(\\d+\\), expected \\d+ bytes"));
    expectedException.expect(IllegalArgumentException.class);
    TransactionPreconditions.checkMessageSize(message, expectedBody);
  }
}
