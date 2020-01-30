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

import static com.exonum.binding.cryptocurrency.PredefinedServiceParameters.ARTIFACT_FILENAME;
import static com.exonum.binding.cryptocurrency.PredefinedServiceParameters.ARTIFACT_ID;
import static com.exonum.binding.cryptocurrency.PredefinedServiceParameters.SERVICE_ID;
import static com.exonum.binding.cryptocurrency.PredefinedServiceParameters.SERVICE_NAME;
import static com.exonum.binding.cryptocurrency.PredefinedServiceParameters.artifactsDirectory;
import static org.assertj.core.api.Assertions.assertThat;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.core.blockchain.BlockchainData;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.testkit.TestKit;
import com.exonum.binding.testkit.TestKitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@RequiresNativeLibrary
class CryptocurrencySchemaIntegrationTest {

  @RegisterExtension
  TestKitExtension testKitExtension = new TestKitExtension(
      TestKit.builder()
          .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
          .withService(ARTIFACT_ID, SERVICE_NAME, SERVICE_ID)
          .withArtifactsDirectory(artifactsDirectory));

  private static final PublicKey WALLET_OWNER_KEY =
      PredefinedOwnerKeys.FIRST_OWNER_KEY_PAIR.getPublicKey();

  @Test
  void walletHistoryNoRecords(TestKit testKit) {
    BlockchainData blockchainData = testKit.getBlockchainData(SERVICE_NAME);
    CryptocurrencySchema schema =
        new CryptocurrencySchema(blockchainData.getExecutingServiceData());

    assertThat(schema.transactionsHistory(WALLET_OWNER_KEY)).isEmpty();
  }
}
