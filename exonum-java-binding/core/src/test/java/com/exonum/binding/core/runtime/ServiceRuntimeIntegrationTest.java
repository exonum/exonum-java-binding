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

import static com.exonum.binding.core.runtime.ServiceWrapper.DEFAULT_INTERFACE_NAME;
import static com.exonum.binding.test.Bytes.bytes;
import static com.google.common.collect.Comparators.isInStrictOrder;
import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.runtime.ServiceArtifactId;
import com.exonum.binding.core.blockchain.BlockchainData;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.CloseFailuresException;
import com.exonum.binding.core.service.BlockCommittedEvent;
import com.exonum.binding.core.service.Configuration;
import com.exonum.binding.core.service.ExecutionContext;
import com.exonum.binding.core.storage.database.Database;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.storage.database.TemporaryDb;
import com.exonum.messages.core.runtime.Lifecycle.InstanceMigration;
import com.exonum.messages.core.runtime.Lifecycle.InstanceStatus;
import com.exonum.messages.core.runtime.Lifecycle.InstanceStatus.Simple;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceRuntimeIntegrationTest {

  // [ECR-587] Replace with a temp directory obtained from a TempDir JUnit extension so that
  //   the check of its existence passes.
  static final Path ARTIFACTS_DIR = Paths.get("/tmp/");
  static final String TEST_NAME = "test_service_name";
  static final int TEST_ID = 17;
  static final HashCode TEST_HASH = HashCode.fromBytes(bytes(1, 2, 3));
  static final PublicKey TEST_PUBLIC_KEY = PublicKey.fromBytes(bytes(4, 5, 6));
  static final InstanceStatus ACTIVE_STATUS = InstanceStatus.newBuilder()
      .setSimple(Simple.ACTIVE).build();

  @Mock
  private ServiceLoader serviceLoader;
  @Mock
  private ServicesFactory servicesFactory;
  @Mock
  private RuntimeTransport transport;
  @Mock
  private BlockchainDataFactory blockchainDataFactory;

  private ServiceRuntime serviceRuntime;

  @BeforeEach
  void setUp() {
    serviceRuntime = new ServiceRuntime(serviceLoader, servicesFactory, transport,
        blockchainDataFactory, ARTIFACTS_DIR);
  }

  @Test
  void startsServerOnInitialization() {
    serviceRuntime.initialize(mock(NodeProxy.class));

    verify(transport).start();
  }

  @Test
  void deployCorrectArtifact() throws Exception {
    serviceRuntime.initialize(mock(NodeProxy.class));

    ServiceArtifactId serviceId = ServiceArtifactId.newJavaId("com.acme/foo-service", "1.0.0");
    String artifactFilename = "foo-service.jar";
    Path serviceArtifactLocation = ARTIFACTS_DIR.resolve(artifactFilename);
    LoadedServiceDefinition serviceDefinition = LoadedServiceDefinition
        .newInstance(serviceId, TestServiceModule::new);
    when(serviceLoader.loadService(serviceArtifactLocation))
        .thenReturn(serviceDefinition);
    when(serviceLoader.findService(serviceId))
        .thenReturn(Optional.of(serviceDefinition));

    serviceRuntime.deployArtifact(serviceId, artifactFilename);

    assertTrue(serviceRuntime.isArtifactDeployed(serviceId));
    verify(serviceLoader).loadService(serviceArtifactLocation);
  }

  @Test
  void deployArtifactWrongId() throws Exception {
    serviceRuntime.initialize(mock(NodeProxy.class));

    ServiceArtifactId actualId = ServiceArtifactId.newJavaId("com.acme/actual", "1.0.0");
    String artifactFilename = "foo-service.jar";
    Path serviceArtifactLocation = ARTIFACTS_DIR.resolve(artifactFilename);
    LoadedServiceDefinition serviceDefinition = LoadedServiceDefinition
        .newInstance(actualId, TestServiceModule::new);
    when(serviceLoader.loadService(serviceArtifactLocation))
        .thenReturn(serviceDefinition);

    ServiceArtifactId expectedId = ServiceArtifactId.newJavaId("com.acme/expected", "1.0.0");

    Exception actual = assertThrows(ServiceLoadingException.class,
        () -> serviceRuntime.deployArtifact(expectedId, artifactFilename));
    assertThat(actual).hasMessageContainingAll(actualId.toString(), expectedId.toString(),
        artifactFilename);

    // Check the service artifact is unloaded
    verify(serviceLoader).unloadService(actualId);
  }

  @Test
  void deployArtifactFailed() throws Exception {
    serviceRuntime.initialize(mock(NodeProxy.class));

    ServiceArtifactId serviceId = ServiceArtifactId.newJavaId("com.acme/actual", "1.0.0");
    String artifactFilename = "foo-service.jar";
    Path serviceArtifactLocation = ARTIFACTS_DIR.resolve(artifactFilename);
    ServiceLoadingException exception = new ServiceLoadingException("Boom");
    when(serviceLoader.loadService(serviceArtifactLocation))
        .thenThrow(exception);
    when(serviceLoader.findService(serviceId))
        .thenReturn(Optional.empty());

    Exception actual = assertThrows(ServiceLoadingException.class,
        () -> serviceRuntime.deployArtifact(serviceId, artifactFilename));
    assertThat(actual).isSameAs(exception);
    assertFalse(serviceRuntime.isArtifactDeployed(serviceId));
  }

  @Test
  void startAddingService() {
    serviceRuntime.initialize(mock(NodeProxy.class));

    ServiceArtifactId artifactId = ServiceArtifactId.parseFrom("1:com.acme/foo-service:1.0.0");
    LoadedServiceDefinition serviceDefinition = LoadedServiceDefinition
        .newInstance(artifactId, TestServiceModule::new);
    ServiceInstanceSpec instanceSpec = ServiceInstanceSpec.newInstance(TEST_NAME,
        TEST_ID, artifactId);
    when(serviceLoader.findService(artifactId))
        .thenReturn(Optional.of(serviceDefinition));

    ServiceWrapper serviceWrapper = mock(ServiceWrapper.class);
    when(serviceWrapper.getName()).thenReturn(TEST_NAME);
    when(serviceWrapper.getId()).thenReturn(TEST_ID);
    when(servicesFactory.createService(eq(serviceDefinition), eq(instanceSpec),
        any(ServiceNodeProxy.class)))
        .thenReturn(serviceWrapper);

    // Create the service from the artifact
    BlockchainData blockchainData = mock(BlockchainData.class);
    byte[] configuration = anyConfiguration();
    serviceRuntime.initiateAddingService(blockchainData, instanceSpec, configuration);

    // Check it was instantiated as expected
    verify(servicesFactory).createService(eq(serviceDefinition), eq(instanceSpec),
        any(ServiceNodeProxy.class));

    // and the service was configured
    ExecutionContext expectedContext = zeroContext(TEST_ID, TEST_NAME, blockchainData);
    Configuration expectedConfig = new ServiceConfiguration(configuration);
    verify(serviceWrapper).initialize(expectedContext, expectedConfig);

    // but not committed
    assertThat(serviceRuntime.findService(TEST_NAME)).isEmpty();
  }

  @Test
  void startAddingServiceUnknownServiceArtifact() {
    serviceRuntime.initialize(mock(NodeProxy.class));

    ServiceArtifactId artifactId = ServiceArtifactId.newJavaId("com.acme/foo-service", "1.0.0");
    when(serviceLoader.findService(artifactId)).thenReturn(Optional.empty());

    ServiceInstanceSpec instanceSpec = ServiceInstanceSpec.newInstance(TEST_NAME,
        TEST_ID, artifactId);

    BlockchainData blockchainData = mock(BlockchainData.class);
    byte[] configuration = anyConfiguration();
    Exception e = assertThrows(IllegalArgumentException.class,
        () -> serviceRuntime.initiateAddingService(blockchainData, instanceSpec, configuration));

    assertThat(e).hasMessageFindingMatch("Unknown.+artifact");
    assertThat(e).hasMessageContaining(String.valueOf(artifactId));

    assertThat(serviceRuntime.findService(TEST_NAME)).isEmpty();
  }

  @Test
  void startAddingServiceBadInitialConfiguration() {
    serviceRuntime.initialize(mock(NodeProxy.class));

    ServiceArtifactId artifactId = ServiceArtifactId.newJavaId("com.acme/foo-service", "1.0.0");
    LoadedServiceDefinition serviceDefinition = LoadedServiceDefinition
        .newInstance(artifactId, TestServiceModule::new);
    ServiceInstanceSpec instanceSpec = ServiceInstanceSpec.newInstance(TEST_NAME,
        TEST_ID, artifactId);
    when(serviceLoader.findService(artifactId))
        .thenReturn(Optional.of(serviceDefinition));

    ServiceWrapper serviceWrapper = mock(ServiceWrapper.class);
    when(serviceWrapper.getName()).thenReturn(TEST_NAME);
    when(serviceWrapper.getId()).thenReturn(TEST_ID);
    when(servicesFactory.createService(eq(serviceDefinition), eq(instanceSpec),
        any(ServiceNodeProxy.class)))
        .thenReturn(serviceWrapper);

    BlockchainData blockchainData = mock(BlockchainData.class);
    ExecutionContext expectedContext = zeroContext(TEST_ID, TEST_NAME, blockchainData);
    byte[] configuration = anyConfiguration();
    ServiceConfiguration expectedConfig = new ServiceConfiguration(configuration);
    doThrow(IllegalArgumentException.class).when(serviceWrapper)
        .initialize(expectedContext, expectedConfig);

    // Try to create and initialize the service
    assertThrows(IllegalArgumentException.class,
        () -> serviceRuntime.initiateAddingService(blockchainData, instanceSpec, configuration));

    assertThat(serviceRuntime.findService(TEST_NAME)).isEmpty();
  }

  @Test
  void activateService() {
    serviceRuntime.initialize(mock(NodeProxy.class));

    ServiceArtifactId artifactId = ServiceArtifactId.newJavaId("com.acme/foo-service", "1.0.0");
    LoadedServiceDefinition serviceDefinition = LoadedServiceDefinition
        .newInstance(artifactId, TestServiceModule::new);
    ServiceInstanceSpec instanceSpec = ServiceInstanceSpec.newInstance(TEST_NAME,
        TEST_ID, artifactId);
    when(serviceLoader.findService(artifactId))
        .thenReturn(Optional.of(serviceDefinition));

    ServiceWrapper serviceWrapper = mock(ServiceWrapper.class);
    when(serviceWrapper.getId()).thenReturn(TEST_ID);
    when(serviceWrapper.getName()).thenReturn(TEST_NAME);
    when(servicesFactory.createService(eq(serviceDefinition), eq(instanceSpec),
        any(ServiceNodeProxy.class)))
        .thenReturn(serviceWrapper);

    // Activate the service from the artifact
    serviceRuntime.updateInstanceStatus(instanceSpec, ACTIVE_STATUS);

    // Check it was instantiated as expected
    verify(servicesFactory).createService(eq(serviceDefinition), eq(instanceSpec),
        any(ServiceNodeProxy.class));

    // and its API is connected
    verify(transport).connectServiceApi(serviceWrapper);

    // and is present in the runtime
    Optional<ServiceWrapper> serviceOpt = serviceRuntime.findService(TEST_NAME);
    assertThat(serviceOpt).hasValue(serviceWrapper);
  }

  @Test
  void activateServiceDuplicate() {
    serviceRuntime.initialize(mock(NodeProxy.class));

    ServiceArtifactId artifactId = ServiceArtifactId.newJavaId("com.acme/foo-service", "1.0.0");
    LoadedServiceDefinition serviceDefinition = LoadedServiceDefinition
        .newInstance(artifactId, TestServiceModule::new);
    ServiceInstanceSpec instanceSpec = ServiceInstanceSpec.newInstance(TEST_NAME,
        TEST_ID, artifactId);
    when(serviceLoader.findService(artifactId))
        .thenReturn(Optional.of(serviceDefinition));

    ServiceWrapper serviceWrapper = mock(ServiceWrapper.class);
    when(serviceWrapper.getId()).thenReturn(TEST_ID);
    when(serviceWrapper.getName()).thenReturn(TEST_NAME);
    when(servicesFactory.createService(eq(serviceDefinition), eq(instanceSpec),
        any(ServiceNodeProxy.class)))
        .thenReturn(serviceWrapper);

    // Activate the service
    serviceRuntime.updateInstanceStatus(instanceSpec, ACTIVE_STATUS);

    // Try to activate another service with the same service instance specification
    Exception e = assertThrows(IllegalArgumentException.class,
        () -> serviceRuntime.updateInstanceStatus(instanceSpec, ACTIVE_STATUS));

    assertThat(e).hasMessageContaining("name");
    assertThat(e).hasMessageContaining(TEST_NAME);

    // Check the service was instantiated only once
    verify(servicesFactory).createService(eq(serviceDefinition), eq(instanceSpec),
        any(ServiceNodeProxy.class));
  }

  @Test
  void stopNonActiveService() {
    ServiceArtifactId artifactId = ServiceArtifactId.newJavaId("com.acme/foo-service", "1.0.0");
    ServiceInstanceSpec instanceSpec = ServiceInstanceSpec.newInstance(TEST_NAME,
        TEST_ID, artifactId);

    InstanceStatus stoppedStatus = InstanceStatus.newBuilder()
        .setSimple(Simple.STOPPED).build();
    serviceRuntime.updateInstanceStatus(instanceSpec, stoppedStatus);

    verify(transport, never()).disconnectServiceApi(any(ServiceWrapper.class));
  }

  @Test
  void stopService() {
    serviceRuntime.initialize(mock(NodeProxy.class));

    ServiceArtifactId artifactId = ServiceArtifactId.newJavaId("com.acme/foo-service", "1.0.0");
    LoadedServiceDefinition serviceDefinition = LoadedServiceDefinition
        .newInstance(artifactId, TestServiceModule::new);
    ServiceInstanceSpec instanceSpec = ServiceInstanceSpec.newInstance(TEST_NAME,
        TEST_ID, artifactId);
    when(serviceLoader.findService(artifactId))
        .thenReturn(Optional.of(serviceDefinition));

    ServiceWrapper serviceWrapper = mock(ServiceWrapper.class);
    when(serviceWrapper.getId()).thenReturn(TEST_ID);
    when(serviceWrapper.getName()).thenReturn(TEST_NAME);

    when(servicesFactory.createService(eq(serviceDefinition), eq(instanceSpec),
        any(ServiceNodeProxy.class)))
        .thenReturn(serviceWrapper);

    // Activate the service
    serviceRuntime.updateInstanceStatus(instanceSpec, ACTIVE_STATUS);

    // Stop the service
    InstanceStatus stoppedStatus = InstanceStatus.newBuilder()
        .setSimple(Simple.STOPPED).build();
    serviceRuntime.updateInstanceStatus(instanceSpec, stoppedStatus);

    // Verify service stopped
    verify(transport).disconnectServiceApi(any(ServiceWrapper.class));
    Optional<ServiceWrapper> serviceOpt = serviceRuntime.findService(TEST_NAME);
    assertThat(serviceOpt).isEmpty();
    verify(serviceWrapper).requestToStop();
  }

  @ParameterizedTest
  @MethodSource("badStatuses")
  void updateServiceStatusBadStatus(InstanceStatus badStatus) {
    ServiceArtifactId artifactId = ServiceArtifactId.newJavaId("com.acme/foo-service", "1.0.0");
    ServiceInstanceSpec instanceSpec = ServiceInstanceSpec.newInstance(TEST_NAME,
        TEST_ID, artifactId);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> serviceRuntime.updateInstanceStatus(instanceSpec, badStatus));
    assertThat(exception).hasMessageContaining(badStatus.getSimple().name());
    assertThat(exception).hasMessageContaining(instanceSpec.getName());
  }

  private static List<InstanceStatus> badStatuses() {
    return ImmutableList.of(
        InstanceStatus.newBuilder()
            .setSimple(Simple.NONE)
            .build(),
        //TODO: add migration support
        InstanceStatus.newBuilder()
            .setMigration(InstanceMigration.newBuilder().build())
            .build()
    );
  }

  @Test
  void initializeResumingService() {
    serviceRuntime.initialize(mock(NodeProxy.class));

    ServiceArtifactId artifactId = ServiceArtifactId.parseFrom("1:com.acme/foo-service:1.0.0");
    LoadedServiceDefinition serviceDefinition = LoadedServiceDefinition
        .newInstance(artifactId, TestServiceModule::new);
    ServiceInstanceSpec instanceSpec = ServiceInstanceSpec.newInstance(TEST_NAME,
        TEST_ID, artifactId);
    when(serviceLoader.findService(artifactId))
        .thenReturn(Optional.of(serviceDefinition));

    ServiceWrapper serviceWrapper = mock(ServiceWrapper.class);
    when(serviceWrapper.getName()).thenReturn(TEST_NAME);
    when(serviceWrapper.getId()).thenReturn(TEST_ID);
    when(servicesFactory.createService(eq(serviceDefinition), eq(instanceSpec),
        any(ServiceNodeProxy.class)))
        .thenReturn(serviceWrapper);

    // Create the service from the artifact
    BlockchainData blockchainData = mock(BlockchainData.class);
    byte[] arguments = anyConfiguration();
    serviceRuntime.initiateResumingService(blockchainData, instanceSpec, arguments);

    // Check it was instantiated as expected
    verify(servicesFactory).createService(eq(serviceDefinition), eq(instanceSpec),
        any(ServiceNodeProxy.class));

    // and the service was resumed
    ExecutionContext expectedContext = zeroContext(TEST_ID, TEST_NAME, blockchainData);
    verify(serviceWrapper).resume(expectedContext, arguments);

    // but not registered in the runtime yet:
    assertThat(serviceRuntime.findService(TEST_NAME)).isEmpty();
  }

  @Test
  void initializeResumingActiveService() {
    serviceRuntime.initialize(mock(NodeProxy.class));

    ServiceArtifactId artifactId = ServiceArtifactId.newJavaId("com.acme/foo-service", "1.0.0");
    LoadedServiceDefinition serviceDefinition = LoadedServiceDefinition
        .newInstance(artifactId, TestServiceModule::new);
    ServiceInstanceSpec instanceSpec = ServiceInstanceSpec.newInstance(TEST_NAME,
        TEST_ID, artifactId);
    when(serviceLoader.findService(artifactId))
        .thenReturn(Optional.of(serviceDefinition));

    ServiceWrapper serviceWrapper = mock(ServiceWrapper.class);
    when(serviceWrapper.getId()).thenReturn(TEST_ID);
    when(serviceWrapper.getName()).thenReturn(TEST_NAME);
    when(servicesFactory.createService(eq(serviceDefinition), eq(instanceSpec),
        any(ServiceNodeProxy.class)))
        .thenReturn(serviceWrapper);

    // Activate the service from the artifact
    serviceRuntime.updateInstanceStatus(instanceSpec, ACTIVE_STATUS);

    byte[] arguments = anyConfiguration();
    BlockchainData blockchainData = mock(BlockchainData.class);
    assertThrows(IllegalArgumentException.class,
        () -> serviceRuntime.initiateResumingService(blockchainData, instanceSpec, arguments));
  }

  @Test
  void shutdown() throws InterruptedException {
    serviceRuntime.shutdown();

    InOrder inOrder = Mockito.inOrder(transport, serviceLoader);
    inOrder.verify(transport).close();
    inOrder.verify(serviceLoader).unloadAll();
  }

  @Test
  void shutdownInitialized() throws InterruptedException {
    NodeProxy node = mock(NodeProxy.class);
    serviceRuntime.initialize(node);

    serviceRuntime.shutdown();

    InOrder inOrder = Mockito.inOrder(transport, serviceLoader, node);
    inOrder.verify(transport).close();
    inOrder.verify(serviceLoader).unloadAll();
    inOrder.verify(node).close();
  }

  @Test
  void shutdownIfStopFailureShallUnloadArtifacts() throws InterruptedException {
    IllegalStateException stopFailure = new IllegalStateException("Server#stop failure");
    doThrow(stopFailure).when(transport).close();

    serviceRuntime.shutdown();

    verify(transport).close();
    // Verify that serviceLoader was invoked despite the stop failure
    verify(serviceLoader).unloadAll();
  }

  @Nested
  class WithSingleService {
    final ServiceArtifactId ARTIFACT_ID = ServiceArtifactId
        .newJavaId("com.acme/foo-service", "1.0.0");
    final ServiceInstanceSpec INSTANCE_SPEC = ServiceInstanceSpec.newInstance(TEST_NAME,
        TEST_ID, ARTIFACT_ID);

    @Mock
    ServiceWrapper serviceWrapper;

    @BeforeEach
    void addService() {
      // Initialize the runtime
      serviceRuntime.initialize(mock(NodeProxy.class));

      // Setup the service
      when(serviceWrapper.getId()).thenReturn(TEST_ID);
      when(serviceWrapper.getName()).thenReturn(TEST_NAME);
      // Setup the loader
      LoadedServiceDefinition serviceDefinition = LoadedServiceDefinition
          .newInstance(ARTIFACT_ID, TestServiceModule::new);
      when(serviceLoader.findService(ARTIFACT_ID))
          .thenReturn(Optional.of(serviceDefinition));
      // Setup the factory
      when(servicesFactory.createService(eq(serviceDefinition), eq(INSTANCE_SPEC),
          any(ServiceNodeProxy.class)))
          .thenReturn(serviceWrapper);

      // Create the service from the artifact
      serviceRuntime.updateInstanceStatus(INSTANCE_SPEC, ACTIVE_STATUS);
    }

    @Test
    void executeTransaction() throws Exception {
      try (Database database = TemporaryDb.newInstance();
          Cleaner cleaner = new Cleaner()) {
        String interfaceName = DEFAULT_INTERFACE_NAME;
        int txId = 1;
        byte[] arguments = bytes(127);
        Fork fork = database.createFork(cleaner);
        BlockchainData blockchainData = BlockchainData.fromRawAccess(fork, TEST_NAME);
        int callerServiceId = 0;
        ExecutionContext expectedContext = ExecutionContext.builder()
            .blockchainData(blockchainData)
            .txMessageHash(TEST_HASH)
            .authorPk(TEST_PUBLIC_KEY)
            .serviceName(TEST_NAME)
            .serviceId(TEST_ID)
            .build();

        serviceRuntime.executeTransaction(TEST_ID, interfaceName, txId, arguments, blockchainData,
            callerServiceId, TEST_HASH, TEST_PUBLIC_KEY);

        verify(serviceWrapper).executeTransaction(interfaceName, txId, arguments,
            callerServiceId, expectedContext);
      }
    }

    @Test
    void executeTransactionUnknownService() throws Exception {
      try (Database database = TemporaryDb.newInstance();
          Cleaner cleaner = new Cleaner()) {
        int serviceId = TEST_ID + 1;
        int txId = 1;
        byte[] arguments = bytes(127);
        Fork fork = database.createFork(cleaner);
        BlockchainData blockchainData = BlockchainData.fromRawAccess(fork, TEST_NAME);

        Exception e = assertThrows(IllegalArgumentException.class,
            () -> serviceRuntime.executeTransaction(serviceId, DEFAULT_INTERFACE_NAME, txId,
                arguments, blockchainData, 0, TEST_HASH, TEST_PUBLIC_KEY));

        assertThat(e).hasMessageContaining(String.valueOf(serviceId));
      }
    }

    @Test
    void beforeTransactionsSingleService() throws CloseFailuresException {
      try (Database database = TemporaryDb.newInstance();
          Cleaner cleaner = new Cleaner()) {
        Fork fork = database.createFork(cleaner);
        BlockchainData blockchainData = BlockchainData.fromRawAccess(fork, TEST_NAME);

        serviceRuntime.beforeTransactions(TEST_ID, blockchainData);

        ExecutionContext expectedContext = zeroContext(TEST_ID, TEST_NAME, blockchainData);
        verify(serviceWrapper).beforeTransactions(expectedContext);
      }
    }

    @Test
    void afterTransactionsSingleService() throws CloseFailuresException {
      try (Database database = TemporaryDb.newInstance();
          Cleaner cleaner = new Cleaner()) {
        Fork fork = database.createFork(cleaner);
        BlockchainData blockchainData = BlockchainData.fromRawAccess(fork, TEST_NAME);

        serviceRuntime.afterTransactions(TEST_ID, blockchainData);

        ExecutionContext expectedContext = zeroContext(TEST_ID, TEST_NAME, blockchainData);
        verify(serviceWrapper).afterTransactions(expectedContext);
      }
    }

    @Test
    void afterTransactionsThrowingServiceExceptionPropagated() throws CloseFailuresException {
      try (Database database = TemporaryDb.newInstance();
          Cleaner cleaner = new Cleaner()) {
        Fork fork = database.createFork(cleaner);
        BlockchainData blockchainData = BlockchainData.fromRawAccess(fork, TEST_NAME);
        RuntimeException serviceException = new RuntimeException("Service exception");
        ExecutionContext expectedContext = zeroContext(TEST_ID, TEST_NAME, blockchainData);
        doThrow(serviceException).when(serviceWrapper).afterTransactions(expectedContext);

        RuntimeException actual = assertThrows(serviceException.getClass(),
            () -> serviceRuntime.afterTransactions(TEST_ID, blockchainData));
        assertThat(actual).isSameAs(serviceException);

        verify(serviceWrapper).afterTransactions(expectedContext);
      }
    }

    @Test
    void afterCommitSingleService() {
      Snapshot snapshot = mock(Snapshot.class);
      OptionalInt validatorId = OptionalInt.of(1);
      long height = 2L;
      BlockchainData blockchainData = mock(BlockchainData.class);
      when(blockchainDataFactory.fromRawAccess(snapshot, TEST_NAME))
          .thenReturn(blockchainData);

      serviceRuntime.afterCommit(snapshot, validatorId, height);

      ArgumentCaptor<BlockCommittedEvent> ac = ArgumentCaptor.forClass(BlockCommittedEvent.class);
      verify(serviceWrapper).afterCommit(ac.capture());

      BlockCommittedEvent actual = ac.getValue();
      assertThat(actual.getValidatorId()).isEqualTo(validatorId);
      assertThat(actual.getHeight()).isEqualTo(height);
      assertThat(actual.getSnapshot()).isEqualTo(blockchainData);
    }

    @Test
    void afterCommitSingleServiceThrowingException() {
      Snapshot snapshot = mock(Snapshot.class);
      OptionalInt validatorId = OptionalInt.of(1);
      long height = 2L;
      BlockchainData blockchainData = mock(BlockchainData.class);
      when(blockchainDataFactory.fromRawAccess(snapshot, TEST_NAME))
          .thenReturn(blockchainData);
      doThrow(RuntimeException.class).when(serviceWrapper)
          .afterCommit(any(BlockCommittedEvent.class));

      // Notify of block commit event — the service runtime must swallow the exception
      serviceRuntime.afterCommit(snapshot, validatorId, height);

      verify(serviceWrapper).afterCommit(any(BlockCommittedEvent.class));
    }
  }

  private static byte[] anyConfiguration() {
    return bytes(1, 2, 3, 4);
  }

  @Nested
  class WithMultipleServices {
    final ServiceArtifactId ARTIFACT_ID = ServiceArtifactId
        .newJavaId("com.acme/foo-service", "1.0.0");
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
      // Initialize the runtime
      serviceRuntime.initialize(mock(NodeProxy.class));

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
        when(servicesFactory.createService(eq(serviceDefinition), eq(instanceSpec),
            any(ServiceNodeProxy.class)))
            .thenReturn(service);
      }

      // Create the services
      for (ServiceInstanceSpec instanceSpec : SERVICES.keySet()) {
        serviceRuntime.updateInstanceStatus(instanceSpec, ACTIVE_STATUS);
      }
    }

    @Test
    void afterCommitMultipleServicesWithFirstThrowing() {
      Collection<ServiceWrapper> services = SERVICES.values();

      // Setup the first service to throw exception in its after commit handler
      ServiceWrapper service1 = services
          .iterator()
          .next();
      doThrow(RuntimeException.class).when(service1).afterCommit(any(BlockCommittedEvent.class));

      Snapshot snapshot = mock(Snapshot.class);
      BlockchainData blockchainData = mock(BlockchainData.class);
      when(blockchainDataFactory.fromRawAccess(eq(snapshot), anyString()))
          .thenReturn(blockchainData);
      OptionalInt validatorId = OptionalInt.of(1);
      long height = 2L;

      // Notify the runtime of the block commit
      serviceRuntime.afterCommit(snapshot, validatorId, height);

      // Verify that each service got the notifications, i.e., the first service
      // throwing an exception has not disrupted the notification process
      InOrder afterCommitOrder = Mockito.inOrder(services.toArray(new Object[0]));
      for (ServiceWrapper service : services) {
        afterCommitOrder.verify(service).afterCommit(any(BlockCommittedEvent.class));
      }

      // Verify the blockchain data instantiation
      InOrder dataOrder = Mockito.inOrder(blockchainDataFactory);
      for (ServiceWrapper service : services) {
        dataOrder.verify(blockchainDataFactory).fromRawAccess(snapshot, service.getName());
      }
    }
  }

  private static ExecutionContext zeroContext(int expectedId, String expectedName,
      BlockchainData expectedData) {
    return ExecutionContext.builder()
        .serviceName(expectedName)
        .serviceId(expectedId)
        .blockchainData(expectedData)
        .build();
  }
}
