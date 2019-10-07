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

class TestKitWithTestArtifact {

  static final String ARTIFACT_FILENAME = "test-service.jar";
  static final ServiceArtifactId ARTIFACT_ID =
      ServiceArtifactId.of("com.exonum.binding", "test-service", "1.0.0");
  static final String SERVICE_NAME = "Test service";
  static final int SERVICE_ID = 46;
  static Path artifactsDirectory;

  @BeforeAll
  static void setUp(@TempDir Path tmp) throws Exception {
    artifactsDirectory = tmp;
    Path artifactLocation = artifactsDirectory.resolve(ARTIFACT_FILENAME);
    createArtifact(artifactLocation);
  }

  private static void createArtifact(Path artifactLocation)
      throws IOException {
    new ServiceArtifactBuilder()
        .setPluginId(ARTIFACT_ID.toString())
        .setPluginVersion(ARTIFACT_ID.getVersion())
        .addClasses(TestTransaction.class, TestSchema.class,
            TestService.class)
        .addExtensionClass(TestServiceModule.class)
        .writeTo(artifactLocation);
  }
}
