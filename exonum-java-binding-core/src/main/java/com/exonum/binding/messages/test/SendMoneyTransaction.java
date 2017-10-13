package com.exonum.binding.messages.test;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.messages.AbstractTransaction;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.BinaryMessageBuilder;
import com.exonum.binding.messages.Message;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.storage.database.Fork;
import java.nio.ByteBuffer;

/**
 * An example of how a user transaction would look like.
 */
public class SendMoneyTransaction extends AbstractTransaction {

  private static final short ID = 0x01;
  
  private final SendMoneyData data;

  public SendMoneyTransaction(BinaryMessage message) {
    super(message);
    data = new SendMoneyData(message.getBody());
  }

  @Override
  public boolean isValid() {
    return data.getFrom() != data.getTo() &&
        data.getAmount() > 0L;
  }

  @Override
  public void execute(Fork view) {
  }

  public static void exampleHandler() {
    BinaryMessage tx1 = BinaryMessageBuilder.toBinary(new Message.Builder()
        .setBody(ByteBuffer.allocate(SendMoneyData.SIZE)
            .putLong(0x01)  // from
            .putLong(0x02)  // to
            .putLong(1_000_00))  // amount
        .buildPartial());

    Transaction transaction = null;
    if (tx1.getMessageType() == ID) {
      transaction = new SendMoneyTransaction(tx1);
    }
    
    if (transaction != null && transaction.isValid()) {
      submit(transaction);

      HashCode hash = transaction.hash();
      System.out.println(hash);
    }
  }

  private static void submit(Transaction transaction) {
    // submit a transaction to the Exonum network
  }
}
