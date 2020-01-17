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

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.CloseFailuresException;
import com.exonum.binding.core.service.BlockCommittedEvent;
import com.exonum.binding.core.service.BlockCommittedEventImpl;
import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.service.NodeProxy;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.transaction.ExecutionException;
import com.exonum.core.messages.Runtime.ArtifactId;
import com.exonum.core.messages.Runtime.InstanceSpec;
import com.exonum.core.messages.Runtime.InstanceState;
import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.OptionalInt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The adapter of {@link ServiceRuntime} to the interface, convenient to the native code
 * accessing it through JNI (simpler, faster, more reliable).
 *
 * <p>For more detailed documentation on the operations, see the {@link ServiceRuntime}.
 */
public class ServiceRuntimeAdapter {

  private final ServiceRuntime serviceRuntime;
  private final ViewFactory viewFactory;
  private static final Logger logger = LogManager.getLogger(ServiceRuntimeAdapter.class);

  @Inject
  public ServiceRuntimeAdapter(ServiceRuntime serviceRuntime, ViewFactory viewFactory) {
    this.serviceRuntime = serviceRuntime;
    this.viewFactory = viewFactory;
  }

  /**
   * Returns the corresponding service runtime.
   */
  public ServiceRuntime getServiceRuntime() {
    return serviceRuntime;
  }

  /**
   * Initializes the runtime.
   *
   * @param nodeNativeHandle the native handle to the Node object
   * @see ServiceRuntime#initialize(Node)
   */
  void initialize(long nodeNativeHandle) {
    Node node = new NodeProxy(nodeNativeHandle);
    serviceRuntime.initialize(node);
  }

  /**
   * Deploys the Java service artifact.
   *
   * @param artifactId bytes representation of the Java service artifact id as a serialized message
   * @param deploySpec the deploy specification as a serialized
   *     {@link com.exonum.binding.core.runtime.DeployArguments}
   *     protobuf message
   * @throws IllegalArgumentException if the deploy specification or id are not valid
   * @throws ServiceLoadingException if the runtime failed to load the service or it is not correct
   * @see ServiceRuntime#deployArtifact(ServiceArtifactId, String)
   */
  void deployArtifact(byte[] artifactId, byte[] deploySpec) throws ServiceLoadingException {
    ArtifactId artifact = parseArtifact(artifactId);
    ServiceArtifactId javaArtifactId = ServiceArtifactId.fromProto(artifact);

    DeployArguments deployArguments = parseDeployArgs(javaArtifactId, deploySpec);
    String artifactFilename = deployArguments.getArtifactFilename();

    serviceRuntime.deployArtifact(javaArtifactId, artifactFilename);
  }

  /**
   * Returns true if the artifact with the given name is deployed in this runtime;
   * false — otherwise.
   * @param artifactId bytes representation of the service artifact
   */
  boolean isArtifactDeployed(byte[] artifactId) {
    ArtifactId artifact = parseArtifact(artifactId);
    ServiceArtifactId serviceArtifact = ServiceArtifactId.newJavaId(
        artifact.getName(), artifact.getVersion());
    return serviceRuntime.isArtifactDeployed(serviceArtifact);
  }

  private static DeployArguments parseDeployArgs(ServiceArtifactId artifact, byte[] deploySpec) {
    try {
      return DeployArguments.parseFrom(deploySpec);
    } catch (InvalidProtocolBufferException e) {
      String message = "Invalid deploy specification for artifact " + artifact;
      logger.error(message, e);
      throw new IllegalArgumentException(message, e);
    }
  }

  private static ArtifactId parseArtifact(byte[] artifactId) {
    try {
      return ArtifactId.parseFrom(artifactId);
    } catch (InvalidProtocolBufferException e) {
      String message = "Invalid artifact";
      logger.error(message, e);
      throw new IllegalArgumentException(message, e);
    }
  }

  /**
   * Starts registration of a new service instance with the given specification.
   *
   * @param forkHandle a handle to a native fork object
   * @param instanceSpec the service instance specification as a serialized {@link InstanceSpec}
   *     protobuf message
   * @param configuration the service initial configuration parameters as a serialized protobuf
   *     message
   * @throws CloseFailuresException if there was a failure in destroying some native peers
   * @throws ExecutionException if the service initialization failed
   * @throws UnexpectedExecutionException if the service initialization failed
   *     with an unexpected exception
   * @see ServiceRuntime#initiateAddingService(Fork, ServiceInstanceSpec, byte[])
   */
  void initiateAddingService(long forkHandle, byte[] instanceSpec, byte[] configuration)
      throws CloseFailuresException {
    try (Cleaner cleaner = new Cleaner()) {
      Fork fork = viewFactory.createFork(forkHandle, cleaner);
      ServiceInstanceSpec javaInstanceSpec = parseInstanceSpec(instanceSpec);

      serviceRuntime.initiateAddingService(fork, javaInstanceSpec, configuration);
    } catch (CloseFailuresException e) {
      handleCloseFailure(e);
    }
  }

  /**
   * Updates the status of the service instance.
   *
   * @param instanceSpec the service instance specification as a serialized {@link InstanceSpec}
   *     protobuf message
   * @param instanceState new state of the service instance as a serialized
   *     representation of the {@link InstanceState} protobuf message
   * @see ServiceRuntime#updateInstanceStatus(ServiceInstanceSpec, InstanceState.Status)
   */
  void updateServiceStatus(byte[] instanceSpec, byte[] instanceState) {
    ServiceInstanceSpec javaInstanceSpec = parseInstanceSpec(instanceSpec);
    InstanceState.Status instanceStatus = parseInstanceStatus(instanceState);
    serviceRuntime.updateInstanceStatus(javaInstanceSpec, instanceStatus);
  }

  private static ServiceInstanceSpec parseInstanceSpec(byte[] instanceSpec) {
    try {
      InstanceSpec spec = InstanceSpec.parseFrom(instanceSpec);
      ArtifactId artifact = spec.getArtifact();
      ServiceArtifactId artifactId = ServiceArtifactId.fromProto(artifact);
      return ServiceInstanceSpec.newInstance(spec.getName(), spec.getId(), artifactId);
    } catch (InvalidProtocolBufferException e) {
      logger.error(e);
      throw new IllegalArgumentException(e);
    }
  }

  private static InstanceState.Status parseInstanceStatus(byte[] instanceState) {
    try {
      InstanceState state = InstanceState.parseFrom(instanceState);
      return state.getStatus();
    } catch (InvalidProtocolBufferException e) {
      logger.error(e);
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Executes the service transaction.
   *
   * @param serviceId the service numeric identifier
   * @param interfaceName the name of the interface in which the transaction is defined
   * @param txId the transaction type identifier within the service
   * @param arguments the transaction arguments
   * @param forkNativeHandle a handle to a native fork object
   * @param callerServiceId the id of the service which invoked the transaction (in case of
   *      inner transactions); or 0 when the caller is an external message
   * @param txMessageHash the hash of the transaction message
   * @param authorPublicKey the public key of the transaction author
   * @throws ExecutionException if the transaction execution failed
   * @throws UnexpectedExecutionException if the transaction execution failed
   *     with an unexpected exception
   * @throws IllegalArgumentException if any argument is not valid
   * @see ServiceRuntime#executeTransaction(int, String, int, byte[], Fork, int, HashCode,
   *      PublicKey)
   */
  void executeTransaction(int serviceId, String interfaceName, int txId, byte[] arguments,
      long forkNativeHandle, int callerServiceId, byte[] txMessageHash, byte[] authorPublicKey)
      throws CloseFailuresException {

    try (Cleaner cleaner = new Cleaner("executeTransaction")) {
      Fork fork = viewFactory.createFork(forkNativeHandle, cleaner);
      HashCode hash = HashCode.fromBytes(txMessageHash);
      PublicKey authorPk = PublicKey.fromBytes(authorPublicKey);

      serviceRuntime.executeTransaction(serviceId, interfaceName, txId, arguments, fork,
          callerServiceId, hash, authorPk);
    } catch (CloseFailuresException e) {
      handleCloseFailure(e);
    }
  }

  /**
   * Performs the after transactions operation for services in this runtime.
   *
   * @param forkHandle a handle to the native fork object, which must support checkpoints
   *                   and rollbacks
   * @throws ExecutionException if the transaction execution failed
   * @throws UnexpectedExecutionException if the transaction execution failed
   *     with an unexpected exception
   * @throws CloseFailuresException if there was a failure in destroying some native peers
   * @see ServiceRuntime#afterTransactions(int, Fork)
   */
  void afterTransactions(int serviceId, long forkHandle) throws CloseFailuresException {
    try (Cleaner cleaner = new Cleaner("afterTransactions")) {
      Fork fork = viewFactory.createFork(forkHandle, cleaner);
      serviceRuntime.afterTransactions(serviceId, fork);
    } catch (CloseFailuresException e) {
      handleCloseFailure(e);
    }
  }

  /**
   * Notifies the runtime of the block commit event.
   *
   * @param snapshotHandle a handle to the native snapshot object
   * @param validatorId a validator id. Negative if this node is not a validator
   * @param height the current blockchain height
   * @throws CloseFailuresException if there was a failure in destroying some native peers
   * @see ServiceRuntime#afterCommit(BlockCommittedEvent)
   */
  void afterCommit(long snapshotHandle, int validatorId, long height)
      throws CloseFailuresException {
    try (Cleaner cleaner = new Cleaner("afterCommit")) {
      Snapshot snapshot = viewFactory.createSnapshot(snapshotHandle, cleaner);
      OptionalInt optionalValidatorId = validatorId >= 0
          ? OptionalInt.of(validatorId)
          : OptionalInt.empty();
      BlockCommittedEvent event =
          BlockCommittedEventImpl.valueOf(snapshot, optionalValidatorId, height);

      serviceRuntime.afterCommit(event);
    } catch (CloseFailuresException e) {
      handleCloseFailure(e);
    }
  }

  /**
   * Stops the Java service runtime.
   *
   * @see ServiceRuntime#shutdown()
   */
  void shutdown() throws InterruptedException {
    serviceRuntime.shutdown();
  }

  private static void handleCloseFailure(CloseFailuresException e) throws CloseFailuresException {
    logger.error("Failed to close some resources", e);
    throw e;
  }
}
