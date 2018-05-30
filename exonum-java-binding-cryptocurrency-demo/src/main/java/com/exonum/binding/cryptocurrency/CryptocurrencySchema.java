package com.exonum.binding.cryptocurrency;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.crypto.PublicKey;
import com.exonum.binding.hash.HashCode;
import com.exonum.binding.service.Schema;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.storage.serialization.StandardSerializers;
import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 * A schema of the cryptocurrency service.
 *
 * <p>Has one collection: Wallets (names and values) (Merklized)
 */
public final class CryptocurrencySchema implements Schema {

  /** A namespace of cryptocurrency service collections. */
  private static final String NAMESPACE = CryptocurrencyService.NAME.replace('-', '_');

  private final View view;

  public CryptocurrencySchema(View view) {
    this.view = checkNotNull(view);
  }

  @Override
  public List<HashCode> getStateHashes() {
    return ImmutableList.of(wallets().getRootHash());
  }

  /**
   * Returns a proof map of wallets.
   */
  public ProofMapIndexProxy<PublicKey, Wallet> wallets() {
    String name = fullIndexName("wallets");
    return ProofMapIndexProxy.newInstance(name, view, PublicKeySerializer.INSTANCE,
        WalletSerializer.INSTANCE);
  }

  private static String fullIndexName(String name) {
    return NAMESPACE + "__" + name;
  }
}
