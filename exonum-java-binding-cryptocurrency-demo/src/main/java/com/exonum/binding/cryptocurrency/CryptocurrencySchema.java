package com.exonum.binding.cryptocurrency;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.service.Schema;
import com.exonum.binding.storage.database.ViewProxy;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.storage.serialization.StandardSerializers;
import com.google.errorprone.annotations.MustBeClosed;

/**
 * A schema of the cryptocurrency service.
 *
 * <p>Has one collection: Wallets (names and values) (Merklized)
 */
public final class CryptocurrencySchema implements Schema {

  /** A namespace of cryptocurrency service collections. */
  private static final String NAMESPACE = CryptocurrencyService.NAME.replace('-', '_');

  private final ViewProxy view;

  public CryptocurrencySchema(ViewProxy view) {
    this.view = checkNotNull(view);
  }

  private static String fullIndexName(String name) {
    return NAMESPACE + "__" + name;
  }

  /**
   * Returns a proof map of wallets. Must be closed.
   */
  @MustBeClosed
  public ProofMapIndexProxy<HashCode, Wallet> wallets() {
    String name = fullIndexName("wallets");
    return new ProofMapIndexProxy<>(
        name, view, StandardSerializers.hash(), WalletSerializer.INSTANCE);
  }
}
