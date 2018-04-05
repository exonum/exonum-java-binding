package ${groupId};

import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.service.TransactionConverter;

public final class MyTransactionConverter implements TransactionConverter {

  @Override
  public Transaction toTransaction(BinaryMessage message) {
    return null;
  }

}
