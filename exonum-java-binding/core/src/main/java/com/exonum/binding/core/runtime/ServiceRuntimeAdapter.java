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
@SuppressWarnings({"unused", "SameParameterValue"}) // Native API
class ServiceRuntimeAdapter {

  private final ServiceRuntime serviceRuntime;
  private final ViewFactory viewFactory;
  private static final Logger logger = LogManager.getLogger(ServiceRuntimeAdapter.class);

  ServiceRuntimeAdapter(ServiceRuntime serviceRuntime, ViewFactory viewFactory) {
    this.serviceRuntime = serviceRuntime;
    this.viewFactory = viewFactory;
  }

  /**
   * Deploys the Java service artifact.
   *
   * @param id the service artifact id in format "groupId:artifactId:version"
   * @param deploySpec the deploy specification as a serialized
   *     {@link com.exonum.binding.core.runtime.ServiceRuntimeProtos.DeployArguments}
   *     protobuf message
   * @throws IllegalArgumentException if the deploy specification or id are not valid
   * @throws ServiceLoadingException if the runtime failed to load the service or it is not correct
   * @see ServiceRuntime#deployArtifact(ServiceArtifactId, String)
   */
  void deployArtifact(String id, byte[] deploySpec) throws ServiceLoadingException {
    DeployArguments deployArguments = unpackDeployArgs(id, deploySpec);
    String artifactFilename = deployArguments.getArtifactFilename();

    serviceRuntime.deployArtifact(ServiceArtifactId.parseFrom(id), artifactFilename);
  }

  /**
   * Returns true if the artifact with the given id is deployed in this runtime; false — otherwise.
   * @param id the service artifact id in format "groupId:artifactId:version"
   */
  boolean isArtifactDeployed(String id) {
    ServiceArtifactId artifactId = ServiceArtifactId.parseFrom(id);
    return serviceRuntime.isArtifactDeployed(artifactId);
  }

  private static DeployArguments unpackDeployArgs(String id, byte[] deploySpec) {
    try {
      return DeployArguments.parseFrom(deploySpec);
    } catch (InvalidProtocolBufferException e) {
      String message = "Invalid deploy specification for " + id;
      logger.error(message, e);
      throw new IllegalArgumentException(message, e);
    }
  }

  /**
   * Creates a new instance of an already deployed service.
   *
   * @param name the name of the service
   * @param id the numeric identifier of the service
   * @param artifactId the service artifact id from which to create a new service
   * @see ServiceRuntime#createService(ServiceInstanceSpec)
   */
  // todo: if ServiceInstanceSpec is in protobuf, consider passing the bytes and decoding them
  //   in Java
  void createService(String name, int id, String artifactId) {
    ServiceInstanceSpec instanceSpec = ServiceInstanceSpec.newInstance(name, id,
        ServiceArtifactId.parseFrom(artifactId));
    serviceRuntime.createService(instanceSpec);
  }

  /**
   * Performs an initial configuration of a started service instance.
   *
   * @param id the id of the service
   * @param forkHandle a handle to a native fork object
   * @param configuration the service configuration parameters as a serialized protobuf message
   * @throws CloseFailuresException if there was a failure in destroying some native peers
   * @see ServiceRuntime#initializeService(Integer, Fork, byte[])
   */
  void initializeService(int id, long forkHandle, byte[] configuration)
      throws CloseFailuresException {
    try (Cleaner cleaner = new Cleaner()) {
      Fork fork = viewFactory.createFork(forkHandle, cleaner);

      serviceRuntime.initializeService(id, fork, configuration);
    } catch (CloseFailuresException e) {
      handleCloseFailure(e);
    }
  }

  /**
   * Stops a service instance with the given id.
   *
   * @param id the id of the service
   * @see ServiceRuntime#stopService(Integer)
   */
  void stopService(int id) {
    serviceRuntime.stopService(id);
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

  private static void handleCloseFailure(CloseFailuresException e) throws CloseFailuresException {
    logger.error("Failed to close some resources", e);
    throw e;
  }
}
