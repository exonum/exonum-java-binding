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

import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.CloseFailuresException;
import com.exonum.binding.core.service.BlockCommittedEvent;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.core.messages.Runtime.ArtifactId;
import com.exonum.core.messages.Runtime.InstanceSpec;
import com.exonum.core.messages.Runtime.InstanceStatus;
import com.exonum.core.messages.Runtime.InstanceStatus.Simple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceRuntimeAdapterTest {

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
    long forkHandle = 0x110b;
    Cleaner cleaner = new Cleaner();
    Fork fork = Fork.newInstance(forkHandle, false, cleaner);
    when(accessFactory.createFork(eq(forkHandle), any(Cleaner.class)))
        .thenReturn(fork);

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
    serviceRuntimeAdapter.initiateAddingService(forkHandle, instanceSpec, configuration);

    // Check the runtime was invoked with correct config
    ServiceInstanceSpec expected = ServiceInstanceSpec.newInstance(serviceName, serviceId,
        ServiceArtifactId.fromProto(artifact));
    verify(serviceRuntime).initiateAddingService(fork, expected, configuration);
  }

  @Test
  void initializeResumingService() throws CloseFailuresException {
    long forkHandle = 0x110b;
    Cleaner cleaner = new Cleaner();
    Fork fork = Fork.newInstance(forkHandle, false, cleaner);
    when(accessFactory.createFork(eq(forkHandle), any(Cleaner.class)))
        .thenReturn(fork);

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

    serviceRuntimeAdapter.initializeResumingService(forkHandle, instanceSpec, arguments);

    // Check the runtime was invoked with correct config
    ServiceInstanceSpec expected = ServiceInstanceSpec.newInstance(serviceName, serviceId,
        ServiceArtifactId.fromProto(artifact));
    verify(serviceRuntime).initializeResumingService(fork, expected, arguments);
  }

  @Test
  void afterTransactions() throws CloseFailuresException {
    int serviceId = 1;
    long forkHandle = 0x110b;
    Fork fork = mock(Fork.class);
    when(accessFactory.createFork(eq(forkHandle), any(Cleaner.class)))
        .thenReturn(fork);

    serviceRuntimeAdapter.afterTransactions(serviceId, forkHandle);

    verify(serviceRuntime).afterTransactions(serviceId, fork);
  }

  @Test
  void afterCommit_ValidatorNode() throws CloseFailuresException {
    when(accessFactory.createSnapshot(eq(SNAPSHOT_HANDLE), any(Cleaner.class)))
        .thenReturn(snapshot);
    serviceRuntimeAdapter.afterCommit(SNAPSHOT_HANDLE, VALIDATOR_ID, HEIGHT);

    ArgumentCaptor<BlockCommittedEvent> ac = ArgumentCaptor.forClass(BlockCommittedEvent.class);
    verify(serviceRuntime).afterCommit(ac.capture());

    BlockCommittedEvent event = ac.getValue();

    assertThat(event.getHeight()).isEqualTo(HEIGHT);
    assertThat(event.getValidatorId()).hasValue(VALIDATOR_ID);
    assertThat(event.getSnapshot()).isEqualTo(snapshot);
  }

  @Test
  void afterCommit_AuditorNode() throws CloseFailuresException {
    // For auditor nodes (which do not have validatorId) negative validatorId is passed
    int validatorId = -1;
    when(accessFactory.createSnapshot(eq(SNAPSHOT_HANDLE), any(Cleaner.class)))
        .thenReturn(snapshot);
    serviceRuntimeAdapter.afterCommit(SNAPSHOT_HANDLE, validatorId, HEIGHT);

    ArgumentCaptor<BlockCommittedEvent> ac = ArgumentCaptor.forClass(BlockCommittedEvent.class);
    verify(serviceRuntime).afterCommit(ac.capture());

    BlockCommittedEvent event = ac.getValue();

    assertThat(event.getHeight()).isEqualTo(HEIGHT);
    assertThat(event.getValidatorId()).isEmpty();
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
