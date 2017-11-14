package com.exonum.binding.service;

import com.exonum.binding.annotations.AutoGenerationCandidate;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Transaction;

/**
 * A converter of a binary Exonum message, which contains transaction data,
 * into an executable transaction.
 */
@FunctionalInterface
@AutoGenerationCandidate(reason = "Perfectly viable given a service id "
    + "and description of all transactions (ids & implementing classes")
public interface TransactionConverter {

  /**
   * Converts an Exonum transaction message to an executable transaction of some service.
   *
   * @param message a transaction message (i.e., whose message type is a transaction)
   * @return an executable transaction of some service
   * @throws IllegalArgumentException if the message is not a transaction,
   *                                  or a transaction of an unknown service
   * @throws NullPointerException if message is null
   */
  Transaction toTransaction(BinaryMessage message);
}
