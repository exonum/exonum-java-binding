/*
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.cryptocurrency;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.service.Schema;
import com.exonum.binding.core.storage.database.Access;
import com.exonum.binding.core.storage.indices.IndexAddress;
import com.exonum.binding.core.storage.indices.ListIndexProxy;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.cryptocurrency.transactions.TxMessageProtos;

/** A schema of the cryptocurrency service. */
public final class CryptocurrencySchema implements Schema {

  /** A namespace of cryptocurrency service collections. */
  private final String namespace;

  private final Access access;

  public CryptocurrencySchema(Access access, String serviceName) {
    this.access = checkNotNull(access);
    this.namespace = serviceName + ".";
  }

  /** Returns a proof map of wallets. Note that this is a proof map that uses non-hashed keys. */
  public ProofMapIndexProxy<PublicKey, Wallet> wallets() {
    String name = fullIndexName("wallets");
    return access.getRawProofMap(
        IndexAddress.valueOf(name), StandardSerializers.publicKey(), WalletSerializer.INSTANCE);
  }

  /**
   * Returns transactions history of the wallet. It contains hashes of {@link
   * TxMessageProtos.TransferTx} transaction messages that changed the balance of the given wallet.
   *
   * @param walletId wallet address
   */
  public ListIndexProxy<HashCode> transactionsHistory(PublicKey walletId) {
    String name = fullIndexName("transactions_history");
    IndexAddress address = IndexAddress.valueOf(name, walletId.toBytes());
    return access.getList(address, StandardSerializers.hash());
  }

  private String fullIndexName(String name) {
    return namespace + name;
  }
}
