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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.fakes.services.service.TestService;
import com.exonum.binding.runtime.ServiceLoadingException;
import com.exonum.binding.runtime.ServiceRuntime;
import com.exonum.binding.runtime.ServiceRuntimeBootstrap;
import com.exonum.binding.service.adapters.UserServiceAdapter;
import com.exonum.binding.test.CiOnly;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@CiOnly
class ServiceArtifactsTest {

  // todo: Is it OK to make the things public?
  private Path artifactLocation;
  private ServiceRuntime serviceRuntime;

  @BeforeEach
  void setUp(@TempDir Path tmp) {
    artifactLocation = tmp.resolve("service.jar");
    serviceRuntime = ServiceRuntimeBootstrap.createServiceRuntime(0);
  }

  @Test
  void createValidArtifact() throws IOException, ServiceLoadingException {
    ServiceArtifacts.createValidArtifact(artifactLocation);

    String artifactId = serviceRuntime.loadArtifact(artifactLocation.toString());

    UserServiceAdapter service = serviceRuntime.createService(artifactId);

    assertThat(service.getId(), equalTo(TestService.ID));
  }

  @Test
  void createUnloadableArtifact() throws IOException {
    ServiceArtifacts.createUnloadableArtifact(artifactLocation);
    assertThrows(ServiceLoadingException.class,
        () -> serviceRuntime.loadArtifact(artifactLocation.toString()));
  }

  @Test
  void createWithUninstantiableService() throws IOException, ServiceLoadingException {
    ServiceArtifacts.createWithUninstantiableService(artifactLocation);

    String artifactId = serviceRuntime.loadArtifact(artifactLocation.toString());

    assertThrows(RuntimeException.class, () -> serviceRuntime.createService(artifactId));
  }
}
