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
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exonum.binding.common.messages.DeployArguments;
import com.exonum.binding.core.blockchain.BlockchainData;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.CloseFailuresException;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.messages.core.runtime.Base.ArtifactId;
import com.exonum.messages.core.runtime.Base.InstanceSpec;
import com.exonum.messages.core.runtime.Lifecycle.InstanceStatus;
import com.exonum.messages.core.runtime.Lifecycle.InstanceStatus.Simple;
import java.util.OptionalInt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/*
It is a unit test marked as IT because it loads classes with native methods (which, in turn,
load the native library in static initializers).
 */
@ExtendWith(MockitoExtension.class)
class ServiceRuntimeAdapterIntegrationTest {

  private static final long SNAPSHOT_HANDLE = 0x0A;
  private static final long HEIGHT = 1;
  private static final int VALIDATOR_ID = 1;
  private static final ArtifactId ARTIFACT_ID =
      ArtifactId.newBuilder()
          .setRuntimeId(1)
          .setName("com.acme/foo")
          .setVersion("1.2.3")
          .build();

  @Mock
  private ServiceRuntime serviceRuntime;
  @Mock
  private AccessFactory accessFactory;
  private ServiceRuntimeAdapter serviceRuntimeAdapter;
  @Mock
  private Snapshot snapshot;

  @BeforeEach
  void setUp() {
    serviceRuntimeAdapter = new ServiceRuntimeAdapter(serviceRuntime, accessFactory);
  }

  @Test
  void deployArtifact() throws ServiceLoadingException {
    String artifactFilename = "foo-1.2.3.jar";
    DeployArguments deployArguments = DeployArguments.newBuilder()
        .setArtifactFilename(artifactFilename)
        .build();
    byte[] deploySpec = deployArguments.toByteArray();

    ArtifactId artifact = ARTIFACT_ID;
    byte[] artifactBytes = artifact.toByteArray();

    serviceRuntimeAdapter.deployArtifact(artifactBytes, deploySpec);

    ServiceArtifactId expectedId = ServiceArtifactId.fromProto(artifact);
    verify(serviceRuntime).deployArtifact(expectedId, artifactFilename);
  }

  @Test
  void isArtifactDeployed() {
    ArtifactId artifact = ARTIFACT_ID;
    byte[] artifactBytes = artifact.toByteArray();

    when(serviceRuntime.isArtifactDeployed(ServiceArtifactId.fromProto(artifact)))
        .thenReturn(true);

    assertTrue(serviceRuntimeAdapter.isArtifactDeployed(artifactBytes));
  }

  @Test
  void deployArtifactWrongSpec() {
    ArtifactId artifact = ARTIFACT_ID;
    byte[] artifactBytes = artifact.toByteArray();
    byte[] deploySpec = bytes("Some rubbish");

    Exception e = assertThrows(IllegalArgumentException.class,
        () -> serviceRuntimeAdapter.deployArtifact(artifactBytes, deploySpec));

    assertThat(e).hasMessageContaining(artifact.getName());
    assertThat(e).hasMessageContaining(artifact.getVersion());
  }

  @Test
  void addService() throws CloseFailuresException {
    int serviceId = 1;
    long bdHandle = 0x110b;
    BlockchainData blockchainData = mock(BlockchainData.class);
    when(accessFactory.createBlockchainData(eq(bdHandle), any(Cleaner.class)))
        .thenReturn(blockchainData);

    String serviceName = "s1";
    ArtifactId artifact = ARTIFACT_ID;
    byte[] instanceSpec = InstanceSpec.newBuilder()
        .setId(serviceId)
        .setName(serviceName)
        .setArtifact(artifact)
        .build()
        .toByteArray();
    byte[] configuration = bytes(1, 2);

    // Initialize the service
    serviceRuntimeAdapter.initiateAddingService(bdHandle, instanceSpec, configuration);

    // Check the runtime was invoked with correct config
    ServiceInstanceSpec expected = ServiceInstanceSpec.newInstance(serviceName, serviceId,
        ServiceArtifactId.fromProto(artifact));
    verify(serviceRuntime).initiateAddingService(blockchainData, expected, configuration);
  }

  @Test
  void initializeResumingService() throws CloseFailuresException {
    long bdNativeHandle = 0x110b;
    BlockchainData blockchainData = mock(BlockchainData.class);
    when(accessFactory.createBlockchainData(eq(bdNativeHandle), any(Cleaner.class)))
        .thenReturn(blockchainData);

    int serviceId = 1;
    String serviceName = "s1";
    ArtifactId artifact = ARTIFACT_ID;
    byte[] instanceSpec = InstanceSpec.newBuilder()
        .setId(serviceId)
        .setName(serviceName)
        .setArtifact(artifact)
        .build()
        .toByteArray();
    byte[] arguments = bytes(1, 2);

    serviceRuntimeAdapter.initiateResumingService(bdNativeHandle, instanceSpec, arguments);

    // Check the runtime was invoked with correct config
    ServiceInstanceSpec expected = ServiceInstanceSpec.newInstance(serviceName, serviceId,
        ServiceArtifactId.fromProto(artifact));
    verify(serviceRuntime).initiateResumingService(blockchainData, expected, arguments);
  }

  @Test
  void beforeTransactions() throws CloseFailuresException {
    int serviceId = 1;
    long bdNativeHandle = 0x110b;
    BlockchainData blockchainData = mock(BlockchainData.class);
    when(accessFactory.createBlockchainData(eq(bdNativeHandle), any(Cleaner.class)))
        .thenReturn(blockchainData);

    serviceRuntimeAdapter.beforeTransactions(serviceId, bdNativeHandle);

    verify(serviceRuntime).beforeTransactions(serviceId, blockchainData);
  }

  @Test
  void afterTransactions() throws CloseFailuresException {
    int serviceId = 1;
    long bdNativeHandle = 0x110b;
    BlockchainData blockchainData = mock(BlockchainData.class);
    when(accessFactory.createBlockchainData(eq(bdNativeHandle), any(Cleaner.class)))
        .thenReturn(blockchainData);

    serviceRuntimeAdapter.afterTransactions(serviceId, bdNativeHandle);

    verify(serviceRuntime).afterTransactions(serviceId, blockchainData);
  }

  @Test
  void afterCommit_ValidatorNode() throws CloseFailuresException {
    when(accessFactory.createSnapshot(eq(SNAPSHOT_HANDLE), any(Cleaner.class)))
        .thenReturn(snapshot);
    serviceRuntimeAdapter.afterCommit(SNAPSHOT_HANDLE, VALIDATOR_ID, HEIGHT);

    verify(serviceRuntime).afterCommit(snapshot, OptionalInt.of(VALIDATOR_ID), HEIGHT);
  }

  @Test
  void afterCommit_AuditorNode() throws CloseFailuresException {
    // For auditor nodes (which do not have validatorId) negative validatorId is passed
    int validatorId = -1;
    when(accessFactory.createSnapshot(eq(SNAPSHOT_HANDLE), any(Cleaner.class)))
        .thenReturn(snapshot);
    serviceRuntimeAdapter.afterCommit(SNAPSHOT_HANDLE, validatorId, HEIGHT);

    verify(serviceRuntime).afterCommit(snapshot, OptionalInt.empty(), HEIGHT);
  }

  @Test
  void updateServiceStatus() {
    int serviceId = 1;
    String serviceName = "s1";
    ArtifactId artifact = ARTIFACT_ID;
    InstanceSpec instanceSpec = InstanceSpec.newBuilder()
        .setId(serviceId)
        .setName(serviceName)
        .setArtifact(artifact)
        .build();
    InstanceStatus status = InstanceStatus.newBuilder()
        .setSimple(Simple.ACTIVE)
        .build();

    serviceRuntimeAdapter
        .updateServiceStatus(instanceSpec.toByteArray(), status.toByteArray());

    ServiceInstanceSpec expectedSpec = ServiceInstanceSpec
        .newInstance(serviceName, serviceId, ServiceArtifactId.fromProto(artifact));
    verify(serviceRuntime).updateInstanceStatus(expectedSpec, status);
  }
}
