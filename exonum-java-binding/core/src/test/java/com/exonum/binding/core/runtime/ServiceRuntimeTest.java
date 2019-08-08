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

import static com.exonum.binding.test.Bytes.bytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.CloseFailuresException;
import com.exonum.binding.core.storage.database.Database;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.database.TemporaryDb;
import com.exonum.binding.core.transaction.TransactionContext;
import com.exonum.binding.core.transport.Server;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.SAME_THREAD) // MockitoExtension is not thread-safe: see mockito/1630
class ServiceRuntimeTest {

  static final int PORT = 25000;
  static final String TEST_NAME = "test_service_name";
  static final int TEST_ID = 17;
  static final HashCode TEST_HASH = HashCode.fromBytes(bytes(1, 2, 3));
  static final PublicKey TEST_PUBLIC_KEY = PublicKey.fromBytes(bytes(4, 5, 6));

  @Mock
  private ServiceLoader serviceLoader;
  @Mock
  private ServicesFactory servicesFactory;
  @Mock
  private Server server;
  private ServiceRuntime serviceRuntime;

  @BeforeEach
  void setUp() {
    serviceRuntime = new ServiceRuntime(serviceLoader, servicesFactory, server, PORT);
  }

  @Test
  void startsServerOnInstantiation() {
    verify(server).start(PORT);
  }

  @Test
  void deployCorrectArtifact() throws Exception {
    ServiceArtifactId serviceId = ServiceArtifactId.of("com.acme", "foo-service", "1.0.0");
    Path serviceArtifactLocation = Paths.get("/tmp/foo-service.jar");
    LoadedServiceDefinition serviceDefinition = LoadedServiceDefinition
        .newInstance(serviceId, TestServiceModule::new);
    when(serviceLoader.loadService(serviceArtifactLocation))
        .thenReturn(serviceDefinition);

    serviceRuntime.deployArtifact(serviceId, serviceArtifactLocation);

    verify(serviceLoader).loadService(serviceArtifactLocation);
  }

  @Test
  void deployArtifactWrongId() throws Exception {
    ServiceArtifactId actualId = ServiceArtifactId.of("com.acme", "actual", "1.0.0");
    Path serviceArtifactLocation = Paths.get("/tmp/foo-service.jar");
    LoadedServiceDefinition serviceDefinition = LoadedServiceDefinition
        .newInstance(actualId, TestServiceModule::new);
    when(serviceLoader.loadService(serviceArtifactLocation))
        .thenReturn(serviceDefinition);

    ServiceArtifactId expectedId = ServiceArtifactId.of("com.acme", "expected", "1.0.0");

    Exception actual = assertThrows(ServiceLoadingException.class,
        () -> serviceRuntime.deployArtifact(expectedId, serviceArtifactLocation));
    assertThat(actual).hasMessageContainingAll(actualId.toString(), expectedId.toString(),
        serviceArtifactLocation.toString());

    // Check the service artifact is unloaded
    verify(serviceLoader).unloadService(actualId);
  }

  @Test
  void deployArtifactFailed() throws Exception {
    ServiceArtifactId serviceId = ServiceArtifactId.of("com.acme", "foo-service", "1.0.0");
    Path serviceArtifactLocation = Paths.get("/tmp/foo-service.jar");
    ServiceLoadingException exception = new ServiceLoadingException("Boom");
    when(serviceLoader.loadService(serviceArtifactLocation))
        .thenThrow(exception);

    Exception actual = assertThrows(ServiceLoadingException.class,
        () -> serviceRuntime.deployArtifact(serviceId, serviceArtifactLocation));
    assertThat(actual).isSameAs(exception);
  }

  @Test
  void createService() {
    ServiceArtifactId artifactId = ServiceArtifactId.parseFrom("com.acme:foo-service:1.0.0");
    LoadedServiceDefinition serviceDefinition = LoadedServiceDefinition
        .newInstance(artifactId, TestServiceModule::new);
    ServiceInstanceSpec instanceSpec = ServiceInstanceSpec.newInstance(TEST_NAME,
        TEST_ID, artifactId);
    when(serviceLoader.findService(artifactId))
        .thenReturn(Optional.of(serviceDefinition));

    ServiceWrapper serviceWrapper = mock(ServiceWrapper.class);
    when(servicesFactory.createService(serviceDefinition, instanceSpec))
        .thenReturn(serviceWrapper);

    // Create the service from the artifact
    serviceRuntime.createService(instanceSpec);

    // Check it was instantiated as expected
    verify(servicesFactory).createService(serviceDefinition, instanceSpec);

    // and is present in the runtime
    Optional<ServiceWrapper> serviceOpt = serviceRuntime.findService(TEST_NAME);
    assertThat(serviceOpt).hasValue(serviceWrapper);
  }

  @Test
  void createServiceDuplicate() {
    ServiceArtifactId artifactId = ServiceArtifactId.parseFrom("com.acme:foo-service:1.0.0");
    LoadedServiceDefinition serviceDefinition = LoadedServiceDefinition
        .newInstance(artifactId, TestServiceModule::new);
    ServiceInstanceSpec instanceSpec = ServiceInstanceSpec.newInstance(TEST_NAME,
        TEST_ID, artifactId);
    when(serviceLoader.findService(artifactId))
        .thenReturn(Optional.of(serviceDefinition));

    ServiceWrapper serviceWrapper = mock(ServiceWrapper.class);
    when(servicesFactory.createService(serviceDefinition, instanceSpec))
        .thenReturn(serviceWrapper);

    // Create the service from the artifact
    serviceRuntime.createService(instanceSpec);

    // Try to create another service with the same service instance specification
    Exception e = assertThrows(IllegalArgumentException.class,
        () -> serviceRuntime.createService(instanceSpec));

    assertThat(e).hasMessageContaining("name");
    assertThat(e).hasMessageContaining(TEST_NAME);

    // Check the service was instantiated only once
    verify(servicesFactory).createService(serviceDefinition, instanceSpec);
  }

  @Test
  void createServiceUnknownService() {
    ServiceArtifactId artifactId = ServiceArtifactId.parseFrom("com.acme:foo-service:1.0.0");
    when(serviceLoader.findService(artifactId)).thenReturn(Optional.empty());

    ServiceInstanceSpec instanceSpec = ServiceInstanceSpec.newInstance(TEST_NAME,
        TEST_ID, artifactId);

    Exception e = assertThrows(IllegalArgumentException.class,
        () -> serviceRuntime.createService(instanceSpec));
    assertThat(e).hasMessageFindingMatch("Unknown.+artifact");
    assertThat(e).hasMessageContaining(String.valueOf(artifactId));
  }

  @Test
  void configureService() throws CloseFailuresException {
    // todo: (here and elsewhere) Extract in some setup method as we have quite some tests
    //  requiring a service? Or even make two nested tests: WithDeployed and WithStarted
    ServiceArtifactId artifactId = ServiceArtifactId.parseFrom("com.acme:foo-service:1.0.0");
    LoadedServiceDefinition serviceDefinition = LoadedServiceDefinition
        .newInstance(artifactId, TestServiceModule::new);
    ServiceInstanceSpec instanceSpec = ServiceInstanceSpec.newInstance(TEST_NAME,
        TEST_ID, artifactId);
    when(serviceLoader.findService(artifactId))
        .thenReturn(Optional.of(serviceDefinition));

    ServiceWrapper serviceWrapper = mock(ServiceWrapper.class);
    when(servicesFactory.createService(serviceDefinition, instanceSpec))
        .thenReturn(serviceWrapper);

    // Create the service from the artifact
    serviceRuntime.createService(instanceSpec);

    // Configure the service
    try (Database database = TemporaryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      Fork view = database.createFork(cleaner);
      Properties properties = new Properties();
      serviceRuntime.configureService(TEST_NAME, view, properties);

      // Check the service was configured
      verify(serviceWrapper).configure(view, properties);
    }
  }

  @Test
  void configureNonExistingService() throws CloseFailuresException {
    try (Database database = TemporaryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      Fork view = database.createFork(cleaner);
      Properties properties = new Properties();

      // Configure the service
      Exception e = assertThrows(IllegalArgumentException.class,
          () -> serviceRuntime.configureService(TEST_NAME, view, properties));

      assertThat(e).hasMessageContaining(TEST_NAME);
    }
  }

  @Test
  void stopService() {
    ServiceArtifactId artifactId = ServiceArtifactId.parseFrom("com.acme:foo-service:1.0.0");
    LoadedServiceDefinition serviceDefinition = LoadedServiceDefinition
        .newInstance(artifactId, TestServiceModule::new);
    when(serviceLoader.findService(artifactId))
        .thenReturn(Optional.of(serviceDefinition));

    ServiceInstanceSpec instanceSpec = ServiceInstanceSpec.newInstance(TEST_NAME,
        TEST_ID, artifactId);
    ServiceWrapper serviceWrapper = mock(ServiceWrapper.class);
    when(servicesFactory.createService(serviceDefinition, instanceSpec))
        .thenReturn(serviceWrapper);

    // Create the service from the artifact
    serviceRuntime.createService(instanceSpec);

    // Stop the service
    serviceRuntime.stopService(TEST_NAME);

    // Check no service with such name remains registered
    Optional<ServiceWrapper> serviceOpt = serviceRuntime.findService(TEST_NAME);
    assertThat(serviceOpt).isEmpty();
  }

  @Test
  void stopNonExistingService() {
    assertThrows(IllegalArgumentException.class, () -> serviceRuntime.stopService(TEST_NAME));
  }

  @Nested
  class WithSingleService {
    final ServiceArtifactId ARTIFACT_ID = ServiceArtifactId.parseFrom("com.acme:foo-service:1.0.0");
    final ServiceInstanceSpec INSTANCE_SPEC = ServiceInstanceSpec.newInstance(TEST_NAME,
        TEST_ID, ARTIFACT_ID);

    @Mock
    ServiceWrapper serviceWrapper;

    @BeforeEach
    void addService() {
      LoadedServiceDefinition serviceDefinition = LoadedServiceDefinition
          .newInstance(ARTIFACT_ID, TestServiceModule::new);
      when(serviceLoader.findService(ARTIFACT_ID))
          .thenReturn(Optional.of(serviceDefinition));
      when(servicesFactory.createService(serviceDefinition, INSTANCE_SPEC))
          .thenReturn(serviceWrapper);

      // Create the service from the artifact
      serviceRuntime.createService(INSTANCE_SPEC);
    }

    @Test
    void executeTransaction() throws Exception {
      try (Database database = TemporaryDb.newInstance();
          Cleaner cleaner = new Cleaner()) {
        int txId = 1;
        byte[] arguments = bytes(127);
        TransactionContext context = TransactionContext.builder()
            .fork(database.createFork(cleaner))
            .txMessageHash(TEST_HASH)
            .authorPk(TEST_PUBLIC_KEY)
            .build();

        serviceRuntime.executeTransaction(TEST_ID, txId, arguments, context);

        verify(serviceWrapper).executeTransaction(txId, arguments, context);
      }
    }

    @Test
    void executeTransactionUnknownService() throws Exception {
      try (Database database = TemporaryDb.newInstance();
          Cleaner cleaner = new Cleaner()) {
        int serviceId = TEST_ID + 1;
        int txId = 1;
        byte[] arguments = bytes(127);
        TransactionContext context = TransactionContext.builder()
            .fork(database.createFork(cleaner))
            .txMessageHash(TEST_HASH)
            .authorPk(TEST_PUBLIC_KEY)
            .build();

        Exception e = assertThrows(IllegalArgumentException.class,
            () -> serviceRuntime.executeTransaction(serviceId, txId, arguments, context));

        assertThat(e).hasMessageContaining(String.valueOf(serviceId));
      }
    }
  }
}
