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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.common.crypto.CryptoFunctions;
import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.storage.indices.ListIndexProxy;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.util.LibraryLoader;
import org.junit.jupiter.api.Test;

@RequiresNativeLibrary
class CryptocurrencySchemaWalletHistoryIntegrationTest {

  static {
    LibraryLoader.load();
  }

  private KeyPair keyPair = CryptoFunctions.ed25519().generateKeyPair();
  private HistoryEntity testTransfer =
      HistoryEntity.Builder.newBuilder()
          .setSeed(1L)
          .setWalletFrom(keyPair.getPublicKey())
          .setWalletTo(keyPair.getPublicKey())
          .setAmount(10L)
          .setTxMessageHash(HashCode.fromString("a0a0a0"))
          .build();

  @Test
  void walletHistoryNoRecords() throws CloseFailuresException {
    try (MemoryDb db = MemoryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      Snapshot view = db.createSnapshot(cleaner);
      CryptocurrencySchema schema = new CryptocurrencySchema(view);

      assertTrue(schema.walletHistory(keyPair.getPublicKey()).isEmpty());
    }
  }

  @Test
  void walletHistoryWithRecords() throws CloseFailuresException {
    try (MemoryDb db = MemoryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      Fork fork = db.createFork(cleaner);
      CryptocurrencySchema schema = new CryptocurrencySchema(fork);

      schema.walletHistory(keyPair.getPublicKey()).add(testTransfer);

      ListIndexProxy<HistoryEntity> history = schema.walletHistory(keyPair.getPublicKey());
      assertFalse(history.isEmpty());

      assertThat(history.get(0)).isEqualTo(testTransfer);
    }
  }

}
