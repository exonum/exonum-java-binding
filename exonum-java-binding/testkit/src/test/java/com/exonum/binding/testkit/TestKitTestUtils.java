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
import com.exonum.binding.core.storage.database.View;
import com.exonum.binding.core.storage.indices.MapIndex;
import com.exonum.binding.test.runtime.ServiceArtifactBuilder;
import com.exonum.binding.testkit.TestProtoMessages.TestConfiguration;
import com.exonum.core.messages.Runtime.InstanceSpec;
import java.io.IOException;
import java.nio.file.Path;

final class TestKitTestUtils {

  static final String ARTIFACT_FILENAME = "test-service.jar";
  private static final String ARTIFACT_VERSION = "1.0.0";
  static final ServiceArtifactId ARTIFACT_ID =
      ServiceArtifactId.newJavaId("com.exonum.binding:test-service:" + ARTIFACT_VERSION);
  static final String SERVICE_NAME = "test-service";
  static final int SERVICE_ID = 46;
  static final String CONFIGURATION_VALUE = "Initial value";
  static final TestConfiguration SERVICE_CONFIGURATION = TestConfiguration.newBuilder()
      .setValue(CONFIGURATION_VALUE)
      .build();

  static final String ARTIFACT_FILENAME_2 = "test-service-2.jar";
  private static final String ARTIFACT_VERSION_2 = "2.8.0";
  static final ServiceArtifactId ARTIFACT_ID_2 =
      ServiceArtifactId.newJavaId("com.exonum.binding:test-service-2:" + ARTIFACT_VERSION_2);
  static final String SERVICE_NAME_2 = "test-service2";
  static final int SERVICE_ID_2 = 48;

  private TestKitTestUtils() {}

  static void createTestServiceArtifact(Path artifactsDirectory) throws IOException {
    Path artifactLocation = artifactsDirectory.resolve(ARTIFACT_FILENAME);
    createArtifact(artifactLocation, ARTIFACT_ID, ARTIFACT_VERSION, TestServiceModule.class,
        TestTransaction.class, TestSchema.class, TestService.class);
  }

  static void createTestService2Artifact(Path artifactsDirectory) throws IOException {
    Path artifactLocation = artifactsDirectory.resolve(ARTIFACT_FILENAME_2);
    createArtifact(artifactLocation, ARTIFACT_ID_2, ARTIFACT_VERSION_2, TestServiceModule2.class,
        TestTransaction.class, TestSchema.class, TestService2.class);
  }

  static void createInvalidArtifact(Path directory, String filename) throws IOException {
    Path artifactLocation = directory.resolve(filename);
    // Create an invalid artifact without a TestService class
    createArtifact(artifactLocation, ARTIFACT_ID, ARTIFACT_VERSION, TestServiceModule.class,
        TestTransaction.class, TestSchema.class);
  }

  static void checkIfServiceEnabled(TestKit testKit, String serviceName, int serviceId) {
    View view = testKit.getSnapshot();
    MapIndex<String, InstanceSpec> serviceInstances =
        new DispatcherSchema(view).serviceInstances();
    assertThat(serviceInstances.containsKey(serviceName)).isTrue();

    InstanceSpec serviceSpec = serviceInstances.get(serviceName);
    int actualServiceId = serviceSpec.getId();
    assertThat(actualServiceId).isEqualTo(serviceId);
  }

  private static void createArtifact(Path artifactLocation, ServiceArtifactId serviceArtifactId,
                                     String serviceArtifactVersion, Class serviceModule,
                                     Class<?>... artifactClasses) throws IOException {
    new ServiceArtifactBuilder()
        .setPluginId(serviceArtifactId.getName())
        .setPluginVersion(serviceArtifactVersion)
        .addClasses(artifactClasses)
        .addExtensionClass(serviceModule)
        .writeTo(artifactLocation);
  }
}
