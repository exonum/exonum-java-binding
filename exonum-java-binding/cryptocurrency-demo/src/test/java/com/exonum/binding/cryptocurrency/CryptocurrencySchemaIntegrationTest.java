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

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.testkit.TestKit;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

@RequiresNativeLibrary
class CryptocurrencySchemaIntegrationTest {

  private static final PublicKey WALLET_OWNER_KEY =
      PredefinedOwnerKeys.FIRST_OWNER_KEY_PAIR.getPublicKey();

  @Test
  void getStateHashes() {
    try (TestKit testKit = TestKit.forService(CryptocurrencyServiceModule.class)) {
      testKit.withSnapshot((view) -> {
        CryptocurrencySchema schema = new CryptocurrencySchema(view);

        HashCode walletsMerkleRoot = schema.wallets().getRootHash();
        ImmutableList<HashCode> expectedHashes = ImmutableList.of(walletsMerkleRoot);

        assertThat(schema.getStateHashes()).isEqualTo(expectedHashes);
        return null;
      });
    }
  }

  @Test
  void walletHistoryNoRecords() {
    try (TestKit testKit = TestKit.forService(CryptocurrencyServiceModule.class)) {
      testKit.withSnapshot((view) -> {
        CryptocurrencySchema schema = new CryptocurrencySchema(view);

        assertThat(schema.transactionsHistory(WALLET_OWNER_KEY)).isEmpty();
        return null;
      });
    }
  }
}
