/*
 * Copyright 2019 The Exonum Team
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

package com.exonum.binding.testkit;

import static com.exonum.binding.testkit.TestKitTestUtils.ARTIFACT_FILENAME;
import static com.exonum.binding.testkit.TestKitTestUtils.ARTIFACT_ID;
import static com.exonum.binding.testkit.TestKitTestUtils.SERVICE_CONFIGURATION;
import static com.exonum.binding.testkit.TestKitTestUtils.SERVICE_ID;
import static com.exonum.binding.testkit.TestKitTestUtils.SERVICE_NAME;
import static com.exonum.binding.testkit.TestKitTestUtils.createTestServiceArtifact;
import static org.assertj.core.api.Assertions.assertThat;

import com.exonum.binding.core.blockchain.Blockchain;
import com.exonum.binding.core.storage.database.Snapshot;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

class TestKitParameterizationTest {

  private static final short TEMPLATE_VALIDATOR_COUNT = 1;
  private static final EmulatedNodeType TEMPLATE_NODE_TYPE = EmulatedNodeType.VALIDATOR;
  private static final short NEW_VALIDATOR_COUNT = 8;

  @TempDir
  @SuppressWarnings("WeakerAccess") // @TempDir can't be private
  static Path artifactsDirectory;

  @RegisterExtension
  TestKitExtension testKitExtension = new TestKitExtension(
      TestKit.builder()
          .withNodeType(TEMPLATE_NODE_TYPE)
          .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
          .withService(ARTIFACT_ID, SERVICE_NAME, SERVICE_ID, SERVICE_CONFIGURATION)
          .withValidators(TEMPLATE_VALIDATOR_COUNT)
          .withArtifactsDirectory(artifactsDirectory));

  @BeforeAll
  static void setUp() throws IOException {
    createTestServiceArtifact(artifactsDirectory);
  }

  @Test
  void testDefaultTestKit(TestKit testKit) {
    // Check that main TestKit node is a validator
    assertThat(testKit.getEmulatedNode().getNodeType()).isEqualTo(TEMPLATE_NODE_TYPE);
    testKit.withSnapshot(verifyNumValidators(TEMPLATE_VALIDATOR_COUNT));
  }

  @Test
  void testTestKitAuditor(@Auditor TestKit testKit) {
    // Check that main TestKit node is an auditor
    assertThat(testKit.getEmulatedNode().getNodeType()).isEqualTo(EmulatedNodeType.AUDITOR);
  }

  @Test
  void testTestKitValidatorCount(@ValidatorCount(NEW_VALIDATOR_COUNT) TestKit testKit) {
    testKit.withSnapshot(verifyNumValidators(NEW_VALIDATOR_COUNT));
  }

  private static Consumer<Snapshot> verifyNumValidators(int expected) {
    return (view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      assertThat(blockchain.getConsensusConfiguration().getValidatorKeysCount())
          .isEqualTo(expected);
    };
  }
}
