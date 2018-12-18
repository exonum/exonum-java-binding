package com.exonum.binding.transaction;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.storage.database.Fork;

/**
 * Default implementation of the transaction context.
 */
final class InternalTransactionContext implements TransactionContext {
  private final Fork fork;
  private final HashCode hash;
  private final PublicKey authorPk;

  InternalTransactionContext(Fork fork, HashCode hash, PublicKey authorPk) {
    this.fork = fork;
    this.hash = hash;
    this.authorPk = authorPk;
  }

  @Override
  public Fork getFork() {
    return fork;
  }

  @Override
  public HashCode getTransactionMessageHash() {
    return hash;
  }

  @Override
  public PublicKey getAuthorPk() {
    return authorPk;
  }

}
