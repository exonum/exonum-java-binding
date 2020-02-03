/*
 * Copyright 2020 The Exonum Team
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

import static com.exonum.binding.testkit.TestKitTestUtils.createArtifact;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.Hashing;
import com.exonum.binding.common.messages.Service.ServiceConfiguration;
import com.exonum.binding.common.messages.Service.ServiceConfiguration.Format;
import com.exonum.binding.core.runtime.ServiceArtifactId;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class StandardServiceConfigurationTestKitTest {
  private static final String ARTIFACT_FILENAME = "test-service-props-configuration.jar";
  private static final String ARTIFACT_VERSION = "1.0.0";
  static final ServiceArtifactId ARTIFACT_ID =
      ServiceArtifactId
          .newJavaId("com.exonum.binding/test-service-props-configuration", ARTIFACT_VERSION);
  private static final String SERVICE_NAME = "test-service-props-configuration";
  private static final int SERVICE_ID = 77;

  @TempDir
  @SuppressWarnings("WeakerAccess") // @TempDir can't be private
  static Path artifactsDirectory;

  @BeforeAll
  static void setUp() throws IOException {
    Path artifactLocation = artifactsDirectory.resolve(ARTIFACT_FILENAME);
    createArtifact(artifactLocation, ARTIFACT_ID, TestServiceModuleStandardConfiguration.class,
        TestSchema.class, TestServiceStandardConfiguration.class);
  }

  @Test
  void createTestKitWithStandardPropertiesConfiguration() {
    ServiceConfiguration testConfiguration = ServiceConfiguration.newBuilder()
        .setValue("key1=value1\nkey2=value2")
        .setFormat(Format.PROPERTIES)
        .build();

    try (TestKit testKit = TestKit.builder()
        .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
        .withService(ARTIFACT_ID, SERVICE_NAME, SERVICE_ID, testConfiguration)
        .withArtifactsDirectory(artifactsDirectory)
        .build()) {
      // Check that configuration value is used in initialization
      Snapshot view = testKit.getSnapshot();
      TestSchema testSchema = new TestSchema(view, SERVICE_ID);
      ProofMapIndexProxy<HashCode, String> testProofMap = testSchema.testMap();
      Map<HashCode, String> testMap = Maps.toMap(testProofMap.keys(), testProofMap::get);
      Map<HashCode, String> expected = ImmutableMap.of(
          toMapKey("key1"), "value1",
          toMapKey("key2"), "value2"
      );
      assertThat(testMap).isEqualTo(expected);
    }
  }

  @Test
  void createTestKitWithWrongConfiguration() {
    ServiceConfiguration testConfiguration = ServiceConfiguration.newBuilder()
        .setValue("wrong config value")
        .setFormat(Format.TEXT)
        .build();

    RuntimeException exception = assertThrows(RuntimeException.class,
        () -> TestKit.builder()
            .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
            .withService(ARTIFACT_ID, SERVICE_NAME, SERVICE_ID, testConfiguration)
            .withArtifactsDirectory(artifactsDirectory)
            .build()
    );
    assertThat(exception)
        .hasMessageContaining("Expected configuration in PROPERTIES format, but actual was TEXT");
  }

  private static HashCode toMapKey(String key) {
    return Hashing.defaultHashFunction().hashString(key, StandardCharsets.UTF_8);
  }
}
