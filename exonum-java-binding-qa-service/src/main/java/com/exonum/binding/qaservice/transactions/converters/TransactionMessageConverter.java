package com.exonum.binding.qaservice.transactions.converters;

import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.qaservice.PromoteToCore;

/**
 * A converter between executable transaction & its binary message.
 *
 * @param <TransactionT> a type of transaction. You can have a single
 *     {@link TransactionMessageConverter} that works with any transactions of this service
 *     or a converter for each transaction type.
 */
@PromoteToCore("… with first-class support of transactions that don’t need a message to operate."
    + "Consider splitting into two functional interfaces, so that some APIs may use "
    + "method references."
    + "An open question is how you convert transaction into message if not all data "
    + "is known (and must be known by transaction): a network id, a protocol version, "
    + "the signature. These things do not comprise the transaction parameters."
)
public interface TransactionMessageConverter<TransactionT extends Transaction> {

  /**
   * Converts a message into an executable transaction.
   */
  TransactionT fromMessage(Message txMessage);

  /**
   * Returns a message, representing the given transaction.
   *
   * <p>In a more general case, one will have to provide the signature, network id, etc.,
   * see class level comment.
   */
  BinaryMessage toMessage(TransactionT transaction);
}
