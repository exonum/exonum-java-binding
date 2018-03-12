package com.exonum.binding.qaservice.transactions;

import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.messages.Message;
import com.exonum.binding.qaservice.QaService;

// TODO: consider moving that to the library, because you have to check these ids all the time!
final class TransactionPreconditions {

  private static final short SERVICE_ID = QaService.ID;

  static <MessageT extends Message> MessageT checkTransaction(MessageT message,
                                                              short expectedTxId) {
    short serviceId = message.getServiceId();
    checkArgument(serviceId == SERVICE_ID,
        "This message (%s) does not belong to this service: wrong service id (%s), must be %s",
        message, serviceId, SERVICE_ID);

    short txId = message.getMessageType();
    checkArgument(txId == expectedTxId,
        "This message (%s) has wrong transaction id (%s), must be %s", message, txId, expectedTxId);

    // todo: check empty signature?
    return message;
  }

  static <MessageT extends Message> MessageT checkMessageSize(MessageT message,
                                                              int expectedBodySize) {
    checkArgument(0 <= expectedBodySize, "You cannot expect negative size, can you?");

    int expectedSize = Message.messageSize(expectedBodySize);
    checkArgument(message.size() == expectedSize,
        "This message (%s) has wrong size (%s), expected %s bytes", message, message.size(),
        expectedSize);

    return message;
  }

  private TransactionPreconditions() {
    throw new AssertionError("Non-instantiable");
  }
}
