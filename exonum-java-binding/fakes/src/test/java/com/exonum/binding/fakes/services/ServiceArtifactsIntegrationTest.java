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

package com.exonum.binding.fakes.services;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.app.ServiceRuntimeBootstrap;
import com.exonum.binding.core.runtime.ServiceArtifactId;
import com.exonum.binding.core.runtime.ServiceLoadingException;
import com.exonum.binding.core.runtime.ServiceRuntime;
import com.exonum.binding.core.service.adapters.UserServiceAdapter;
import com.exonum.binding.fakes.services.service.TestService;
import com.exonum.binding.test.CiOnly;
import com.exonum.binding.test.RequiresNativeLibrary;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@CiOnly
@RequiresNativeLibrary
class ServiceArtifactsIntegrationTest {

  private static final String ARTIFACT_FILENAME = "test-service.jar";
  private Path artifactLocation;
  private ServiceRuntime serviceRuntime;

  @BeforeEach
  void setUp(@TempDir Path tmp) {
    artifactLocation = tmp.resolve(ARTIFACT_FILENAME);
    serviceRuntime = ServiceRuntimeBootstrap.createServiceRuntime(tmp.toString(), 0);
  }

  @AfterEach
  void tearDown() throws InterruptedException {
    serviceRuntime.close();
  }

  @Test
  void createValidArtifact() throws IOException, ServiceLoadingException {
    ServiceArtifactId id =
        ServiceArtifactId.newJavaId("com.exonum.binding:valid-test-service:1.0.0");
    ServiceArtifacts.createValidArtifact(id, artifactLocation);

    serviceRuntime.deployArtifact(id, ARTIFACT_FILENAME);

    UserServiceAdapter service = serviceRuntime.addService(fork, id.toString(), configuration);

    assertThat(service.getId(), equalTo(TestService.ID));
  }

  @Test
  void createUnloadableArtifact() throws IOException {
    String id = "1:com.exonum.binding:unloadable-test-service:1.0.0";
    ServiceArtifacts.createUnloadableArtifact(id, artifactLocation);
    assertThrows(
        ServiceLoadingException.class,
        () -> serviceRuntime.deployArtifact(ServiceArtifactId.parseFrom(id), ARTIFACT_FILENAME));
  }

  @Test
  void createWithUninstantiableService() throws IOException, ServiceLoadingException {
    ServiceArtifactId id =
        ServiceArtifactId.newJavaId("com.exonum.binding:uninstantiable-test-service:1.0.0");
    ServiceArtifacts.createWithUninstantiableService(id, artifactLocation);

    serviceRuntime.deployArtifact(id, ARTIFACT_FILENAME);

    assertThrows(
        RuntimeException.class,
        () -> serviceRuntime.addService(fork, id.toString(), configuration));
  }
}
