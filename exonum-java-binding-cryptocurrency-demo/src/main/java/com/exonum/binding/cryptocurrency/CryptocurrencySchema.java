package com.exonum.binding.cryptocurrency;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.service.Schema;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.storage.indices.MapIndexProxy;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.storage.serialization.StandardSerializers;
import com.google.errorprone.annotations.MustBeClosed;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A schema of the cryptocurrency service.
 *
 * <p>Has one collection:
 * Wallets (names and values) (Merklized)
 */
public final class CryptocurrencySchema implements Schema {

  /** A namespace of cryptocurrency service collections. */
  private static final String NAMESPACE = CryptocurrencyService.NAME.replace('-', '_');

  private final View view;

  public CryptocurrencySchema(View view) {
    this.view = checkNotNull(view);
  }

  @MustBeClosed
  public ProofMapIndexProxy<HashCode, Wallet> wallets() {
    String name = fullIndexName("wallets");
    return new ProofMapIndexProxy<>(name, view, StandardSerializers.hash(),
            WalletSerializer.INSTANCE);
  }

  private static String fullIndexName(String name) {
    return NAMESPACE + "__" + name;
  }
}
