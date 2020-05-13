/*
 * Copyright 2020 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.example.testing;

import static com.exonum.binding.example.testing.IntegrationTestArguments.ARTIFACTS_DIR;
import static com.exonum.binding.example.testing.IntegrationTestArguments.ARTIFACT_FILENAME;
import static com.exonum.binding.example.testing.IntegrationTestArguments.ARTIFACT_ID;
import static org.assertj.core.api.Assertions.assertThat;

import com.exonum.binding.core.blockchain.Blockchain;
import com.exonum.binding.core.blockchain.BlockchainData;
import com.exonum.binding.test.Integration;
import com.exonum.binding.testkit.Auditor;
import com.exonum.binding.testkit.TestKit;
import com.exonum.binding.testkit.TestKitExtension;
import com.exonum.binding.testkit.ValidatorCount;
import com.exonum.messages.core.Blockchain.Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@SuppressWarnings({"EmptyMethod", "squid:S2699"})
@Integration
class TestkitExamples2Test {

  private static final String SERVICE_NAME = "test-service";

  @RegisterExtension
  TestKitExtension testKitExtension = new TestKitExtension(
      TestKit.builder()
          .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
          .withService(ARTIFACT_ID, SERVICE_NAME, 17)
          .withArtifactsDirectory(ARTIFACTS_DIR));

  @Test
  void validatorTest(TestKit testKit) {
    // Injected TestKit has a default configuration, specified in the builder
    // above
  }

  @Test
  void auditorTest(@Auditor @ValidatorCount(8) TestKit testKit) {
    // Injected TestKit has an altered configuration — "auditor" as an emulated
    // node and 8 validator nodes
    BlockchainData blockchainData = testKit.getBlockchainData(SERVICE_NAME);
    Blockchain blockchain = blockchainData.getBlockchain();
    Config consensusConfig = blockchain.getConsensusConfiguration();
    assertThat(consensusConfig.getValidatorKeysCount()).isEqualTo(8);
  }
}
