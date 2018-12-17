package com.exonum.binding.transaction;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.storage.database.Fork;

/**
 * Transaction context class. Contains required information for the transaction execution.
 */
public interface TransactionContext {
  /**
   * Returns database view allowing R/W operations.
   */
  Fork getFork();

  /**
   * Returns SHA-256 hash of the transaction.
   */
  HashCode getTransactionMessageHash();

  /**
   * Returns public key of the transaction author.
   */
  PublicKey getAuthorPk();
}
