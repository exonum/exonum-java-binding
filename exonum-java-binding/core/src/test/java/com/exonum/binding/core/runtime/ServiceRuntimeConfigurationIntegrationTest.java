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

package com.exonum.binding.core.runtime;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.exonum.binding.common.runtime.ServiceArtifactId;
import com.exonum.binding.core.blockchain.BlockchainData;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.database.TemporaryDb;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.exonum.binding.test.runtime.ServiceArtifactBuilder;
import com.exonum.messages.core.runtime.Lifecycle.InstanceStatus;
import com.exonum.messages.core.runtime.Lifecycle.InstanceStatus.Simple;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * An integration test of the runtime: the runtime module bindings and ServiceRuntime integration
 * with its dependencies.
 */
@RequiresNativeLibrary
class ServiceRuntimeConfigurationIntegrationTest {

  private static final String ARTIFACT_VERSION = "1.0.0";
  private static final ServiceArtifactId ARTIFACT_ID =
      ServiceArtifactId.newJavaId("com.exonum.binding/test-service", ARTIFACT_VERSION);
  private static final String ARTIFACT_FILENAME = "test-service.jar";

  @BeforeEach
  void createValidArtifact(@TempDir Path tmpArtifactDir) throws IOException {
    Path artifactLocation = tmpArtifactDir.resolve(ARTIFACT_FILENAME);
    new ServiceArtifactBuilder()
        .setPluginId(ARTIFACT_ID.toString())
        .setPluginVersion(ARTIFACT_ID.getVersion())
        .addClasses(TestService.class)
        .addExtensionClass(TestServiceModule.class)
        .writeTo(artifactLocation);
  }

  @Test
  void runtimeConfigurationTest(@TempDir Path tmpArtifactDir) throws Exception {
    int port = 0; // any port
    // Use 'production' stage as it involves up-front error checking of the configured bindings
    Injector injector = Guice.createInjector(Stage.PRODUCTION,
        new FrameworkModule(tmpArtifactDir, port, emptyMap()));

    // Create the runtime
    ServiceRuntime runtime = injector.getInstance(ServiceRuntime.class);

    try (TemporaryDb database = TemporaryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      // Initialize it
      runtime.initialize(mock(NodeProxy.class));

      // Deploy the service to the runtime
      runtime.deployArtifact(ARTIFACT_ID, ARTIFACT_FILENAME);
      assertTrue(runtime.isArtifactDeployed(ARTIFACT_ID));

      // Initialize and register a service instance
      String name = "s1";
      ServiceInstanceSpec instanceSpec = ServiceInstanceSpec.newInstance(name, 1, ARTIFACT_ID);
      InstanceStatus instanceStatus = InstanceStatus.newBuilder().setSimple(Simple.ACTIVE).build();
      Fork fork = database.createFork(cleaner);
      BlockchainData blockchainData = BlockchainData.fromRawAccess(fork, name);
      runtime.initiateAddingService(blockchainData, instanceSpec, new byte[0]);
      runtime.updateInstanceStatus(instanceSpec, instanceStatus);
      assertThat(runtime.findService(name)).isNotEmpty();
    }

    // Shutdown the runtime
    runtime.shutdown();
  }
}
