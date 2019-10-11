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

import com.exonum.binding.core.runtime.ServiceArtifactId;
import com.exonum.binding.test.runtime.ServiceArtifactBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Path;

class TestKitTestWithArtifactsCreated {

  static final String ARTIFACT_FILENAME = "test-service.jar";
  static final ServiceArtifactId ARTIFACT_ID =
      ServiceArtifactId.of("com.exonum.binding", "test-service", "1.0.0");
  static final String SERVICE_NAME = "Test service";
  static final int SERVICE_ID = 46;

  static final String ARTIFACT_FILENAME_2 = "test-service-2.jar";
  static final ServiceArtifactId ARTIFACT_ID_2 =
      ServiceArtifactId.of("com.exonum.binding", "test-service-2", "1.0.0");
  static final String SERVICE_NAME_2 = "Test service 2";
  static final int SERVICE_ID_2 = 48;

  static Path artifactsDirectory;

  @BeforeAll
  static void setUp(@TempDir Path tmp) throws IOException {
    artifactsDirectory = tmp;
    createTestServiceArtifact();
    // Although this artifact is needed only for some tests, it is created here so that tests
    // wouldn't create artifacts in the shared directory
    createTestService2Artifact();
  }

  private static void createTestServiceArtifact() throws IOException {
    Path artifactLocation = artifactsDirectory.resolve(ARTIFACT_FILENAME);
    createArtifact(artifactLocation, ARTIFACT_ID, TestServiceModule.class, TestTransaction.class,
        TestSchema.class, TestService.class);
  }

  private static void createTestService2Artifact() throws IOException {
    Path artifactLocation = artifactsDirectory.resolve(ARTIFACT_FILENAME_2);
    createArtifact(artifactLocation, ARTIFACT_ID_2, TestServiceModule2.class,
        TestTransaction.class, TestSchema.class, TestService2.class);
  }

  static void createInvalidArtifact(String filename) throws IOException {
    Path artifactLocation = artifactsDirectory.resolve(filename);
    createArtifact(artifactLocation, ARTIFACT_ID, TestServiceModule.class, TestTransaction.class,
        TestSchema.class);
  }

  private static void createArtifact(Path artifactLocation, ServiceArtifactId serviceArtifactId,
                                     Class serviceModule, Class<?>... artifactClasses) throws IOException {
    new ServiceArtifactBuilder()
        .setPluginId(serviceArtifactId.toString())
        .setPluginVersion(serviceArtifactId.getVersion())
        .addClasses(artifactClasses)
        .addExtensionClass(serviceModule)
        .writeTo(artifactLocation);
  }
}
