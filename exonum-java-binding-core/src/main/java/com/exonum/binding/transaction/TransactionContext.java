package com.exonum.binding.transaction;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.storage.database.Fork;

public interface TransactionContext {
  Fork getFork();

  HashCode getTransactionMessageHash();

  PublicKey getAuthorPk();
}
