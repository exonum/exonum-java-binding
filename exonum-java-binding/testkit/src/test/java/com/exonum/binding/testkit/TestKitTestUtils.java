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

import static org.assertj.core.api.Assertions.assertThat;

import com.exonum.binding.core.runtime.DispatcherSchema;
import com.exonum.binding.core.runtime.ServiceArtifactId;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.storage.indices.MapIndex;
import com.exonum.binding.test.runtime.ServiceArtifactBuilder;
import com.exonum.binding.testkit.TestProtoMessages.TestConfiguration;
import com.exonum.core.messages.Runtime.InstanceSpec;
import com.exonum.core.messages.Runtime.InstanceState;
import java.io.IOException;
import java.nio.file.Path;

final class TestKitTestUtils {

  static final String ARTIFACT_FILENAME = "test-service.jar";
  private static final String ARTIFACT_VERSION = "1.0.0";
  static final ServiceArtifactId ARTIFACT_ID =
      ServiceArtifactId.newJavaId("com.exonum.binding/test-service", ARTIFACT_VERSION);
  static final String SERVICE_NAME = "test-service";
  static final int SERVICE_ID = 46;
  static final String CONFIGURATION_VALUE = "Initial value";
  static final TestConfiguration SERVICE_CONFIGURATION = TestConfiguration.newBuilder()
      .setValue(CONFIGURATION_VALUE)
      .build();

  static final String ARTIFACT_FILENAME_2 = "test-service-2.jar";
  private static final String ARTIFACT_VERSION_2 = "2.8.0";
  static final ServiceArtifactId ARTIFACT_ID_2 =
      ServiceArtifactId.newJavaId("com.exonum.binding/test-service-2", ARTIFACT_VERSION_2);
  static final String SERVICE_NAME_2 = "test-service2";
  static final int SERVICE_ID_2 = 48;

  private TestKitTestUtils() {}

  static void createTestServiceArtifact(Path artifactsDirectory) throws IOException {
    Path artifactLocation = artifactsDirectory.resolve(ARTIFACT_FILENAME);
    createArtifact(artifactLocation, ARTIFACT_ID, TestServiceModule.class,
        TestSchema.class, TestService.class);
  }

  static void createTestService2Artifact(Path artifactsDirectory) throws IOException {
    Path artifactLocation = artifactsDirectory.resolve(ARTIFACT_FILENAME_2);
    createArtifact(artifactLocation, ARTIFACT_ID_2, TestServiceModule2.class,
        TestSchema.class, TestService2.class);
  }

  /**
   * Creates an invalid service artifact that has no required metadata.
   */
  static void createInvalidArtifact(Path directory, String filename) throws IOException {
    Path artifactLocation = directory.resolve(filename);
    // Create an invalid artifact with no required metadata (plugin-id) and no classes.
    new ServiceArtifactBuilder()
        .setPluginId("")
        .setPluginVersion("1.0.0")
        .writeTo(artifactLocation);
  }

  static void checkIfServiceEnabled(TestKit testKit, String serviceName, int serviceId) {
    Snapshot snapshot = testKit.getSnapshot();
    MapIndex<String, InstanceState> serviceInstances =
        new DispatcherSchema(snapshot).serviceInstances();
    assertThat(serviceInstances.containsKey(serviceName)).isTrue();

    InstanceSpec serviceSpec = serviceInstances.get(serviceName).getSpec();
    int actualServiceId = serviceSpec.getId();
    assertThat(actualServiceId).isEqualTo(serviceId);
  }

  static void createArtifact(Path artifactLocation, ServiceArtifactId serviceArtifactId,
      Class serviceModule, Class<?>... artifactClasses) throws IOException {
    new ServiceArtifactBuilder()
        .setPluginId(serviceArtifactId.toString())
        .setPluginVersion(serviceArtifactId.getVersion())
        .addClasses(artifactClasses)
        .addExtensionClass(serviceModule)
        .writeTo(artifactLocation);
  }
}
