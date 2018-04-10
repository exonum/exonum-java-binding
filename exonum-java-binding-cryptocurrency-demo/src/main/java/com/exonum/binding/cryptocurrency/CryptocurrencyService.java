package com.exonum.binding.cryptocurrency;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.messages.InternalServerError;
import com.exonum.binding.messages.InvalidTransactionException;
import com.exonum.binding.messages.Transaction;

public interface CryptocurrencyService {
  short ID = 42;
  String NAME = "cryptocurrency-demo-service";

  HashCode submitTransaction(Transaction tx)
      throws InvalidTransactionException, InternalServerError;
}
