package com.exonum.binding.cryptocurrency.transactions.converters;

import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.messages.Transaction;

/**
 * A converter between executable transaction & its binary message.
 *
 * @param <TransactionT> a type of transaction. You can have a single {@link
 *     TransactionMessageConverter} that works with any transactions of this service or a converter
 *     for each transaction type.
 */
public interface TransactionMessageConverter<TransactionT extends Transaction> {

  /** Converts a message into an executable transaction. */
  TransactionT fromMessage(Message txMessage);

  /**
   * Returns a message, representing the given transaction.
   *
   * <p>In a more general case, one will have to provide the signature, network getId, etc., see
   * class level comment.
   */
  BinaryMessage toMessage(TransactionT transaction);
}
