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
import com.exonum.binding.common.messages.DeployArguments;
import com.exonum.binding.common.runtime.ServiceArtifactId;
import com.exonum.binding.core.blockchain.BlockchainData;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.CloseFailuresException;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.transaction.ExecutionException;
import com.exonum.messages.core.runtime.Base.ArtifactId;
import com.exonum.messages.core.runtime.Base.InstanceSpec;
import com.exonum.messages.core.runtime.Lifecycle.InstanceStatus;
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
  private final AccessFactory accessFactory;
  private static final Logger logger = LogManager.getLogger(ServiceRuntimeAdapter.class);

  @Inject
  public ServiceRuntimeAdapter(ServiceRuntime serviceRuntime, AccessFactory accessFactory) {
    this.serviceRuntime = serviceRuntime;
    this.accessFactory = accessFactory;
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
   * @see ServiceRuntime#initialize(NodeProxy)
   */
  void initialize(long nodeNativeHandle) {
    NodeProxy node = new NodeProxy(nodeNativeHandle);
    serviceRuntime.initialize(node);
  }

  /**
   * Deploys the Java service artifact.
   *
   * @param artifactId bytes representation of the Java service artifact id as a serialized message
   * @param deploySpec the deploy specification as a serialized
   *     {@link com.exonum.binding.common.messages.DeployArguments}
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
   * @param bdNativeHandle a handle to a native BlockchainData object
   * @param instanceSpec the service instance specification as a serialized {@link InstanceSpec}
   *     protobuf message
   * @param configuration the service initial configuration parameters as a serialized protobuf
   *     message
   * @throws CloseFailuresException if there was a failure in destroying some native peers
   * @throws ExecutionException if the service initialization failed
   * @throws UnexpectedExecutionException if the service initialization failed
   *     with an unexpected exception
   * @see ServiceRuntime#initiateAddingService(com.exonum.binding.core.blockchain.BlockchainData,
   *     ServiceInstanceSpec, byte[])
   */
  void initiateAddingService(long bdNativeHandle, byte[] instanceSpec, byte[] configuration)
      throws CloseFailuresException {
    try (Cleaner cleaner = new Cleaner()) {
      BlockchainData blockchainData = accessFactory.createBlockchainData(bdNativeHandle, cleaner);
      ServiceInstanceSpec javaInstanceSpec = parseInstanceSpec(instanceSpec);

      serviceRuntime.initiateAddingService(blockchainData, javaInstanceSpec, configuration);
    } catch (CloseFailuresException e) {
      handleCloseFailure(e);
    }
  }

  /**
   * Starts resuming a service with the given specification.
   *
   * @param bdHandle a handle to a native BlockchainData object
   * @param instanceSpec the service instance specification as a serialized {@link InstanceSpec}
   *     protobuf message
   * @param arguments the service arguments as a serialized protobuf message
   * @see ServiceRuntime#initiateResumingService(com.exonum.binding.core.blockchain.BlockchainData,
   *     ServiceInstanceSpec, byte[])
   */
  void initiateResumingService(long bdHandle, byte[] instanceSpec, byte[] arguments)
      throws CloseFailuresException {
    try (Cleaner cleaner = new Cleaner()) {
      BlockchainData blockchainData = accessFactory.createBlockchainData(bdHandle, cleaner);
      ServiceInstanceSpec javaInstanceSpec = parseInstanceSpec(instanceSpec);
      serviceRuntime.initiateResumingService(blockchainData, javaInstanceSpec, arguments);
    } catch (CloseFailuresException e) {
      handleCloseFailure(e);
    }
  }

  /**
   * Updates the status of the service instance.
   *
   * @param instanceSpec the service instance specification as a serialized {@link InstanceSpec}
   *     protobuf message
   * @param instanceStatus new status of the service instance as a serialized
   *     representation of the {@link InstanceStatus} protobuf message
   * @see ServiceRuntime#updateInstanceStatus(ServiceInstanceSpec, InstanceStatus)
   */
  void updateServiceStatus(byte[] instanceSpec, byte[] instanceStatus) {
    ServiceInstanceSpec javaInstanceSpec = parseInstanceSpec(instanceSpec);
    InstanceStatus status = parseInstanceStatus(instanceStatus);
    serviceRuntime.updateInstanceStatus(javaInstanceSpec, status);
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

  private static InstanceStatus parseInstanceStatus(byte[] instanceStatus) {
    try {
      return InstanceStatus.parseFrom(instanceStatus);
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
   * @param bdNativeHandle a handle to a native BlockchainData object
   * @param callerServiceId the id of the service which invoked the transaction (in case of
   *      inner transactions); or 0 when the caller is an external message
   * @param txMessageHash the hash of the transaction message
   * @param authorPublicKey the public key of the transaction author
   * @throws ExecutionException if the transaction execution failed
   * @throws UnexpectedExecutionException if the transaction execution failed
   *     with an unexpected exception
   * @throws IllegalArgumentException if any argument is not valid
   * @see ServiceRuntime#executeTransaction(int, String, int, byte[], BlockchainData, int, HashCode,
   *     PublicKey)
   */
  void executeTransaction(int serviceId, String interfaceName, int txId, byte[] arguments,
      long bdNativeHandle, int callerServiceId, byte[] txMessageHash, byte[] authorPublicKey)
      throws CloseFailuresException {

    try (Cleaner cleaner = new Cleaner("executeTransaction")) {
      BlockchainData blockchainData = accessFactory.createBlockchainData(bdNativeHandle, cleaner);
      HashCode hash = HashCode.fromBytes(txMessageHash);
      PublicKey authorPk = PublicKey.fromBytes(authorPublicKey);

      serviceRuntime.executeTransaction(serviceId, interfaceName, txId, arguments, blockchainData,
          callerServiceId, hash, authorPk);
    } catch (CloseFailuresException e) {
      handleCloseFailure(e);
    }
  }

  /**
   * Performs the before transactions operation for the service in this runtime.
   *
   * @see #afterTransactions(int, long)
   * @see ServiceRuntime#beforeTransactions(int, BlockchainData)
   */
  void beforeTransactions(int serviceId, long bdNativeHandle) throws CloseFailuresException {
    try (Cleaner cleaner = new Cleaner("beforeTransactions")) {
      BlockchainData blockchainData = accessFactory.createBlockchainData(bdNativeHandle, cleaner);
      serviceRuntime.beforeTransactions(serviceId, blockchainData);
    } catch (CloseFailuresException e) {
      handleCloseFailure(e);
    }
  }

  /**
   * Performs the after transactions operation for the service in this runtime.
   *
   * @param bdNativeHandle a handle to the native BlockchainData object
   * @throws ExecutionException if the transaction execution failed
   * @throws UnexpectedExecutionException if the transaction execution failed
   *     with an unexpected exception
   * @throws CloseFailuresException if there was a failure in destroying some native peers
   * @see ServiceRuntime#afterTransactions(int, com.exonum.binding.core.blockchain.BlockchainData)
   */
  void afterTransactions(int serviceId, long bdNativeHandle) throws CloseFailuresException {
    try (Cleaner cleaner = new Cleaner("afterTransactions")) {
      BlockchainData blockchainData = accessFactory.createBlockchainData(bdNativeHandle, cleaner);
      serviceRuntime.afterTransactions(serviceId, blockchainData);
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
   * @see ServiceRuntime#afterCommit(Snapshot, OptionalInt, long)
   */
  void afterCommit(long snapshotHandle, int validatorId, long height)
      throws CloseFailuresException {
    try (Cleaner cleaner = new Cleaner("afterCommit")) {
      Snapshot snapshot = accessFactory.createSnapshot(snapshotHandle, cleaner);
      OptionalInt optionalValidatorId = validatorId >= 0
          ? OptionalInt.of(validatorId)
          : OptionalInt.empty();
      serviceRuntime.afterCommit(snapshot, optionalValidatorId, height);
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
