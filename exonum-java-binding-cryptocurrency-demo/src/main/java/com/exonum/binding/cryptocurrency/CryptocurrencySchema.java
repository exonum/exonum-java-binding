/* 
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.cryptocurrency;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.crypto.PublicKey;
import com.exonum.binding.crypto.PublicKeySerializer;
import com.exonum.binding.hash.HashCode;
import com.exonum.binding.service.Schema;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
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
