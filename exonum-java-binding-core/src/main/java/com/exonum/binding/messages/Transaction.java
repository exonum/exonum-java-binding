package com.exonum.binding.messages;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.storage.database.Fork;

/**
 * An Exonum transaction.
 *
 * <p>You shall usually extend {@link AbstractTransaction} rather than implementing
 * this interface.
 *
 * @see <a href="https://exonum.com/doc/architecture/transactions">Exonum Transactions</a>
 * @see <a href="https://exonum.com/doc/architecture/services">Exonum Services</a>
 */
public interface Transaction {

  /**
   * Returns true if this transaction is valid: its data holds the invariants,
   * it has a correct signature, etc.
   *
   * <p>This method is intended to check the <em>internal</em> consistency of a transaction.
   * You shall <strong>not</strong> access any external objects in this method
   * (e.g., files, network resources, databases).
   *
   * <p>If this method returns false, the transaction is considered incorrect,
   * and Exonum discards it. Exonum never records invalid transactions into a blockchain.
   */
  boolean isValid();

  /**
   * Execute the transaction, possibly modifying the blockchain state.
   *
   * @param view a database view, which allows to modify the blockchain state
   */
  void execute(Fork view);


  /**
   * Returns some information about this transaction in JSON format.
   */
  default String info() {
    return "";
  }

  /**
   * Returns a hash of this transaction — a SHA-256 hash of the transaction message.
   *
   * @implSpec Default implementation returns {@code getMessage().hash()}.
   */
  default HashCode hash() {
    return getMessage().hash();
  }

  /**
   * Returns this transaction as a binary Exonum message.
   */
  BinaryMessage getMessage();
}
