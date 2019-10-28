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
import com.exonum.binding.core.runtime.ServiceRuntimeProtos.DeployArguments;
import com.exonum.binding.core.runtime.ServiceRuntimeProtos.ServiceRuntimeStateHashes;
import com.exonum.binding.core.service.BlockCommittedEvent;
import com.exonum.binding.core.service.BlockCommittedEventImpl;
import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.service.NodeProxy;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.transaction.TransactionContext;
import com.exonum.binding.core.transaction.TransactionExecutionException;
import com.exonum.binding.messages.Runtime.ArtifactId;
import com.exonum.binding.messages.Runtime.InstanceSpec;
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
   * Deploys the Java service artifact.
   *
   * @param name the Java service artifact name in format "groupId:artifactId:version"
   * @param deploySpec the deploy specification as a serialized
   *     {@link com.exonum.binding.core.runtime.ServiceRuntimeProtos.DeployArguments}
   *     protobuf message
   * @throws IllegalArgumentException if the deploy specification or id are not valid
   * @throws ServiceLoadingException if the runtime failed to load the service or it is not correct
   * @see ServiceRuntime#deployArtifact(ServiceArtifactId, String)
   */
  void deployArtifact(String name, byte[] deploySpec) throws ServiceLoadingException {
    DeployArguments deployArguments = parseDeployArgs(name, deploySpec);
    String artifactFilename = deployArguments.getArtifactFilename();

    serviceRuntime.deployArtifact(ServiceArtifactId.newJavaId(name), artifactFilename);
  }

  /**
   * Returns true if the artifact with the given name is deployed in this runtime;
   * false — otherwise.
   * @param name the service artifact name in format "groupId:artifactId:version"
   */
  boolean isArtifactDeployed(String name) {
    ServiceArtifactId artifactId = ServiceArtifactId.newJavaId(name);
    return serviceRuntime.isArtifactDeployed(artifactId);
  }

  private static DeployArguments parseDeployArgs(String name, byte[] deploySpec) {
    try {
      return DeployArguments.parseFrom(deploySpec);
    } catch (InvalidProtocolBufferException e) {
      String message = "Invalid deploy specification for " + name;
      logger.error(message, e);
      throw new IllegalArgumentException(message, e);
    }
  }

  /**
   * Creates a new instance of an already deployed service and performs its initial configuration.
   *
   * @param forkHandle a handle to a native fork object
   * @param instanceSpec the service instance specification as a serialized {@link InstanceSpec}
   *     protobuf message
   * @param configuration the service initial configuration parameters as a serialized protobuf
   *     message
   * @see ServiceRuntime#addService(Fork, ServiceInstanceSpec, byte[])
   * @throws CloseFailuresException if there was a failure in destroying some native peers
   */
  void addService(long forkHandle, byte[] instanceSpec, byte[] configuration)
      throws CloseFailuresException {
    try (Cleaner cleaner = new Cleaner()) {
      Fork fork = viewFactory.createFork(forkHandle, cleaner);
      ServiceInstanceSpec javaInstanceSpec = parseInstanceSpec(instanceSpec);

      serviceRuntime.addService(fork, javaInstanceSpec, configuration);
    } catch (CloseFailuresException e) {
      handleCloseFailure(e);
    }
  }

  /**
   * Restarts the service instance that has been successfully added to the blockchain.
   *
   * @param instanceSpec the service instance specification as a serialized {@link InstanceSpec}
   *     protobuf message
   */
  void restartService(byte[] instanceSpec) {
    ServiceInstanceSpec javaInstanceSpec = parseInstanceSpec(instanceSpec);
    serviceRuntime.restartService(javaInstanceSpec);
  }

  private static ServiceInstanceSpec parseInstanceSpec(byte[] instanceSpec) {
    try {
      InstanceSpec spec = InstanceSpec.parseFrom(instanceSpec);
      ArtifactId artifact = spec.getArtifact();
      ServiceArtifactId artifactId = ServiceArtifactId.valueOf(artifact.getRuntimeId(),
          artifact.getName());
      return ServiceInstanceSpec.newInstance(spec.getName(), spec.getId(), artifactId);
    } catch (InvalidProtocolBufferException e) {
      logger.error(e);
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Executes the service transaction.
   *
   * @param serviceId the service numeric identifier
   * @param txId the transaction type identifier within the service
   * @param arguments the transaction arguments
   * @param forkNativeHandle a handle to a native fork object
   * @param txMessageHash the hash of the transaction message
   * @param authorPublicKey the public key of the transaction author
   * @throws TransactionExecutionException if the transaction execution failed
   * @see ServiceRuntime#executeTransaction(Integer, int, byte[], TransactionContext)
   * @see com.exonum.binding.core.transaction.Transaction#execute(TransactionContext)
   */
  void executeTransaction(int serviceId, int txId, byte[] arguments,
      long forkNativeHandle, byte[] txMessageHash, byte[] authorPublicKey)
      throws TransactionExecutionException, CloseFailuresException {

    try (Cleaner cleaner = new Cleaner("executeTransaction")) {
      Fork fork = viewFactory.createFork(forkNativeHandle, cleaner);
      HashCode hash = HashCode.fromBytes(txMessageHash);
      PublicKey authorPk = PublicKey.fromBytes(authorPublicKey);
      TransactionContext context = TransactionContext.builder()
          .fork(fork)
          .txMessageHash(hash)
          .authorPk(authorPk)
          .build();

      serviceRuntime.executeTransaction(serviceId, txId, arguments, context);
    } catch (CloseFailuresException e) {
      handleCloseFailure(e);
    }
  }

  /**
   * Returns the state hashes of this runtime. The state hashes are serialized in protobuf,
   * see {@link ServiceRuntimeStateHashes} for message specification.
   *
   * @param snapshotHandle a handle to the native snapshot object
   * @throws CloseFailuresException if there was a failure in destroying some native peers
   * @see ServiceRuntime#getStateHashes(Snapshot)
   * @see ServiceRuntimeStateHashes
   */
  byte[] getStateHashes(long snapshotHandle) throws CloseFailuresException {
    try (Cleaner cleaner = new Cleaner("getStateHashes")) {
      Snapshot snapshot = viewFactory.createSnapshot(snapshotHandle, cleaner);
      ServiceRuntimeStateHashes stateHashes = serviceRuntime.getStateHashes(snapshot);
      return stateHashes.toByteArray();
    } catch (CloseFailuresException e) {
      handleCloseFailure(e);
      // unreachable, ^ throws
      return null;
    }
  }

  /**
   * Performs the before commit operation for services in this runtime.
   *
   * @param forkHandle a handle to the native fork object, which must support checkpoints
   *                   and rollbacks
   * @throws CloseFailuresException if there was a failure in destroying some native peers
   * @see ServiceRuntime#beforeCommit(Fork)
   */
  void beforeCommit(long forkHandle) throws CloseFailuresException {
    try (Cleaner cleaner = new Cleaner("beforeCommit")) {
      Fork fork = viewFactory.createFork(forkHandle, cleaner);
      serviceRuntime.beforeCommit(fork);
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
   * Mounts the APIs of services with the given ids to the Java web-server.
   *
   * @param serviceIds the numeric ids of services to connect; must not be empty
   * @param nodeNativeHandle the native handle to the Node object
   */
  void connectServiceApis(int[] serviceIds, long nodeNativeHandle) {
    Node node = new NodeProxy(nodeNativeHandle);
    serviceRuntime.connectServiceApis(serviceIds, node);
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
