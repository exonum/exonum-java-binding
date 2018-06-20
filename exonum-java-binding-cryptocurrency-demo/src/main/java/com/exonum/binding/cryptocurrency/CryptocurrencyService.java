package com.exonum.binding.cryptocurrency;

import com.exonum.binding.crypto.PublicKey;
import com.exonum.binding.hash.HashCode;
import com.exonum.binding.messages.Transaction;
import java.util.Optional;

public interface CryptocurrencyService {
  short ID = 42;
  String NAME = "cryptocurrency-demo-service";

  HashCode submitTransaction(Transaction tx);

  Optional<Wallet> getValue(PublicKey ownerKey);
}
