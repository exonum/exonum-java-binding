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
import static com.google.common.collect.Comparators.isInStrictOrder;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.CloseFailuresException;
import com.exonum.binding.core.runtime.ServiceRuntimeProtos.ServiceRuntimeStateHashes;
import com.exonum.binding.core.runtime.ServiceRuntimeProtos.ServiceStateHashes;
import com.exonum.binding.core.service.BlockCommittedEvent;
import com.exonum.binding.core.storage.database.Database;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.storage.database.TemporaryDb;
import com.exonum.binding.core.transaction.TransactionContext;
import com.exonum.binding.core.transport.Server;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.SAME_THREAD) // MockitoExtension is not thread-safe: see mockito/1630
class ServiceRuntimeIntegrationTest {

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
    when(serviceWrapper.getId()).thenReturn(TEST_ID);
    when(serviceWrapper.getName()).thenReturn(TEST_NAME);
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
    when(serviceWrapper.getId()).thenReturn(TEST_ID);
    when(serviceWrapper.getName()).thenReturn(TEST_NAME);
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
  void configureNonExistingService() throws CloseFailuresException {
    try (Database database = TemporaryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      Fork view = database.createFork(cleaner);
      Properties properties = new Properties();

      // Configure the service
      Exception e = assertThrows(IllegalArgumentException.class,
          () -> serviceRuntime.configureService(TEST_ID, view, properties));

      assertThat(e).hasMessageContaining(String.valueOf(TEST_ID));
    }
  }

  @Test
  void stopNonExistingService() {
    assertThrows(IllegalArgumentException.class, () -> serviceRuntime.stopService(TEST_ID));
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
      // Setup the service
      when(serviceWrapper.getId()).thenReturn(TEST_ID);
      when(serviceWrapper.getName()).thenReturn(TEST_NAME);
      // Setup the loader
      LoadedServiceDefinition serviceDefinition = LoadedServiceDefinition
          .newInstance(ARTIFACT_ID, TestServiceModule::new);
      when(serviceLoader.findService(ARTIFACT_ID))
          .thenReturn(Optional.of(serviceDefinition));
      // Setup the factory
      when(servicesFactory.createService(serviceDefinition, INSTANCE_SPEC))
          .thenReturn(serviceWrapper);

      // Create the service from the artifact
      serviceRuntime.createService(INSTANCE_SPEC);
    }

    @Test
    void configureService() throws CloseFailuresException {
      try (Database database = TemporaryDb.newInstance();
          Cleaner cleaner = new Cleaner()) {
        Fork view = database.createFork(cleaner);
        Properties properties = new Properties();
        // Configure the service
        serviceRuntime.configureService(TEST_ID, view, properties);

        // Check the service was configured
        verify(serviceWrapper).configure(view, properties);
      }
    }

    @Test
    void stopService() {
      // Stop the service
      serviceRuntime.stopService(TEST_ID);

      // Check no service with such name remains registered
      Optional<ServiceWrapper> serviceOpt = serviceRuntime.findService(TEST_NAME);
      assertThat(serviceOpt).isEmpty();
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

    @Test
    void getStateHashesSingleService() throws CloseFailuresException {
      try (Database database = TemporaryDb.newInstance();
          Cleaner cleaner = new Cleaner()) {
        Snapshot s = database.createSnapshot(cleaner);
        List<HashCode> serviceStateHashes = asList(HashCode.fromBytes(bytes(1, 2)),
            HashCode.fromBytes(bytes(3, 4)));
        when(serviceWrapper.getStateHashes(s)).thenReturn(serviceStateHashes);

        List<ByteString> serviceStateHashesAsBytes = serviceStateHashes.stream()
            .map(hash -> ByteString.copyFrom(hash.asBytes()))
            .collect(toList());
        ServiceRuntimeStateHashes expected = ServiceRuntimeStateHashes.newBuilder()
            .addServiceStateHashes(ServiceStateHashes.newBuilder()
                .setInstanceId(TEST_ID)
                .addAllStateHashes(serviceStateHashesAsBytes))
            .build();

        ServiceRuntimeStateHashes runtimeStateHashes = serviceRuntime.getStateHashes(s);
        assertThat(runtimeStateHashes).isEqualTo(expected);
      }
    }

    @Test
    void afterCommitSingleService() {
      BlockCommittedEvent event = mock(BlockCommittedEvent.class);

      serviceRuntime.afterCommit(event);

      verify(serviceWrapper).afterCommit(event);
    }

    @Test
    void afterCommitSingleServiceThrowingException() {
      BlockCommittedEvent event = mock(BlockCommittedEvent.class);
      doThrow(RuntimeException.class).when(serviceWrapper)
          .afterCommit(event);

      // Notify of block commit event — the service runtime must swallow the exception
      serviceRuntime.afterCommit(event);

      verify(serviceWrapper).afterCommit(event);
    }
  }

  @Nested
  class WithMultipleServices {
    final ServiceArtifactId ARTIFACT_ID = ServiceArtifactId.parseFrom("com.acme:foo-service:1.0.0");
    final Map<ServiceInstanceSpec, ServiceWrapper> SERVICES = ImmutableMap.of(
        ServiceInstanceSpec.newInstance("a", 1, ARTIFACT_ID), mock(ServiceWrapper.class, "a"),
        ServiceInstanceSpec.newInstance("b", 2, ARTIFACT_ID), mock(ServiceWrapper.class, "b"),
        ServiceInstanceSpec.newInstance("c", 3, ARTIFACT_ID), mock(ServiceWrapper.class, "c")
    );

    @BeforeEach
    @SuppressWarnings("UnstableApiUsage")
    void checkTestData() {
      // Check the test data correctness: check that test instances are ordered by name,
      // as some tests rely on that
      assertTrue(isInStrictOrder(SERVICES.keySet(), comparing(ServiceInstanceSpec::getName)),
          "Services must be ordered by name — the same order as used in "
              + "the ServiceRuntime for iteration on services, as this test relies on that");
    }

    @BeforeEach
    void addServices() {
      LoadedServiceDefinition serviceDefinition = LoadedServiceDefinition
          .newInstance(ARTIFACT_ID, TestServiceModule::new);
      when(serviceLoader.findService(ARTIFACT_ID))
          .thenReturn(Optional.of(serviceDefinition));
      // Setup the factory
      for (Entry<ServiceInstanceSpec, ServiceWrapper> entry : SERVICES.entrySet()) {
        ServiceInstanceSpec instanceSpec = entry.getKey();
        ServiceWrapper service = entry.getValue();
        when(service.getId()).thenReturn(instanceSpec.getId());
        when(service.getName()).thenReturn(instanceSpec.getName());
        when(servicesFactory.createService(serviceDefinition, instanceSpec))
            .thenReturn(service);
      }

      // Create the services
      for (ServiceInstanceSpec instanceSpec : SERVICES.keySet()) {
        serviceRuntime.createService(instanceSpec);
      }
    }

    @Test
    void getStateHashesMultipleServices() throws CloseFailuresException {
      try (Database database = TemporaryDb.newInstance();
          Cleaner cleaner = new Cleaner()) {
        Snapshot s = database.createSnapshot(cleaner);
        // Setup the services
        ServiceRuntimeStateHashes.Builder expectedBuilder = ServiceRuntimeStateHashes.newBuilder();
        for (Entry<ServiceInstanceSpec, ServiceWrapper> entry : SERVICES.entrySet()) {
          ServiceInstanceSpec instanceSpec = entry.getKey();
          byte[] serviceStateHash = bytes(instanceSpec.getId());
          List<HashCode> serviceStateHashes = singletonList(HashCode.fromBytes(serviceStateHash));

          // Setup the service
          ServiceWrapper serviceWrapper = entry.getValue();
          when(serviceWrapper.getStateHashes(s)).thenReturn(serviceStateHashes);

          // Add to the expected state hashes
          expectedBuilder.addServiceStateHashes(ServiceStateHashes.newBuilder()
              .setInstanceId(serviceWrapper.getId())
              .addStateHashes(ByteString.copyFrom(serviceStateHash))
          );
        }

        // Request the state hashes
        ServiceRuntimeStateHashes runtimeStateHashes = serviceRuntime.getStateHashes(s);
        ServiceRuntimeStateHashes expectedStateHashes = expectedBuilder.build();
        assertThat(runtimeStateHashes).isEqualTo(expectedStateHashes);
      }
    }

    @Test
    void afterCommitMultipleServicesWithFirstThrowing() {
      Collection<ServiceWrapper> services = SERVICES.values();

      // Setup the first service to throw exception in its after commit handler
      ServiceWrapper service1 = services
          .iterator()
          .next();
      BlockCommittedEvent event = mock(BlockCommittedEvent.class);
      doThrow(RuntimeException.class).when(service1).afterCommit(event);

      // Notify the runtime of the block commit
      serviceRuntime.afterCommit(event);

      // Verify that each service got the notifications, i.e., the first service
      // throwing an exception has not disrupted the notification process
      InOrder inOrder = Mockito.inOrder(services.toArray(new Object[0]));
      for (ServiceWrapper service: services) {
        inOrder.verify(service).afterCommit(event);
      }
    }
  }
}
