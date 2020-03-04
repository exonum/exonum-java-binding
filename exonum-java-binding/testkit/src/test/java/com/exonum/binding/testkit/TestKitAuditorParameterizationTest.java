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

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

// TODO(ECR-???)
@DisabledOnOs(OS.WINDOWS)
class TestKitAuditorParameterizationTest {

  @TempDir
  @SuppressWarnings("WeakerAccess") // @TempDir can't be private
  static Path artifactsDirectory;

  @RegisterExtension
  TestKitExtension testKitAuditorExtension = new TestKitExtension(
      TestKit.builder()
          .withNodeType(EmulatedNodeType.AUDITOR)
          .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
          .withService(ARTIFACT_ID, SERVICE_NAME, SERVICE_ID, SERVICE_CONFIGURATION)
          .withArtifactsDirectory(artifactsDirectory));

  @BeforeAll
  static void setUp() throws IOException {
    createTestServiceArtifact(artifactsDirectory);
  }

  @Test
  void testTestKitValidator(@Validator TestKit testKit) {
    // Check that main TestKit node is a validator
    assertThat(testKit.getEmulatedNode().getNodeType()).isEqualTo(EmulatedNodeType.VALIDATOR);
  }
}
