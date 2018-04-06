package ${groupId};

import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.service.TransactionConverter;

/**
 * {@code MyTransactionConverter} converts transaction messages of {@link $.MyService}
 * into {@linkplain Transaction executable transactions} of this service.
 */
public final class MyTransactionConverter implements TransactionConverter {

  @Override
  public Transaction toTransaction(BinaryMessage message) {
    return null;
  }

}
