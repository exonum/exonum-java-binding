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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.util.LibraryLoader;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

class CryptocurrencySchemaIntegrationTest {

  static {
    LibraryLoader.load();
  }

  @Test
  void getStateHashes() throws CloseFailuresException {
    try (MemoryDb db = MemoryDb.newInstance();
         Cleaner cleaner = new Cleaner()) {
      Snapshot view = db.createSnapshot(cleaner);
      CryptocurrencySchema schema = new CryptocurrencySchema(view);

      HashCode walletsMerkleRoot = schema.wallets().getRootHash();
      ImmutableList<HashCode> expectedHashes = ImmutableList.of(walletsMerkleRoot);

      assertThat(schema.getStateHashes(), equalTo(expectedHashes));
    }
  }
}
