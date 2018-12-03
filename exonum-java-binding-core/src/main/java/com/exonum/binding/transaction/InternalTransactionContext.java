package com.exonum.binding.transaction;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.storage.database.Fork;

public class InternalTransactionContext implements TransactionContext {
  private Fork fork;
  private HashCode hash;
  private PublicKey authorPk;


  /**
   * Creates Internal Transaction Context.
   * @param fork fork
   * @param hash hash
   * @param authorPk author private key
   */
  public InternalTransactionContext(Fork fork, HashCode hash, PublicKey authorPk) {
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
